/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import org.xml.sax.SAXException;
import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class OpenSaveManager {
    private static final int CURRENT_PXC_VERSION_NUMBER = 0x03;
    private static final float DEFAULT_JPEG_QUALITY = 0.87f;
    private static float jpegQuality = DEFAULT_JPEG_QUALITY;

    /**
     * Utility class with static methods
     */
    private OpenSaveManager() {
    }

    public static void openFile(File file) {
        assert SwingUtilities.isEventDispatchThread();
        Runnable r = () -> {
            Composition comp = createCompositionFromFile(file);
            if(comp != null) { // there was no decoding problem
                AppLogic.addComposition(comp);
            }
        };
        Utils.executeWithBusyCursor(r);

        RecentFilesMenu.getInstance().addFile(file);
    }

    public static Composition createCompositionFromFile(File file) {
        String ext = FileExtensionUtils.getFileExtension(file.getName());
        if ("pxc".equals(ext)) {
            return openLayered(file, "pxc");
        } else if ("ora".equals(ext)) {
            return openLayered(file, "ora");
        } else {
            return openSimpleFile(file);
        }
    }

    // opens an a file with a single-layer image format
    private static Composition openSimpleFile(File file) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(file);
        } catch (IOException ex) {
            Messages.showException(ex);
        }
        if (img == null) {
            String message = String.format("Could not load \"%s\" as an image file", file.getName());
            Messages.showError("Error", message);
            return null;
        }

        Composition comp = Composition.fromImage(img, file, null);
        return comp;
    }

    private static Composition openLayered(File selectedFile, String type) {
        Composition comp = null;
        try {
            switch (type) {
                case "pxc":
                    comp = deserializeComposition(selectedFile);
                    break;
                case "ora":
                    comp = OpenRaster.readOpenRaster(selectedFile);
                    break;
                default:
                    throw new IllegalStateException("type = " + type);
            }
        } catch (NotPxcFormatException | ParserConfigurationException | IOException | SAXException e) {
            Messages.showException(e);
        }
        return comp;
    }

    public static boolean save(boolean saveAs) {
        Composition comp = ImageComponents.getActiveCompOrNull();
        return save(comp, saveAs);
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    private static boolean save(Composition comp, boolean saveAs) {
        boolean needsFileChooser = saveAs || (comp.getFile() == null);
        if (needsFileChooser) {
            return FileChoosers.saveWithFileChooser(comp);
        } else {
            File file = comp.getFile();
            OutputFormat outputFormat = OutputFormat.valueFromFile(file);
            outputFormat.saveComposition(comp, file, true);
            return true;
        }
    }

    public static void saveImageToFile(File selectedFile, BufferedImage image, String format) {
        Objects.requireNonNull(selectedFile);
        Objects.requireNonNull(image);
        Objects.requireNonNull(format);

        Runnable r = () -> {
            try {
                if ("jpg".equals(format)) {
                    JpegOutput.writeJPG(image, selectedFile, jpegQuality);
                } else {
                    ImageIO.write(image, format, selectedFile);
                }
            } catch (IOException e) {
                if (e.getMessage().contains("another process")) {
                    String msg = String.format("Cannot save to\n%s\nbecause this file is being used by another program.",
                            selectedFile.getAbsolutePath());
                    Messages.showError("Cannot save", msg);
                } else {
                    Messages.showException(e);
                }
            }
        };
        Utils.executeWithBusyCursor(r);
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

    public static void serializePXC(Composition comp, File f) {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(new byte[]{(byte) 0xAB, (byte) 0xC4, CURRENT_PXC_VERSION_NUMBER});

            try (GZIPOutputStream gz = new GZIPOutputStream(fos)) {
                try (ObjectOutput oos = new ObjectOutputStream(gz)) {
                    oos.writeObject(comp);
                    oos.flush();
                }
            }
        } catch (IOException e) {
            Messages.showException(e);
        }
    }

    private static Composition deserializeComposition(File file) throws NotPxcFormatException {
        Composition comp = null;
        try (FileInputStream fis = new FileInputStream(file)) {
            int firstByte = fis.read();
            int secondByte = fis.read();
            if (firstByte == 0xAB && secondByte == 0xC4) {
                // identification bytes OK
            } else {
                throw new NotPxcFormatException(file.getName() + " is not in the pxc format.");
            }
            int versionByte = fis.read();
            if (versionByte == 0) {
                throw new NotPxcFormatException(file.getName() + " is in an obsolete pxc format, it can only be opened in the old beta Pixelitor versions 0.9.2-0.9.7");
            }
            if (versionByte == 1) {
                throw new NotPxcFormatException(file.getName() + " is in an obsolete pxc format, it can only be opened in the old beta Pixelitor version 0.9.8");
            }
            if (versionByte == 2) {
                throw new NotPxcFormatException(file.getName() + " is in an obsolete pxc format, it can only be opened in the old Pixelitor versions 0.9.9-1.1.2");
            }
            if (versionByte > 3) {
                throw new NotPxcFormatException(file.getName() + " has unknown version byte " + versionByte);
            }

            try (GZIPInputStream gs = new GZIPInputStream(fis)) {
                try (ObjectInput ois = new ObjectInputStream(gs)) {
                    comp = (Composition) ois.readObject();

                    // file is transient in Composition because the pxc file can be renamed
                    comp.setFile(file);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Messages.showException(e);
        }

        return comp;
    }

    public static void openAllImagesInDir(File dir) {
        File[] files = FileExtensionUtils.getAllSupportedFilesInDir(dir);
        if (files != null) {
            for (File file : files) {
                openFile(file);
            }
        }
    }

    public static void exportLayersToPNG() {
        boolean okPressed = SingleDirChooserPanel.selectOutputDir(false);
        if (!okPressed) {
            return;
        }

        Composition comp = ImageComponents.getActiveCompOrNull();
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getImage();

                File outputDir = FileChoosers.getLastSaveDir();

                String fileName = String.format("%03d_%s.%s", i, Utils.toFileName(layer.getName()), "png");

                File file = new File(outputDir, fileName);
                saveImageToFile(file, image, "png");
            }
        }
    }

    public static void saveCurrentImageInAllFormats() {
        Composition comp = ImageComponents.getActiveCompOrNull();

        boolean cancelled = !SingleDirChooserPanel.selectOutputDir(false);
        if (cancelled) {
            return;
        }
        File saveDir = FileChoosers.getLastSaveDir();
        if (saveDir != null) {
            OutputFormat[] outputFormats = OutputFormat.values();
            for (OutputFormat outputFormat : outputFormats) {
                File f = new File(saveDir, "all_formats." + outputFormat.toString());
                outputFormat.saveComposition(comp, f, false);
            }
        }
    }

    // called by the "Save All Images to Folder..." menu
    public static void saveAllImagesToDir() {
        boolean cancelled = !SingleDirChooserPanel.selectOutputDir(true);
        if (cancelled) {
            return;
        }

        OutputFormat outputFormat = OutputFormat.getLastOutputFormat();
        File saveDir = FileChoosers.getLastSaveDir();
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
                    String fileName = String.format("%04d_%s.%s", i, Utils.toFileName(comp.getName()), outputFormat.toString());
                    File f = new File(saveDir, fileName);
                    progressMonitor.setNote("Saving " + fileName);
                    outputFormat.saveComposition(comp, f, false);
                }
                progressMonitor.close();
                return null;
            } // end of doInBackground()
        };
        worker.execute();
    }

    public static void saveJpegWithQuality(float quality) {
        try {
            FileChoosers.initSaveFileChooser();
            FileChoosers.setOnlyOneSaveExtension(FileChoosers.jpegFilter);

            jpegQuality = quality;
            FileChoosers.showSaveFileChooserAndSaveComp(ImageComponents.getActiveCompOrNull());
        } finally {
            FileChoosers.setDefaultSaveExtensions();
            jpegQuality = DEFAULT_JPEG_QUALITY;
        }
    }

    public static void afterSaveActions(Composition comp, File file, boolean addToRecentMenus) {
        // TODO for a multilayered image this should be set only if it was saved in a layered format?
        comp.setDirty(false);

        comp.setFile(file);
        if(addToRecentMenus) {
            RecentFilesMenu.getInstance().addFile(file);
        }
        Messages.showFileSavedMessage(file);
    }
}

