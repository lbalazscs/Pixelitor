/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.ThreadPool;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TextLayer;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class with static methods related to opening and saving files.
 */
public class OpenSaveManager {
    private static JpegSettings jpegSettings = JpegSettings.DEFAULTS;

    private OpenSaveManager() {
    }

    public static CompletableFuture<Composition> openFileAsync(File file) {
        CompletableFuture<Composition> cf =
                loadCompFromFileAsync(file);
        cf.thenAcceptAsync(comp -> {
            if (comp != null) { // there was no decoding problem
                ImageComponents.addAsNewImage(comp);
                RecentFilesMenu.getInstance().addFile(file);
                Messages.showInStatusBar("<html><b>" + file.getName() + "</b> was opened.");
            }
        }, EventQueue::invokeLater);
        return cf;
    }

    public static CompletableFuture<Composition> loadCompFromFileAsync(File file) {
        CompletableFuture<Composition> cf;

        String ext = FileExtensionUtils.getExt(file.getName());
        if ("pxc".equals(ext)) {
            cf = loadLayered(file, "pxc");
        } else if ("ora".equals(ext)) {
            cf = loadLayered(file, "ora");
        } else {
            cf = loadSimpleFile(file);
        }

        return cf.exceptionally(Messages::showExceptionOnEDT);
    }

    // loads an a file with a single-layer image format
    private static CompletableFuture<Composition> loadSimpleFile(File file) {
        return CompletableFuture.supplyAsync(
                () -> TrackedIO.uncheckedRead(file), ThreadPool.getExecutor())
                .handle((img, e) -> handleDecodingError(file, img))
                .thenApply(img -> Composition.fromImage(img, file, null));
    }

    private static BufferedImage handleDecodingError(File file, BufferedImage img) {
        if (img != null) {
            return img;
        }

        // if we have a null image here, it means a decoding error,
        // even if no exception was thrown
        EventQueue.invokeLater(() -> {
            String message = String.format("Could not load \"%s\" as an image file.", file.getName());

            String ext = FileExtensionUtils.getExt(file.getName());
            if (ext.startsWith("tif") && Utils.getCurrentMainJavaVersion() == 8) {
                message += "\nNote that TIFF files are supported only when Pixelitor is running on Java 9+.";
                message += "\nCurrently it is running on Java 8.";
            }
            Messages.showError("Error", message);
        });
        return null;
    }

    private static CompletableFuture<Composition> loadLayered(File selectedFile, String type) {
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
                ThreadPool.getExecutor());
    }

    public static void save(boolean saveAs) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        save(comp, saveAs);
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    private static boolean save(Composition comp, boolean saveAs) {
        boolean needsFileChooser = saveAs || (comp.getFile() == null);
        if (needsFileChooser) {
            return FileChoosers.saveWithChooser(comp);
        } else {
            File file = comp.getFile();
            OutputFormat outputFormat = OutputFormat.fromFile(file);
            comp.saveAsync(file, outputFormat, true);
            return true;
        }
    }

    public static void saveImageToFile(File selectedFile, BufferedImage image, OutputFormat format) {
        Objects.requireNonNull(selectedFile);
        Objects.requireNonNull(image);
        Objects.requireNonNull(format);

        try {
            if (format == OutputFormat.JPG) {
                JpegOutput.writeJPG(image, selectedFile, jpegSettings);
            } else {
                TrackedIO.write(image, format.toString(), selectedFile);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("another process")) {
                String msg = String
                        .format("Cannot save to\n%s\nbecause this file is being used by another program.",
                                selectedFile.getAbsolutePath());
                Messages.showError("Cannot save", msg);
            } else {
                Messages.showException(e);
            }
        }
    }

    public static void warnAndCloseImage(ImageComponent ic) {
        try {
            Composition comp = ic.getComp();
            if (comp.isDirty()) {
                Object[] options = {"Save",
                        "Don't Save",
                        "Cancel"};
                String question = String.format("<html><b>Do you want to save the changes made to %s?</b>" +
                        "<br>Your changes will be lost if you don't save them.</html>", comp.getName());

                GlobalKeyboardWatch.setDialogActive(true);
                int answer = JOptionPane.showOptionDialog(PixelitorWindow.getInstance(), new JLabel(question),
                        "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                GlobalKeyboardWatch.setDialogActive(false);

                if (answer == JOptionPane.YES_OPTION) { // save
                    boolean fileSaved = OpenSaveManager.save(comp, false);
                    if (fileSaved) {
                        ic.close();
                    }
                } else if (answer == JOptionPane.NO_OPTION) { // don't save
                    ic.close();
                } else if (answer == JOptionPane.CANCEL_OPTION) { // cancel
                    // do nothing
                } else { // dialog closed by pressing X
                    // do nothing
                }
            } else {
                ic.close();
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    public static void warnAndCloseAllImages() {
        List<ImageComponent> imageComponents = ImageComponents.getICList();
        // make a copy because items will be removed from the original while iterating
        Iterable<ImageComponent> tmpCopy = new ArrayList<>(imageComponents);
        for (ImageComponent component : tmpCopy) {
            warnAndCloseImage(component);
        }
    }

    public static void openAllImagesInDirAsync(File dir) {
        File[] files = FileExtensionUtils.listSupportedInputFilesIn(dir);
        if (files != null) {
            for (File file : files) {
                // TODO make sure only one file is read at a time
                openFileAsync(file);
            }
        }
    }

    public static void exportLayersToPNG() {
        boolean okPressed = SingleDirChooserPanel.selectOutputDir(false);
        if (!okPressed) {
            return;
        }

        Composition comp = ImageComponents.getActiveCompOrNull();
        int numSavedImages = 0;

        for (int layerIndex = 0; layerIndex < comp.getNumLayers(); layerIndex++) {
            Layer layer = comp.getLayer(layerIndex);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getImage();

                saveImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            } else if (layer instanceof TextLayer) {
                TextLayer textLayer = (TextLayer) layer;
                BufferedImage image = textLayer.createRasterizedImage();

                saveImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                BufferedImage image = mask.getImage();
                saveImage(image, layer.getName() + "_mask", layerIndex);
                numSavedImages++;
            }
        }
        Messages.showInStatusBar("Saved " + numSavedImages
                + " images to " + Directories.getLastSaveDir());
    }

    private static void saveImage(BufferedImage image, String layerName, int layerIndex) {
        File outputDir = Directories.getLastSaveDir();
        String fileName = String.format("%03d_%s.%s", layerIndex, Utils.toFileName(layerName), "png");
        File file = new File(outputDir, fileName);
        saveImageToFile(file, image, OutputFormat.PNG);
    }

    public static void saveCurrentImageInAllFormats() {
        Composition comp = ImageComponents.getActiveCompOrNull();

        boolean canceled = !SingleDirChooserPanel.selectOutputDir(false);
        if (canceled) {
            return;
        }
        File saveDir = Directories.getLastSaveDir();
        if (saveDir != null) {
            OutputFormat[] outputFormats = OutputFormat.values();
            for (OutputFormat outputFormat : outputFormats) {
                File f = new File(saveDir, "all_formats." + outputFormat.toString());
                comp.saveAsync(f, outputFormat, false).join();
            }
        }
    }

    public static void saveAllImagesToDir() {
        boolean cancelled = !SingleDirChooserPanel.selectOutputDir(true);
        if (cancelled) {
            return;
        }

        OutputFormat outputFormat = OutputFormat.getLastUsed();
        File saveDir = Directories.getLastSaveDir();
        List<ImageComponent> imageComponents = ImageComponents.getICList();

        ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Saving All Images to Folder");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                for (int i = 0; i < imageComponents.size(); i++) {
                    progressMonitor.setProgress((int) ((float) i * 100 / imageComponents.size()));
                    if (progressMonitor.isCanceled()) {
                        break;
                    }

                    ImageComponent ic = imageComponents.get(i);
                    Composition comp = ic.getComp();
                    String fileName = String
                            .format("%04d_%s.%s", i, Utils.toFileName(comp.getName()), outputFormat.toString());
                    File f = new File(saveDir, fileName);
                    progressMonitor.setNote("Saving " + fileName);
                    comp.saveAsync(f, outputFormat, false).join();
                }
                progressMonitor.close();
                return null;
            } // end of doInBackground()
        };
        worker.execute();
    }

    public static void saveJpegWithQuality(JpegSettings settings) {
        try {
            FileChoosers.initSaveChooser();
            FileChoosers.setOnlyOneSaveExtension(FileChoosers.jpegFilter);

            jpegSettings = settings;
            FileChoosers.showSaveChooserAndSaveComp(ImageComponents.getActiveCompOrNull());
        } finally {
            FileChoosers.setDefaultSaveExtensions();
            jpegSettings = JpegSettings.DEFAULTS;
        }
    }
}

