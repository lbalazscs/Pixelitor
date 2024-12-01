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

package pixelitor.io;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.Views;
import pixelitor.automate.DirectoryChooser;
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
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isWritable;
import static pixelitor.io.FileChoosers.svgFilter;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.calledOutsideEDT;
import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;
import static pixelitor.utils.Threads.threadInfo;

/**
 * Utility class with static methods related to opening and saving files.
 */
public class FileIO {
    private FileIO() {
        // Prevent instantiation
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
            .whenComplete((comp, exception) -> handleFileReadErrors(exception));
    }

    /**
     * Asynchronously loads a {@link Composition} from a file.
     */
    public static CompletableFuture<Composition> loadCompAsync(File file) {
        // If the file format isn't recognized, this will still try to
        // read it in a single-layered format, which doesn't have to be JPG.
        FileFormat format = FileFormat.fromFile(file).orElse(FileFormat.JPG);
        return format.readAsync(file);
    }

    /**
     * Synchronously loads a Composition from a file.
     */
    public static Composition loadCompSync(File file) {
        FileFormat format = FileFormat.fromFile(file).orElse(FileFormat.JPG);
        return format.readSync(file);
    }

    /**
     * Asynchronously adds a new image layer to an existing composition.
     */
    public static void addNewImageLayerAsync(File file, Composition comp) {
        CompletableFuture
            .supplyAsync(() -> TrackedIO.uncheckedRead(file), onIOThread)
            .thenAcceptAsync(img -> comp.addExternalImageAsNewLayer(
                    img, file.getName(), "Dropped Layer"),
                onEDT)
            .whenComplete((result, exception) -> handleFileReadErrors(exception));
    }

    /**
     * Handles errors that occur during file reading.
     * Can be called from any thread.
     */
    public static void handleFileReadErrors(Throwable error) {
        if (error == null) {
            return;
        }
        if (error instanceof CompletionException) {
            // if the exception was thrown in a previous
            // stage, handle it the same way
            handleFileReadErrors(error.getCause());
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
        if (de.wasMagick()) {
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
     * false if the user cancels the saving or if it could not be saved.
     */
    public static boolean save(Composition comp, boolean forceChooser) {
        boolean useFileChooser = forceChooser || comp.getFile() == null;
        if (useFileChooser) {
            return FileChoosers.saveWithChooser(comp);
        }

        File targetFile = comp.getFile();
        if (targetFile.exists()) { // if it was not deleted in the meantime...
            if (!isWritable(targetFile.toPath())) {
                Dialogs.showFileNotWritableDialog(targetFile);
                return false;
            }
        }
        Optional<FileFormat> fileFormat = FileFormat.fromFile(targetFile);
        if (fileFormat.isPresent()) {
            var saveSettings = new SaveSettings.Simple(fileFormat.get(), targetFile);
            comp.saveAsync(saveSettings, true);
            return true;
        } else {
            // the file was read from a file with an unsupported
            // extension, save it with a file chooser
            return FileChoosers.saveWithChooser(comp);
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
            if (e.getMessage().contains("another process")) {
                // handle here, because we have the file information
                showFileInUseError(targetFile);
            } else {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void showFileInUseError(File file) {
        String msg = format(
            "Can't save to%n%s%nbecause this file is being used by another program.",
            file.getAbsolutePath());

        EventQueue.invokeLater(() -> Messages.showError("Save Error", msg));
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
            addNewImageLayerAsync(file, comp);
        }
    }

    /**
     * Exports all layers of a composition as separate PNG files asynchronously.
     */
    public static void exportLayersToPNGAsync(Composition comp) {
        assert calledOnEDT() : threadInfo();

        boolean directorySelected = DirectoryChooser.selectOutputDir();
        if (!directorySelected) {
            return;
        }

        CompletableFuture
            .supplyAsync(() -> exportLayersToPNG(comp), onIOThread)
            .thenAcceptAsync(numImg -> Messages.showStatusMessage(
                getSavedImagesMessage(numImg, Dirs.getLastSave())), onEDT)
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
            BufferedImage image = layer.asImage(true, false);
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

        File outputDir = Dirs.getLastSave();
        String fileName = format("%03d_%s.png", layerIndex, FileUtils.sanitizeToFileName(layerName));
        File file = new File(outputDir, fileName);

        saveImageToFile(image, new SaveSettings.Simple(FileFormat.PNG, file));
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
        File outputDir = Dirs.getLastSave();
        for (FileFormat format : FileFormat.values()) {
            File outFile = new File(outputDir, "all_formats." + format);
            comp.saveAsync(new SaveSettings.Simple(format, outFile), false);
        }
    }

    public static void saveJpegWithCustomSettings(Composition comp,
                                                  float quality,
                                                  boolean progressive) {
        File selectedFile = FileChoosers.selectSaveFileForFormat(
            comp.suggestFileName("jpg"), FileChoosers.jpegFilter);
        comp.saveAsync(new JpegSettings(quality, progressive, selectedFile), true);
    }

    public static void saveSVG(Shape shape, StrokeParam strokeParam, String suggestedFileName) {
        saveSVG(createSVGContent(shape, strokeParam), suggestedFileName);
    }

    public static void saveSVG(String content, String suggestedFileName) {
        File file = FileChoosers.selectSaveFileForFormat(suggestedFileName, svgFilter);
        if (file == null) { // save file dialog canceled
            return;
        }

        try (PrintWriter out = new PrintWriter(file, UTF_8)) {
            out.println(content);
        } catch (IOException e) {
            Messages.showException(e);
        }
        Messages.showFileSavedMessage(file);
    }

    private static String createSVGContent(Shape shape, StrokeParam strokeParam) {
        boolean exportFilled = isSvgExportFilled(strokeParam);
        if (exportFilled) {
            shape = strokeParam.createStroke().createStrokedShape(shape);
        }
        String svgPath = Shapes.toSVGPath(shape);
        String svgFillRule = Shapes.determineSvgFillRule(shape);

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

    public static Result<BufferedImage, String> runCommandLineFilter(BufferedImage src, List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command.toArray(String[]::new))
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE);

        BufferedImage out;

        try {
            Process process = pb.start();

            writeToCommandLineProcess(src, process);
            out = readFromCommandLineProcess(process);

            String errorMsg = null;
            if (out == null) {
                // There was an error. Try to get the error message.
                try (InputStream processError = process.getErrorStream()) {
                    errorMsg = new String(processError.readAllBytes(), UTF_8);
                }
            }
            process.waitFor();
            if (errorMsg != null) {
                return Result.error(errorMsg);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Result.success(out);
    }

    // Read an image the from the standard output of an external process
    public static BufferedImage readFromCommandLineProcess(Process process) throws IOException {
        BufferedImage image;
        try (InputStream processOutput = process.getInputStream()) {
            assert processOutput instanceof BufferedInputStream;

            image = ImageIO.read(processOutput);
        }
        return image;
    }

    // Write the source image to the standard input of the external process
    public static void writeToCommandLineProcess(BufferedImage src, Process process) throws IOException {
        try (OutputStream processInput = process.getOutputStream()) {
            assert processInput instanceof BufferedOutputStream;

            writeToCommandLineInputStream(src, processInput);
            processInput.flush();
        }
    }

    /**
     * Writes an image to the standard input of an external
     * command-line program in PNG format.
     */
    private static void writeToCommandLineInputStream(BufferedImage img, OutputStream commandLineInput) throws IOException {
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
