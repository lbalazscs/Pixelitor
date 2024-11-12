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
import java.awt.geom.Path2D;
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
public class IO {
    private IO() {
        // Prevent instantiation
    }

    public static CompletableFuture<Composition> openFileAsync(File file,
                                                               boolean checkAlreadyOpen) {
        if (checkAlreadyOpen && !Views.warnIfAlreadyOpen(file)) {
            return CompletableFuture.completedFuture(null);
        }
        return loadCompAsync(file)
            .thenApplyAsync(Views::addJustLoadedComp, onEDT)
            .whenComplete((comp, e) -> handleReadingErrors(e));
    }

    public static CompletableFuture<Composition> loadCompAsync(File file) {
        // If the file format isn't recognized, this will still try to
        // read it in a single-layered format, which doesn't have to be JPG.
        FileFormat format = FileFormat.fromFile(file).orElse(FileFormat.JPG);
        return format.readAsync(file);
    }

    public static Composition loadCompSync(File file) {
        FileFormat format = FileFormat.fromFile(file).orElse(FileFormat.JPG);
        return format.readSync(file);
    }

    public static void addNewImageLayerAsync(File file, Composition comp) {
        CompletableFuture
            .supplyAsync(() -> TrackedIO.uncheckedRead(file), onIOThread)
            .thenAcceptAsync(img -> comp.addExternalImageAsNewLayer(
                    img, file.getName(), "Dropped Layer"),
                onEDT)
            .whenComplete((v, e) -> handleReadingErrors(e));
    }

    /**
     * Utility method designed to be used with CompletableFuture.
     * Can be called on any thread.
     */
    public static void handleReadingErrors(Throwable error) {
        if (error == null) {
            // do nothing if the stage didn't complete exceptionally
            return;
        }
        if (error instanceof CompletionException) {
            // if the exception was thrown in a previous
            // stage, handle it the same way
            handleReadingErrors(error.getCause());
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
            String[] options = {"Try with ImageMagick Import", GUIText.CANCEL};
            boolean retryWithMagick = Dialogs.showOKCancelDialog(msg, "Error",
                options, 0, JOptionPane.ERROR_MESSAGE);
            if (retryWithMagick) {
                ImageMagick.importComposition(de.getFile(), false);
            }
        }
    }

    /**
     * Saves a {@link Composition}, optionally using a file chooser
     * for the file location. Returns true if the file was saved,
     * false if the user cancels the saving or if it could not be saved.
     */
    public static boolean save(Composition comp, boolean saveAs) {
        boolean needsFileChooser = saveAs || comp.getFile() == null;
        if (needsFileChooser) {
            return FileChoosers.saveWithChooser(comp);
        } else {
            File file = comp.getFile();
            if (file.exists()) { // if it was not deleted in the meantime...
                if (!isWritable(file.toPath())) {
                    Dialogs.showFileNotWritableDialog(file);
                    return false;
                }
            }
            Optional<FileFormat> fileFormat = FileFormat.fromFile(file);
            if (fileFormat.isPresent()) {
                var saveSettings = new SaveSettings.Simple(fileFormat.get(), file);
                comp.saveAsync(saveSettings, true);
                return true;
            } else {
                // the file was read from a file with an unsupported
                // extension, save it with a file chooser
                return FileChoosers.saveWithChooser(comp);
            }
        }
    }

    public static void saveImageToFile(BufferedImage image,
                                       SaveSettings saveSettings) {
        FileFormat format = saveSettings.format();
        File selectedFile = saveSettings.file();

        Objects.requireNonNull(format);
        Objects.requireNonNull(selectedFile);
        Objects.requireNonNull(image);
        assert calledOutsideEDT() : "on EDT";

        try {
            if (format == FileFormat.JPG) {
                JpegSettings settings = JpegSettings.from(saveSettings);
                Consumer<ImageWriteParam> customizer = settings.toCustomizer();
                TrackedIO.write(image, "jpg", selectedFile, customizer);
            } else {
                TrackedIO.write(image, format.toString(), selectedFile, null);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("another process")) {
                // handle here, because we have the file information
                showAnotherProcessErrorMsg(selectedFile);
            } else {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void showAnotherProcessErrorMsg(File file) {
        String msg = format(
            "Can't save to%n%s%nbecause this file is being used by another program.",
            file.getAbsolutePath());

        EventQueue.invokeLater(() -> Messages.showError("Save Error", msg));
    }

    public static void openAllSupportedImagesInDir(File dir) {
        List<File> files = FileUtils.listSupportedInputFiles(dir);
        boolean found = false;
        for (File file : files) {
            found = true;
            openFileAsync(file, false);
        }
        if (!found) {
            Messages.showInfo("No Images Found",
                format("<html>No supported image files found in <b>%s</b>.", dir.getName()));
        }
    }

    public static void addAllImagesInDirAsLayers(File dir, Composition comp) {
        List<File> files = FileUtils.listSupportedInputFiles(dir);
        for (File file : files) {
            addNewImageLayerAsync(file, comp);
        }
    }

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

    private static String getSavedImagesMessage(int numImg, File directory) {
        String what = numImg == 1 ? "image" : "images";
        return "Saved %d %s to <b>%s</b>".formatted(numImg, what, directory);
    }

    private static int exportLayersToPNG(Composition comp) {
        assert calledOutsideEDT() : "on EDT";

        int numSavedImages = 0;
        for (int layerIndex = 0; layerIndex < comp.getNumLayers(); layerIndex++) {
            Layer layer = comp.getLayer(layerIndex);
            BufferedImage image = layer.asImage(true, false);
            if (image != null) {
                saveLayerImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            }
        }
        return numSavedImages;
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

    public static void saveInAllFormats(Composition comp) {
        boolean canceled = !DirectoryChooser.selectOutputDir();
        if (canceled) {
            return;
        }
        File saveDir = Dirs.getLastSave();
        for (FileFormat format : FileFormat.values()) {
            File outFile = new File(saveDir, "all_formats." + format);
            comp.saveAsync(new SaveSettings.Simple(format, outFile), false);
        }
    }

    public static void saveJpegWithQuality(Composition comp, float quality, boolean progressive) {
        File selectedFile = FileChoosers.selectSaveFileForFormat(
            comp.suggestFileName("jpg"), FileChoosers.jpegFilter);
        comp.saveAsync(new JpegSettings(quality, progressive, selectedFile), true);
    }

    public static void saveSVG(Shape shape, StrokeParam strokeParam, String suggestedFileName) {
        saveSVG(createSVGContent(shape, strokeParam), suggestedFileName);
    }

    public static void saveSVG(String content, String suggestedFileName) {
        File file = FileChoosers.selectSaveFileForFormat(suggestedFileName, svgFilter);
        if (file == null) { // save file dialog cancelled
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
        boolean exportFilled = false;
        if (strokeParam != null) {
            exportFilled = switch (strokeParam.getStrokeType()) {
                case ZIGZAG, CALLIGRAPHY, SHAPE, TAPERING, TAPERING_REV, RAILWAY -> true;
                case BASIC, WOBBLE, CHARCOAL, BRISTLE, OUTLINE -> false;
            };
        }
        if (exportFilled) {
            shape = strokeParam.createStroke().createStrokedShape(shape);
        }
        String svgPath = Shapes.toSVGPath(shape);

        String svgFillRule = "nonzero";
        if (shape instanceof Path2D path) {
            svgFillRule = switch (path.getWindingRule()) {
                case Path2D.WIND_EVEN_ODD -> "evenodd";
                case Path2D.WIND_NON_ZERO -> "nonzero";
                default -> throw new IllegalStateException("Error: " + path.getWindingRule());
            };
        }

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

    public static void writeToOutStream(BufferedImage img, OutputStream magickInput) throws IOException {
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

    public static BufferedImage commandLineFilter(BufferedImage src, List<String> command) {
        return switch (runCommandLineFilter(src, command)) {
            case Success<BufferedImage, ?>(var img) -> ImageUtils.toSysCompatibleImage(img);
            case Error<?, String>(String errorMsg) -> {
                Messages.showError("Command Line Error", errorMsg);
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
            Process p = pb.start();

            // Write the source image to the standard input
            // of the external process
            try (OutputStream processInput = p.getOutputStream()) {
                assert processInput instanceof BufferedOutputStream;

                writeToOutStream(src, processInput);
                processInput.flush();
            }

            // Read the filtered image the from the standard output
            // of the external process
            try (InputStream processOutput = p.getInputStream()) {
                assert processOutput instanceof BufferedInputStream;

                out = ImageIO.read(processOutput);
            }
            String errorMsg = null;
            if (out == null) {
                // There was an error. Try to get the error message.
                try (InputStream processError = p.getErrorStream()) {
                    errorMsg = new String(processError.readAllBytes(), UTF_8);
                }
            }
            p.waitFor();
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
}
