/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.automate.SingleDirChooser;
import pixelitor.gui.OpenComps;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TextLayer;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static pixelitor.utils.Utils.getJavaMainVersion;

/**
 * Utility class with static methods related to opening and saving files.
 */
public class OpenSave {
    private OpenSave() {
    }

    public static CompletableFuture<Composition> openFileAsync(File file) {
        return loadCompFromFileAsync(file).
                thenApplyAsync(comp -> addJustLoadedComp(comp, file),
                        EventQueue::invokeLater)
                .exceptionally(Messages::showExceptionOnEDT);
    }

    private static Composition addJustLoadedComp(Composition comp, File file) {
        if (comp != null) { // there was no decoding problem
            OpenComps.addAsNewImage(comp);
            RecentFilesMenu.getInstance().addFile(file);
            Messages.showInStatusBar("<html><b>" + file.getName() + "</b> was opened.");
        }
        return comp;
    }

    public static CompletableFuture<Composition> loadCompFromFileAsync(File file) {
        CompletableFuture<Composition> cf;

        String ext = FileUtils.findExtension(file.getName()).orElse("");
        if ("pxc".equals(ext)) {
            cf = loadLayered(file, "pxc");
        } else if ("ora".equals(ext)) {
            cf = loadLayered(file, "ora");
        } else {
            cf = loadSimpleFile(file);
        }

        return cf;
    }

    // loads an a file with a single-layer image format
    private static CompletableFuture<Composition> loadSimpleFile(File file) {
        return CompletableFuture.supplyAsync(
                () -> TrackedIO.uncheckedRead(file), IOThread.getExecutor())
                .handle((img, e) -> handleDecodingError(file, img, e))
                .thenApplyAsync(img -> Composition.fromImage(img, file, null),
                        EventQueue::invokeLater);
    }

    private static BufferedImage handleDecodingError(File file,
                                                     BufferedImage img,
                                                     Throwable e) {
        if (e != null) {
            assert img == null;
            Messages.showExceptionOnEDT(e);
            return img;
        }

        if (img != null) { // everything went well
            return img;
        }

        // if we have a null image here, it means a decoding error,
        // even if no exception was thrown
        EventQueue.invokeLater(() -> showDecodingError(file));
        return null;
    }

    private static void showDecodingError(File file) {
        String msg = format("Could not load \"%s\" as an image file.",
                file.getName());

        String ext = FileUtils.findExtension(file.getName()).orElse("");
        if (ext.startsWith("tif") && getJavaMainVersion() == 8) {
            msg += "\nNote that TIFF files are supported only when Pixelitor is running on Java 9+.";
            msg += "\nCurrently it is running on Java 8.";
        }
        Messages.showError("Error", msg);
    }

    public static CompletableFuture<Void> loadFileAndAddAsNewImageLayer(File file, Composition comp) {
        return CompletableFuture.supplyAsync(
                () -> TrackedIO.uncheckedRead(file), IOThread.getExecutor())
                .handle((img, e) -> handleDecodingError(file, img, e))
                .thenAcceptAsync(image -> comp.addExternalImageAsNewLayer(
                        image, file.getName(), "Dropped Layer"),
                        EventQueue::invokeLater);
    }

    private static CompletableFuture<Composition> loadLayered(File selectedFile,
                                                              String type) {
        Callable<Composition> loadTask;
        switch (type) {
            case "pxc":
                loadTask = () -> PXCFormat.read(selectedFile);
                break;
            case "ora":
                loadTask = () -> OpenRaster.read(selectedFile);
                break;
            default:
                throw new IllegalStateException("type = " + type);
        }

        return CompletableFuture.supplyAsync(
                Utils.toSupplier(loadTask),
                IOThread.getExecutor());
    }

    public static void save(boolean saveAs) {
        Composition comp = OpenComps.getActiveCompOrNull();
        save(comp, saveAs);
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean save(Composition comp, boolean saveAs) {
        boolean needsFileChooser = saveAs || (comp.getFile() == null);
        if (needsFileChooser) {
            return FileChoosers.saveWithChooser(comp);
        } else {
            File file = comp.getFile();
            OutputFormat outputFormat = OutputFormat.fromFile(file);
            SaveSettings saveSettings = new SaveSettings(outputFormat, file);
            comp.saveAsync(saveSettings, true);
            return true;
        }
    }

    public static void saveImageToFile(BufferedImage image,
                                       SaveSettings saveSettings) {
        OutputFormat format = saveSettings.getOutputFormat();
        File selectedFile = saveSettings.getFile();

        Objects.requireNonNull(format);
        Objects.requireNonNull(selectedFile);
        Objects.requireNonNull(image);
        assert !EventQueue.isDispatchThread() : "EDT thread";

        try {
            if (format == OutputFormat.JPG) {
                JpegSettings settings;
                if (saveSettings instanceof JpegSettings) {
                    settings = (JpegSettings) saveSettings;
                } else {
                    settings = JpegSettings.DEFAULTS;
                    settings.setFile(saveSettings.getFile());
                    settings.setOutputFormat(saveSettings.getOutputFormat());
                }
                JpegOutput.writeJPG(image, selectedFile, settings);
            } else {
                TrackedIO.write(image, format.toString(), selectedFile);
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
                "Cannot save to\n%s\nbecause this file is being used by another program.",
                file.getAbsolutePath());

        EventQueue.invokeLater(() -> Messages.showError("Cannot save", msg));
    }

    public static void openAllImagesInDir(File dir) {
        File[] files = FileUtils.listSupportedInputFilesIn(dir);
        if (files != null) {
            for (File file : files) {
                openFileAsync(file);
            }
        }
    }

    public static void addAsLayersAllImagesInDir(File dir, Composition comp) {
        File[] files = FileUtils.listSupportedInputFilesIn(dir);
        if (files != null) {
            for (File file : files) {
                loadFileAndAddAsNewImageLayer(file, comp);
            }
        }
    }

    public static void exportLayersToPNGAsync() {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        boolean okPressed = SingleDirChooser.selectOutputDir(false);
        if (!okPressed) {
            return;
        }

        Composition comp = OpenComps.getActiveCompOrNull();

        CompletableFuture
                .supplyAsync(() -> exportLayersToPNG(comp), IOThread.getExecutor())
                .thenAcceptAsync(numImg -> Messages.showInStatusBar(
                        "Saved " + numImg + " images to " + Dirs.getLastSave())
                        , EventQueue::invokeLater)
                .exceptionally(Messages::showExceptionOnEDT);
    }

    private static int exportLayersToPNG(Composition comp) {
        assert !EventQueue.isDispatchThread() : "EDT thread";

        int numSavedImages = 0;
        for (int layerIndex = 0; layerIndex < comp.getNumLayers(); layerIndex++) {
            Layer layer = comp.getLayer(layerIndex);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getImage();

                saveLayerImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            } else if (layer instanceof TextLayer) {
                TextLayer textLayer = (TextLayer) layer;
                BufferedImage image = textLayer.createRasterizedImage();

                saveLayerImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                BufferedImage image = mask.getImage();
                saveLayerImage(image, layer.getName() + "_mask", layerIndex);
                numSavedImages++;
            }
        }
        return numSavedImages;
    }

    private static void saveLayerImage(BufferedImage image,
                                       String layerName,
                                       int layerIndex) {
        assert !EventQueue.isDispatchThread() : "EDT thread";

        File outputDir = Dirs.getLastSave();
        String fileName = format("%03d_%s.%s", layerIndex,
                Utils.toFileName(layerName), "png");
        File file = new File(outputDir, fileName);
        saveImageToFile(image, new SaveSettings(OutputFormat.PNG, file));
    }

    public static void saveCurrentImageInAllFormats() {
        Composition comp = OpenComps.getActiveCompOrNull();

        boolean canceled = !SingleDirChooser.selectOutputDir(false);
        if (canceled) {
            return;
        }
        File saveDir = Dirs.getLastSave();
        if (saveDir != null) {
            OutputFormat[] outputFormats = OutputFormat.values();
            for (OutputFormat outputFormat : outputFormats) {
                File f = new File(saveDir, "all_formats." + outputFormat.toString());
                SaveSettings saveSettings = new SaveSettings(outputFormat, f);
                comp.saveAsync(saveSettings, false).join();
            }
        }
    }

    public static void saveJpegWithQuality(JpegSettings settings) {
        try {
            FileChoosers.initSaveChooser();
            FileChoosers.setOnlyOneSaveExtension(FileChoosers.jpegFilter);

            Composition comp = OpenComps.getActiveCompOrNull();
            FileChoosers.showSaveChooserAndSaveComp(comp, settings);
        } finally {
            FileChoosers.setDefaultSaveExtensions();
        }
    }
}

