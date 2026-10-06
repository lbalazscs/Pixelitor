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
import pixelitor.utils.Messages;
import pixelitor.utils.Shapes;

import javax.imageio.ImageWriteParam;
import javax.swing.*;
import java.awt.EventQueue;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
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
import static pixelitor.utils.Threads.*;

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
        if (preventDuplicateOpen && !Views.confirmIfAlreadyOpen(file)) {
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
            return TrackedIO.readSingleLayerCompAsync(file);
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
            return TrackedIO.readSingleLayerCompSync(file);
        }
    }

    /**
     * Asynchronously adds a new image layer to an existing composition.
     */
    public static void addImageLayerAsync(File file, Composition comp) {
        CompletableFuture
            .supplyAsync(() -> TrackedIO.readUnchecked(file), onIOThread)
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
            promptMagickImportFallback(de, msg);
        }
    }

    private static void promptMagickImportFallback(DecodingException de, String msg) {
        String[] options = {"Try with ImageMagick Import", GUIText.CANCEL};
        boolean retryWithMagick = Dialogs.showOKCancelQuestion(msg, "Error",
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
                Dialogs.showFileNotWritableError(targetFile);
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
            .thenAcceptAsync(exportedCount -> Messages.showStatusMessage(
                getSavedImagesMessage(exportedCount, RecentDirs.getLastSave())), onEDT)
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

        String svgStrokeStyleAttr = strokeParam != null && !exportFilled
            ? strokeParam.copyState().toSVGAttributes()
            : "";

        Canvas canvas = Views.getActiveComp().getCanvas();
        return """
            %s
              <path d="%s" fill="%s" stroke="%s" fill-rule="%s" %s/>
            </svg>
            """.formatted(canvas.createSvgRootTag(), svgPath,
            svgFillAttr, svgStrokeAttr, svgFillRule, svgStrokeStyleAttr);
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
}
