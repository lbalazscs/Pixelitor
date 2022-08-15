/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.io.magick;

import com.bric.util.JVM;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.gui.GUIText;
import pixelitor.gui.utils.Dialogs;
import pixelitor.io.*;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static pixelitor.utils.AppPreferences.magickDirName;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

/**
 * If ImageMagick is installed and can be found in the PATH, then this
 * class provides static utility methods that use it.
 */
public class ImageMagick {
    private ImageMagick() {
        // only static utility methods
    }

    // check only once, when this class is first used
    private static final boolean installed = checkInstalled();
    private static File magickCommand;

    public static void export(Composition comp) {
        if (!isInstalled()) {
            showNotInstalledDialog();
            return;
        }

        BufferedImage image = comp.getCompositeImage();
        File file = FileChoosers.showSaveDialog(FileChooserInfo.forMagickExport(comp));
        if (file == null) { // canceled
            return;
        }

        ExportSettings settings = settingsFromExtension(file);
        boolean accepted = true;
        if (settings instanceof JPanel) { // has GUI
            accepted = Dialogs.showOKCancelDialog(settings,
                settings.getFormatName() + " Export Options for " + file.getName(),
                new String[]{"Export", GUIText.CANCEL}, 0,
                JOptionPane.PLAIN_MESSAGE);
        }
        if (!accepted) {
            return;
        }

        var progressHandler = Messages.startProgress("ImageMagick Export", -1);
        CompletableFuture.runAsync(() -> exportImage(image, file, settings), onIOThread)
            .thenRunAsync(() -> {
                progressHandler.stopProgress();
                comp.afterSuccessfulSaveActions(file, true);
                comp.setDirty(false);
            }, onEDT)
            .whenComplete((v, e) -> {
                if (e != null) {
                    Messages.showExceptionOnEDT(e);
                }
            });
    }

    private static ExportSettings settingsFromExtension(File file) {
        String ext = FileUtils.calcExtension(file.getName());
        if (ext == null) {
            return ExportSettings.DEFAULTS;
        }
        return switch (ext.toLowerCase()) {
            case "png" -> PNGExportSettingsPanel.INSTANCE;
            case "webp" -> WebpExportSettingsPanel.INSTANCE;
            default -> ExportSettings.DEFAULTS;
        };
    }

    public static void importComposition() {
        if (!isInstalled()) {
            showNotInstalledDialog();
            return;
        }
        File file = FileChoosers.getAnyOpenFile();
        if (file != null) {
            importComposition(file, true);
        }
    }

    public static void importComposition(File file, boolean checkAlreadyOpen) {
        if (!isInstalled()) {
            showNotInstalledDialog();
            return;
        }

        if (checkAlreadyOpen && !Views.warnIfAlreadyOpen(file)) {
            return;
        }

        var progressHandler = Messages.startProgress("ImageMagick Import", -1);
        CompletableFuture.supplyAsync(() -> importImage(file), onIOThread)
            .thenAcceptAsync(img -> {
                // called if there were no exceptions while importing
                Composition comp = Composition.fromImage(img, file, null);
                progressHandler.stopProgress();
                Views.addJustLoadedComp(comp);
            }, onEDT)
            .whenComplete((v, e) -> {
                // always called
                if (e != null) {
                    progressHandler.stopProgressOnEDT();
                }
                IO.checkForReadingProblems(e);
            });
    }

    @VisibleForTesting
    public static void exportImage(BufferedImage img, File outFile,
                                   ExportSettings settings) {
        List<String> command = new ArrayList<>();
        command.add(magickCommand.getAbsolutePath());
        command.add("convert");

// this setting doesn't seem to have an effect on the speed
//        command.add("-define");
//        command.add("stream:buffer-size=0");

        command.add("png:-"); // read png from stdin

        settings.addMagickOptions(command);
        command.add(settings.getFormatSpecifier() + outFile.getAbsolutePath());

        System.out.println("ImageMagick::exportImage: command = " + command);

        // a process that reads a png file from the standard input,
        // and converts it to the given file
        ProcessBuilder pb = new ProcessBuilder(command.toArray(String[]::new));
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        try {
            Process p = pb.start();
            try (OutputStream magickInput = p.getOutputStream()) {
                writeToOutStream(img, magickInput);
                magickInput.flush();
            }
            p.waitFor();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeToOutStream(BufferedImage img, OutputStream magickInput) throws IOException {
        // Write as png to ImageMagick and let it do
        // the conversion to the final format.
        // Explicitly setting a low compression level doesn't seem
        // to make it faster (why?), so use the simple approach.
        ImageIO.write(img, "png", magickInput);

//        try (ImageOutputStream ios = ImageIO.createImageOutputStream(magickInput)) {
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

    private static BufferedImage importImage(File file) {
        // a process that reads the given file,
        // and writes it as png (depth=8 bit) to the standard output
        ProcessBuilder pb = new ProcessBuilder(
            magickCommand.getAbsolutePath(), "convert", file.getAbsolutePath(),
            "-depth", "8", // don't send 16-bit data
            "-quality", "1", // importing is faster with minimal compression
            "png:-");
        BufferedImage img;
        try {
            Process p = pb.start();

            // read the image as png after ImageMagick did the conversion
            try (InputStream magickOutput = p.getInputStream()) {
                img = ImageIO.read(magickOutput);
            }
        } catch (IOException e) {
            throw DecodingException.magick(file, e);
        }
        if (img == null) {
            throw DecodingException.magick(file, null);
        }

        return img;
    }

    private static boolean checkInstalled() {
        if (!magickDirName.isEmpty()) {
            String executable = JVM.isWindows ? "magick.exe" : "magick";
            magickCommand = new File(magickDirName, executable);
            if (magickCommand.exists()) {
                // ImageMagick was found in the directory given in the preferences
                return true;
            }
        }

        // try to find it
        String searchCommand = JVM.isWindows ? "where" : "which";

        ProcessBuilder pb = new ProcessBuilder(searchCommand, "magick");
        try {
            Process process = pb.start();
            // read the first line of the standard output
            String magickFullPath;
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                magickFullPath = reader.readLine();
            }
            int exitValue = process.waitFor();
            if (exitValue == 0 && magickFullPath != null) {
                magickCommand = new File(magickFullPath);
                magickDirName = magickCommand.getParent();
            }
            return exitValue == 0;
        } catch (InterruptedException | IOException e) {
            return false;
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    private static void showNotInstalledDialog() {
        Dialogs.showInfoDialog("ImageMagick 7 not found",
            "<html>ImageMagick 7 was not found in the PATH" +
                "<br>or in the folder configured in the Preferences." +
                "<br><br>It can be downloaded from https://imagemagick.org");
    }
}
