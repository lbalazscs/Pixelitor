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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.automate.DirectoryChooser;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.StrokeParam;
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.Dialogs;
import pixelitor.io.magick.ImageMagick;
import pixelitor.layers.Layer;
import pixelitor.utils.Error;
import pixelitor.utils.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.swing.*;
import java.awt.EventQueue;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isWritable;
import static pixelitor.io.FileChoosers.svgFilter;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

/**
 * Utility class with static methods related to opening and saving files.
 */
public class FileIO {
    private FileIO() {
        // prevent instantiation
    }

    /**
     * Opens a file asynchronously, optionally checking if it's already open.
     */
    public static CompletableFuture<Composition> openFileAsync(File file,
                                                               boolean preventDuplicateOpen) {
        if (preventDuplicateOpen && !Views.warnIfAlreadyOpen(file)) {
            return CompletableFuture.completedFuture(null);
        }
        return loadCompAsync(file)
            .thenApplyAsync(Views::addJustLoadedComp, onEDT)
            .whenComplete((comp, exception) -> handleFileReadError(exception));
    }

    /**
     * Asynchronously loads a {@link Composition} from a file.
     */
    public static CompletableFuture<Composition> loadCompAsync(File file) {
        Optional<FileFormat> fileFormat = FileFormat.fromExtension(file);
        if (fileFormat.isPresent()) {
            return fileFormat.get().readAsync(file);
        } else {
            // if the file format isn't recognized from the extension,
            // try to read it as a generic single-layered format
            return TrackedIO.readSingleLayeredAsync(file);
        }
    }

    /**
     * Synchronously loads a Composition from a file.
     */
    public static Composition loadCompSync(File file) {
        Optional<FileFormat> fileFormat = FileFormat.fromExtension(file);
        if (fileFormat.isPresent()) {
            return fileFormat.get().readSync(file);
        } else {
            // if the file format isn't recognized from the extension,
            // try to read it as a generic single-layered format
            return TrackedIO.readSingleLayeredSync(file);
        }
    }

    /**
     * Asynchronously adds a new image layer to an existing composition.
     */
    public static void addImageLayerAsync(File file, Composition comp) {
        CompletableFuture
            .supplyAsync(() -> TrackedIO.uncheckedRead(file), onIOThread)
            .thenAcceptAsync(img -> comp.addExternalImageAsNewLayer(
                    img, file.getName(), "Dropped Layer"),
                onEDT)
            .whenComplete((result, exception) -> handleFileReadError(exception));
    }

    /**
     * Handles errors that occur during file reading.
     * Can be called from any thread.
     */
    public static void handleFileReadError(Throwable error) {
        if (error == null) {
            return;
        }
        if (error instanceof CompletionException) {
            // if the exception was thrown in a previous
            // stage, handle it the same way
            handleFileReadError(error.getCause());
            return;
        }
        if (error instanceof DecodingException de) {
            if (calledOnEDT()) {
                showDecodingError(de);
            } else {
                EventQueue.invokeLater(() -> showDecodingError(de));
            }
        } else {
            Messages.showExceptionOnEDT(error);
        }
    }

    private static void showDecodingError(DecodingException de) {
        String msg = de.getMessage();
        if (de.isFromImageMagick()) {
            Messages.showError("Error", msg);
        } else {
            promptImageMagickRetry(de, msg);
        }
    }

    private static void promptImageMagickRetry(DecodingException de, String msg) {
        String[] options = {"Try with ImageMagick Import", GUIText.CANCEL};
        boolean retryWithMagick = Dialogs.showOKCancelDialog(msg, "Error",
            options, 0, JOptionPane.ERROR_MESSAGE);
        if (retryWithMagick) {
            ImageMagick.importComposition(de.getFile(), false);
        }
    }

    /**
     * Saves a {@link Composition}, optionally using a file chooser
     * for the file location. Returns true if the file was saved,
     * false if the user cancels or if it could not be saved.
     */
    public static boolean save(Composition comp, boolean forceChooser) {
        boolean useFileChooser = forceChooser || comp.getFile() == null;
        if (useFileChooser) {
            return FileChoosers.promptAndSaveComp(comp);
        }

        File targetFile = comp.getFile();
        if (targetFile.exists()) { // if it was not deleted in the meantime...
            if (!isWritable(targetFile.toPath())) {
                Dialogs.showFileNotWritableDialog(targetFile);
                return false;
            }
        }
        Optional<FileFormat> fileFormat = FileFormat.fromExtension(targetFile);
        if (fileFormat.isPresent()) {
            var saveSettings = new SaveSettings.Default(fileFormat.get(), targetFile);
            comp.saveAsync(saveSettings, true);
            return true;
        } else {
            // the file was read from a file with an unsupported
            // extension, save it with a file chooser
            return FileChoosers.promptAndSaveComp(comp);
        }
    }

    /**
     * Saves an image to a file using the given settings.
     */
    public static void saveImageToFile(BufferedImage image,
                                       SaveSettings saveSettings) {
        FileFormat format = saveSettings.format();
        File targetFile = saveSettings.file();

        Objects.requireNonNull(format);
        Objects.requireNonNull(targetFile);
        Objects.requireNonNull(image);
        assert calledOutsideEDT() : "on EDT";

        try {
            if (format == FileFormat.JPG) {
                JpegSettings settings = JpegSettings.from(saveSettings);
                Consumer<ImageWriteParam> customizer = settings.toCustomizer();
                TrackedIO.write(image, "jpg", targetFile, customizer);
            } else {
                TrackedIO.write(image, format.toString(), targetFile, null);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void openAllSupportedImagesInDir(File dir) {
        List<File> files = FileUtils.listSupportedInputFiles(dir);
        if (files.isEmpty()) {
            Messages.showInfo("No Images Found",
                format("<html>No supported image files found in <b>%s</b>.", dir.getName()));
            return;
        }
        for (File file : files) {
            openFileAsync(file, false);
        }
    }

    /**
     * Adds all supported images from a directory as new layers to an existing composition.
     */
    public static void addAllImagesInDirAsLayers(File dir, Composition comp) {
        List<File> files = FileUtils.listSupportedInputFiles(dir);
        for (File file : files) {
            addImageLayerAsync(file, comp);
        }
    }

    /**
     * Exports all layers of a composition as separate PNG files asynchronously.
     */
    public static void exportLayersToPNGAsync(Composition comp) {
        assert calledOnEDT() : callInfo();

        boolean directorySelected = DirectoryChooser.selectOutputDir();
        if (!directorySelected) {
            return;
        }

        CompletableFuture
            .supplyAsync(() -> exportLayersToPNG(comp), onIOThread)
            .thenAcceptAsync(numImg -> Messages.showStatusMessage(
                getSavedImagesMessage(numImg, RecentDirs.getLastSave())), onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private static String getSavedImagesMessage(int imageCount, File directory) {
        String what = imageCount == 1 ? "image" : "images";
        return "Saved %d %s to <b>%s</b>".formatted(imageCount, what, directory);
    }

    private static int exportLayersToPNG(Composition comp) {
        assert calledOutsideEDT() : "on EDT";

        int exportedCount = 0;
        for (int layerIndex = 0; layerIndex < comp.getNumLayers(); layerIndex++) {
            Layer layer = comp.getLayer(layerIndex);
            BufferedImage image = layer.toImage(true, false);
            if (image != null) {
                saveLayerImage(image, layer.getName(), layerIndex);
                exportedCount++;
            }
        }
        return exportedCount;
    }

    private static void saveLayerImage(BufferedImage image,
                                       String layerName,
                                       int layerIndex) {
        assert calledOutsideEDT() : "on EDT";

        File outputDir = RecentDirs.getLastSave();
        String fileName = format("%03d_%s.png", layerIndex, FileUtils.sanitizeToFileName(layerName));
        File file = new File(outputDir, fileName);

        saveImageToFile(image, new SaveSettings.Default(FileFormat.PNG, file));
    }

    /**
     * Saves a composition in all supported file formats.
     * Prompts user for output directory selection.
     */
    public static void saveInAllFormats(Composition comp) {
        boolean canceled = !DirectoryChooser.selectOutputDir();
        if (canceled) {
            return;
        }
        File outputDir = RecentDirs.getLastSave();
        for (FileFormat format : FileFormat.values()) {
            File outFile = new File(outputDir, "all_formats." + format);
            comp.saveAsync(new SaveSettings.Default(format, outFile), false);
        }
    }

    public static void saveJpegWithCustomSettings(Composition comp,
                                                  float quality,
                                                  boolean progressive) {
        File selectedFile = FileChoosers.selectSaveFileForFormat(
            comp.suggestFileName("jpg"), FileChoosers.jpegFilter);
        if (selectedFile != null) {
            comp.saveAsync(new JpegSettings(selectedFile, quality, progressive), true);
        }
    }

    public static void saveSVG(Shape shape, StrokeParam strokeParam, String suggestedFileName) {
        saveSVG(createSVGContent(shape, strokeParam), suggestedFileName);
    }

    public static void saveSVG(String content, Filter filter) {
        saveSVG(content, filter.getName() + ".svg");
    }

    public static void saveSVG(String content, String suggestedFileName) {
        File file = FileChoosers.selectSaveFileForFormat(suggestedFileName, svgFilter);
        if (file == null) { // save file dialog canceled
            return;
        }

        try (PrintWriter out = new PrintWriter(file, UTF_8)) {
            out.println(content);
            Messages.showFileSavedMessage(file);
        } catch (IOException e) {
            Messages.showException(e);
        }
    }

    private static String createSVGContent(Shape shape, StrokeParam strokeParam) {
        boolean exportFilled = isSvgExportFilled(strokeParam);
        if (exportFilled) {
            shape = strokeParam.createStroke().createStrokedShape(shape);
        }
        String svgPath = Shapes.toSvgPath(shape);
        String svgFillRule = Shapes.getSvgFillRule(shape);

        String svgFillAttr = exportFilled ? "black" : "none";
        String svgStrokeAttr = exportFilled ? "none" : "black";
        String svgStrokeStyle = "";
        if (strokeParam != null && !exportFilled) {
            svgStrokeStyle = strokeParam.copyState().toSVGStyle();
        }

        Canvas canvas = Views.getActiveComp().getCanvas();
        return """
            %s
              <path d="%s" fill="%s" stroke="%s" fill-rule="%s" %s/>
            </svg>
            """.formatted(canvas.createSVGElement(), svgPath,
            svgFillAttr, svgStrokeAttr, svgFillRule, svgStrokeStyle);
    }

    private static boolean isSvgExportFilled(StrokeParam strokeParam) {
        if (strokeParam == null) {
            return false;
        }
        return switch (strokeParam.getStrokeType()) {
            case ZIGZAG, CALLIGRAPHY, SHAPE, TAPERING, TAPERING_REV, RAILWAY -> true;
            case BASIC, WOBBLE, CHARCOAL, BRISTLE, OUTLINE -> false;
        };
    }

    public static BufferedImage applyCommandLineFilter(BufferedImage src, List<String> command) {
        return switch (runCommandLineFilter(src, command)) {
            case Success<BufferedImage, ?>(var img) -> ImageUtils.toSysCompatibleImage(img);
            case Error<?, String>(String errorMsg) -> {
                Messages.showError("Command Line Filter Error", errorMsg);
                yield src;
            }
        };
    }

    /**
     * Executes an external command that understands PNG on stdin and writes PNG to stdout.
     */
    public static Result<BufferedImage, String> runCommandLineFilter(BufferedImage src, List<String> command) {
        ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

        ProcessBuilder pb = new ProcessBuilder(command.toArray(String[]::new))
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE);

        try {
            Process process = pb.start();

            try {
                // drain stderr asynchronously to prevent pipe buffer deadlock
                CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
                    try (InputStream processError = process.getErrorStream()) {
                        return new String(processError.readAllBytes(), UTF_8);
                    } catch (IOException e) {
                        return "Error reading stderr: " + e.getMessage();
                    }
                }, ioExecutor);

                // feed stdin asynchronously to prevent blocking if stdout/stderr fills up early
                CompletableFuture<Void> stdinFuture = CompletableFuture.runAsync(() -> {
                    try {
                        writeToCommandLineProcess(src, process);
                    } catch (IOException e) {
                        // this happens if the process terminates early (broken pipe)
                        throw new CompletionException(e);
                    }
                }, ioExecutor);

                // read stdout synchronously in the current thread
                BufferedImage out = null;
                try {
                    out = readFromCommandLineProcess(process);
                } catch (IOException e) {
                    // will be handled using exit code and stderr
                }

                int exit = process.waitFor();

                // ensure the background write completes (or fails)
                try {
                    stdinFuture.join();
                } catch (Exception e) {
                    // ignored: a failure here is expected if the process exited early
                }

                // retrieve any captured error messages
                String errorMsg = stderrFuture.join();

                // check for errors
                if (exit != 0 || out == null) {
                    if (errorMsg != null && !errorMsg.isBlank()) {
                        return Result.error(errorMsg.trim());
                    } else {
                        return Result.error("Process failed (exit=" + exit + ")");
                    }
                }

                return Result.success(out);
            } finally {
                // guarantee process termination
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads an image from the standard output of an external process.
     */
    public static BufferedImage readFromCommandLineProcess(Process process) throws IOException {
        BufferedImage image;
        try (InputStream rawIn = process.getInputStream();
             InputStream processOutput = rawIn instanceof BufferedInputStream
                 ? rawIn
                 : new BufferedInputStream(rawIn)) {
            image = ImageIO.read(processOutput);
        }
        return image;
    }

    /**
     * Writes an image to the standard input of an external process.
     */
    public static void writeToCommandLineProcess(BufferedImage src, Process process) throws IOException {
        try (OutputStream rawOut = process.getOutputStream();
             OutputStream processInput = rawOut instanceof BufferedOutputStream
                 ? rawOut
                 : new BufferedOutputStream(rawOut)) {
            writePngToProcessStdin(src, processInput);
            processInput.flush();
        }
    }

    /**
     * Writes the given image to the standard input
     * of an external command-line program in PNG format.
     */
    private static void writePngToProcessStdin(BufferedImage img, OutputStream commandLineInput) throws IOException {
        // Write as png to ImageMagick and let it do
        // the conversion to the final format.
        // Explicitly setting a low compression level doesn't seem
        // to make it faster (why?), so use the simple approach.
        ImageIO.write(img, "png", commandLineInput);

//        try (ImageOutputStream ios = ImageIO.createImageOutputStream(commandLineInput)) {
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
}
