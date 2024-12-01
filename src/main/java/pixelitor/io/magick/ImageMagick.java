/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.FileChooserConfig.SelectableFormats;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static pixelitor.utils.AppPreferences.magickDirName;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

/**
 * Utility class for working with ImageMagick if installed and accessible
 * in the PATH (or if the ImageMagick path is configured in the settings).
 */
public class ImageMagick {
    private ImageMagick() {
        // utility class
    }

    // check only once, when this class is first used
    private static final boolean installed = checkImageMagickInstalled();
    private static File magickExecutable;

    /**
     * Exports the given composition using ImageMagick.
     */
    public static void export(Composition comp) {
        if (!installed) {
            showNotInstalledDialog();
            return;
        }

        BufferedImage image = comp.getCompositeImage();
        String suggestedFileName = FileUtils.removeExtension(comp.getName());

        File targetFile = FileChoosers.showSaveDialog(new FileChooserConfig(
            suggestedFileName, null, SelectableFormats.ANY));
        if (targetFile == null) { // canceled
            return;
        }

        ExportSettings settings = determineExportSettings(targetFile);
        if (settings instanceof JPanel) { // has a configuration GUI
            boolean dialogAccepted = Dialogs.showOKCancelDialog(settings,
                settings.getFormatName() + " Export Options for " + targetFile.getName(),
                new String[]{"Export", GUIText.CANCEL}, 0,
                JOptionPane.PLAIN_MESSAGE);
            if (!dialogAccepted) {
                return;
            }
        }

        // executes the export asynchronously
        var progressHandler = Messages.startProgress("Exporting with ImageMagick", -1);
        CompletableFuture.runAsync(() -> exportImage(image, targetFile, settings), onIOThread)
            .thenRunAsync(() -> {
                progressHandler.stopProgress();
                comp.afterSuccessfulSaveActions(targetFile, true);
                comp.setDirty(false);
            }, onEDT)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    Messages.showExceptionOnEDT(exception);
                }
            });
    }

    private static ExportSettings determineExportSettings(File file) {
        String ext = FileUtils.getExtension(file.getName());
        if (ext == null) {
            return ExportSettings.DEFAULTS;
        }
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "png" -> PNGExportSettings.INSTANCE;
            case "webp" -> WebPExportSettings.INSTANCE;
            default -> ExportSettings.DEFAULTS;
        };
    }

    /**
     * Imports a composition from a file using ImageMagick.
     */
    public static void importComposition() {
        if (!installed) {
            showNotInstalledDialog();
            return;
        }
        File file = FileChoosers.getAnyOpenFile();
        if (file != null) {
            importComposition(file, true);
        }
    }

    public static void importComposition(File file, boolean checkAlreadyOpen) {
        if (!installed) {
            showNotInstalledDialog();
            return;
        }

        if (checkAlreadyOpen && !Views.warnIfAlreadyOpen(file)) {
            return;
        }

        var progressHandler = Messages.startProgress("Importing with ImageMagick", -1);
        CompletableFuture.supplyAsync(() -> importImage(file), onIOThread)
            .thenAcceptAsync(img -> {
                // called if there were no exceptions while importing
                Composition comp = Composition.fromImage(img, file, null);
                progressHandler.stopProgress();
                Views.addJustLoadedComp(comp);
            }, onEDT)
            .whenComplete((result, exception) -> {
                // always called
                if (exception != null) {
                    progressHandler.stopProgressOnEDT();
                    FileIO.handleFileReadErrors(exception);
                }
            });
    }

    public static void exportImage(BufferedImage img, File outFile,
                                   ExportSettings settings) {
        List<String> command = createExportCommand(outFile, settings);

        // a process that reads a png file from the standard input,
        // and converts it to the given file
        ProcessBuilder pb = new ProcessBuilder(command.toArray(String[]::new));
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);

        try {
            Process process = pb.start();
            FileIO.writeToCommandLineProcess(img, process);
            process.waitFor();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> createExportCommand(File outFile, ExportSettings settings) {
        List<String> command = new ArrayList<>();
        command.add(magickExecutable.getAbsolutePath());
        command.add("convert");

// this setting doesn't seem to have an effect on the speed
//        command.add("-define");
//        command.add("stream:buffer-size=0");

        command.add("png:-"); // read png from stdin

        settings.addMagickOptions(command);
        command.add(settings.getFormatSpecifier() + outFile.getAbsolutePath());

        return command;
    }

    private static BufferedImage importImage(File file) {
        // a process that reads the given file,
        // and writes it as png (depth=8 bit) to the standard output
        ProcessBuilder pb = new ProcessBuilder(
            magickExecutable.getAbsolutePath(), "convert", file.getAbsolutePath(),
            "-depth", "8", // don't send 16-bit data
            "-quality", "1", // importing is faster with minimal compression
            "png:-");
        BufferedImage img;
        try {
            Process process = pb.start();

            // read the image as png after ImageMagick did the conversion
            img = FileIO.readFromCommandLineProcess(process);
        } catch (IOException e) {
            throw DecodingException.forMagickImport(file, e);
        }
        if (img == null) {
            throw DecodingException.forMagickImport(file, null);
        }

        return img;
    }

    public static void main(String[] args) throws IOException {
        if (!installed) {
            System.out.println("ImageMagick::main: NOT INSTALLED");
            System.exit(1);
        }

        File inputFile = new File(System.getProperty("user.home"), "test_input.jpg");
        System.out.println("ImageMagick::main: inputFile = " + inputFile.getAbsolutePath() + (inputFile.exists() ? " - exists" : " - does not exist!"));
        BufferedImage img = ImageIO.read(inputFile);

        File origFile = new File("origFile.png");
        ImageIO.write(img, "PNG", origFile);
        System.out.println("ImageMagick::main: origFile = " + origFile.getAbsolutePath() + (origFile.exists() ? " - exists" : " - does not exist!"));

        BufferedImage out = FileIO.runCommandLineFilter(img,
            List.of(
                magickExecutable.getAbsolutePath(),
                "convert",
                "png:-",
                "-bilateral-blur",
                "8",
                "png:-"
            )).get();
        File outFile = new File("outFile.png");
        ImageIO.write(out, "PNG", outFile);
        System.out.println("ImageMagick::main: outFile = " + outFile.getAbsolutePath() + (outFile.exists() ? " - exists" : " - does not exist!"));
    }

    private static boolean checkImageMagickInstalled() {
        magickExecutable = Utils.findExecutable(magickDirName, "magick");
        if (magickExecutable != null) {
            return true; // executable found
        }

        // try to find it in the PATH
        String searchCommand = JVM.isWindows ? "where" : "which";

        ProcessBuilder pb = new ProcessBuilder(searchCommand, "magick");
        try {
            Process process = pb.start();
            // read the first line of the standard output
            String magickFullPath;
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), UTF_8))) {
                magickFullPath = reader.readLine();
            }
            int exitValue = process.waitFor();
            if (exitValue == 0 && magickFullPath != null) {
                magickExecutable = new File(magickFullPath);
                magickDirName = magickExecutable.getParent();
            }
            return exitValue == 0;
        } catch (InterruptedException | IOException e) {
            return false;
        }
    }

    private static void showNotInstalledDialog() {
        Dialogs.showInfoDialog("ImageMagick 7 Not Found",
            "<html>ImageMagick 7 was not found in the PATH" +
                "<br>or in the folder configured in the Preferences." +
                "<br><br>It can be downloaded from https://imagemagick.org");
    }
}
