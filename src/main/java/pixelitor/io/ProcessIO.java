/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.io;

import pixelitor.utils.*;
import pixelitor.utils.Error;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class with static methods related to external processes.
 */
public class ProcessIO {
    /**
     * Runs the pipe operations, which can neither be interrupted nor
     * given a timeout. A task stuck on a pipe is harmless: it is a cheap
     * virtual thread, and the caller never waits for it beyond the deadline.
     */
    private static final Executor IO_EXECUTOR = Thread::startVirtualThread;

    private ProcessIO() {
        // prevent instantiation
    }

    public static BufferedImage applyCommandLineFilter(BufferedImage src,
                                                       List<String> command,
                                                       Duration timeout) {
        return switch (runCommandLineFilter(src, command, timeout)) {
            case Success<BufferedImage, ?>(var img) -> ImageUtils.toSysCompatibleImage(img);
            case Error<?, String>(String errorMsg) -> {
                Messages.showError("Command Line Filter Error", errorMsg);
                yield src;
            }
        };
    }

    /**
     * Executes an external command that understands PNG on stdin and writes PNG to stdout.
     * All failures (including timeouts and interruption) are returned as
     * error results, nothing is thrown.
     *
     * @param timeout the time the command has to finish, and to close
     *                its output streams after it has finished
     */
    public static Result<BufferedImage, String> runCommandLineFilter(BufferedImage src,
                                                                     List<String> command,
                                                                     Duration timeout) {
        if (command.isEmpty()) {
            return Result.error("No command was specified.");
        }

        // Encode before starting the process: this way an encoder
        // failure can't be confused with a failure of the process or its pipes.
        byte[] png;
        try {
            png = encodePng(src);
        } catch (IOException | RuntimeException e) {
            return Result.error("Could not encode the image as PNG: " + messageOf(e));
        }

        Process process;
        try {
            process = new ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
        } catch (IOException e) {
            // typically a missing or non-executable command
            return Result.error("Could not start the command: " + messageOf(e));
        }

        try {
            return awaitResult(process, png, timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("The command was interrupted.");
        } finally {
            // guarantee process termination
            destroyProcessTree(process);
        }
    }

    private static Result<BufferedImage, String> awaitResult(Process process,
                                                             byte[] png,
                                                             Duration timeout)
        throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        // All three pipes are serviced concurrently, otherwise a full
        // pipe buffer could deadlock the process. The tasks don't throw,
        // they return their problems in their results.
        var stdin = CompletableFuture.supplyAsync(() -> writeStdin(png, process), IO_EXECUTOR);
        var stdout = CompletableFuture.supplyAsync(() -> readImage(process), IO_EXECUTOR);
        var stderr = CompletableFuture.supplyAsync(() -> readStderr(process), IO_EXECUTOR);

        if (!process.waitFor(timeout)) {
            return Result.error("The command didn't finish within " + seconds(timeout) + ".");
        }

        // The process has exited, so the pipes should be at EOF
        // any moment now, unless a descendant process inherited them.
        try {
            CompletableFuture.allOf(stdin, stdout, stderr)
                .get(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            return Result.error("The command exited, but its output streams were still open after "
                + seconds(timeout) + " (did it start a background process?).");
        } catch (ExecutionException e) {
            // only if a task died with an unexpected exception or error
            return Result.error("Unexpected error: " + messageOf(e.getCause()));
        }

        int exitCode = process.exitValue();
        Result<BufferedImage, String> image = stdout.join();
        if (exitCode == 0 && image.isSuccess()) {
            // a stdin problem doesn't matter if the command succeeded
            // (for example because it didn't need all the input)
            return image;
        }

        String errorOutput = stderr.join().orElse("").trim();
        if (!errorOutput.isEmpty()) {
            return Result.error(errorOutput);
        }

        // stderr doesn't help, so report everything else that is known
        var problems = new ArrayList<String>();
        if (exitCode != 0) {
            problems.add("Process failed (exit code=" + exitCode + ")");
        }
        image.ifError(problems::add);
        stdin.join().ifError(problems::add);
        stderr.join().ifError(problems::add);
        // never empty, because the command failed: either exitCode != 0 or image is an error
        return Result.error(String.join("\n", problems));
    }

    private static Result<Void, String> writeStdin(byte[] png, Process process) {
        try {
            writeToCommandLineProcess(png, process);
            return Result.success(null);
        } catch (IOException e) {
            // typically a broken pipe: the process exited or closed
            // its stdin before it consumed everything
            return Result.error("Could not send the image to the command: " + messageOf(e));
        }
    }

    private static Result<BufferedImage, String> readImage(Process process) {
        try {
            return Result.ofNullable(readFromCommandLineProcess(process),
                () -> "The command didn't write a valid image (PNG is expected) to its output.");
        } catch (IOException | RuntimeException e) {
            // RuntimeExceptions can come from the image readers on malformed data
            return Result.error("Could not read the image written by the command: " + messageOf(e));
        }
    }

    private static Result<String, String> readStderr(Process process) {
        try (InputStream processError = process.getErrorStream()) {
            return Result.success(new String(processError.readAllBytes(), UTF_8));
        } catch (IOException e) {
            return Result.error("Could not read the error output of the command: " + messageOf(e));
        }
    }

    /**
     * Writes the PNG data to the standard input of an external process, and closes it.
     */
    public static void writeToCommandLineProcess(byte[] png, Process process) throws IOException {
        try (OutputStream processStdin = process.getOutputStream()) {
            processStdin.write(png);
        }
    }

    /**
     * Encodes the given image in PNG format. Callers that pipe the result
     * to a process should call this before starting the process.
     *
     * @throws IOException if the image can't be encoded
     */
    public static byte[] encodePng(BufferedImage img) throws IOException {
        var out = new ByteArrayOutputStream();

        // Write as png to the external process and let it handle further processing.
        // Explicitly setting a low compression level doesn't seem
        // to make it faster (why?), so use the simple approach.
        if (!ImageIO.write(img, "png", out)) {
            // ImageIO.write returns false if there is no suitable writer
            throw new IIOException("No PNG writer was found for this image type.");
        }
        return out.toByteArray();

//        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
//            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
//            ImageWriter writer = writers.next();
//            ImageWriteParam writeParam = writer.getDefaultWriteParam();
//            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//            writeParam.setCompressionQuality(1.0f); // 1 is no compression
//            try {
//                writer.setOutput(ios);
//                writer.write(img);
//            } finally {
//                writer.dispose();
//                ios.flush();
//            }
//        }
    }

    /**
     * Reads an image from the standard output of an external process.
     * Returns null if the output doesn't contain a recognizable image.
     */
    public static BufferedImage readFromCommandLineProcess(Process process) throws IOException {
        try (InputStream rawIn = process.getInputStream();
             InputStream processStdout = rawIn instanceof BufferedInputStream
                 ? rawIn
                 : new BufferedInputStream(rawIn)) {
            BufferedImage image = ImageIO.read(processStdout);

            // ImageIO.read can stop before the end of the stream. Consume the
            // rest, because if the pipe is closed while the process still writes
            // trailing data, the process could die from a broken pipe.
            processStdout.transferTo(OutputStream.nullOutputStream());
            return image;
        }
    }

    private static void destroyProcessTree(Process process) {
        if (process.isAlive()) {
            // the descendants must be collected while the process is
            // still alive, because they are reparented when it dies
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    private static String messageOf(Throwable t) {
        String message = t.getMessage();
        return message != null ? message : t.getClass().getSimpleName();
    }

    private static String seconds(Duration duration) {
        return "%.1f seconds".formatted(duration.toMillis() / 1000.0);
    }
}
