/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.io;

import org.xml.sax.SAXException;
import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.PixelitorWindow;
import pixelitor.automate.SingleDirChooserPanel;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.utils.Dialogs;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class OpenSaveManager {
    public static final int CURRENT_PXC_VERSION_NUMBER = 0x03;
    private static final float DEFAULT_JPEG_QUALITY = 0.87f;
    private static float jpegQuality = DEFAULT_JPEG_QUALITY;

    /**
     * Utility class with static methods
     */
    private OpenSaveManager() {
    }

    public static void openFile(final File selectedFile) {
        String ext = FileExtensionUtils.getFileExtension(selectedFile.getName());
        if ("pxc".equals(ext)) {
            openLayered(selectedFile, "pxc");
        } else if ("ora".equals(ext)) {
            openLayered(selectedFile, "ora");
        } else {
            openOneLayeredFile(selectedFile);
        }
        RecentFilesMenu.getInstance().addFile(selectedFile);
    }

    private static void openOneLayeredFile(final File selectedFile) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                BufferedImage img = null;
                try {
                    img = ImageIO.read(selectedFile);
                } catch (IOException ex) {
                    Dialogs.showExceptionDialog(ex);
                }
                if (img == null) {
                    JOptionPane.showMessageDialog(PixelitorWindow.getInstance(), "Could not load \"" + selectedFile.getName() + "\" as an image file", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                img = ImageUtils.transformToCompatibleImage(img);
                PixelitorWindow.getInstance().addNewImage(img, selectedFile, null);
            }
        };
        Utils.executeWithBusyCursor(r, false);
    }


    public static void save(boolean saveAs) {
        try {
            Composition comp = ImageComponents.getActiveComp();
            save(comp, saveAs);
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    private static boolean save(Composition comp, boolean saveAs) {
        boolean needsFileChooser = saveAs || (comp.getFile() == null);
        if (needsFileChooser) {
            return FileChooser.saveWithFileChooser(comp);
        } else {
            File file = comp.getFile();
            OutputFormat outputFormat = OutputFormat.valueFromFile(file);
            outputFormat.saveComposition(comp, file);
            return true;
        }
    }

    public static void saveImageToFile(final File selectedFile, final BufferedImage image, final String format) {
        if (selectedFile == null) {
            throw new IllegalArgumentException("selectedFile is null");
        }
        if (image == null) {
            throw new IllegalArgumentException("image is null");
        }
        if (format == null) {
            throw new IllegalArgumentException("format is null");
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if ("jpg".equals(format)) {
                        JpegOutput.writeJPG(image, selectedFile, jpegQuality);
                    } else {
                        ImageIO.write(image, format, selectedFile);
                    }
                } catch (IOException e) {
                    Dialogs.showExceptionDialog(e);
                }
            }
        };
        Utils.executeWithBusyCursor(r, false);
    }

    public static void warnAndCloseImage(ImageComponent imageComponent) {
        try {
            Composition comp = imageComponent.getComp();
            if (comp.isDirty()) {
                Object[] options = {"Save",
                        "Don't save",
                        "Cancel"};
                Object message = new JLabel("<html><b>Do you want to save the changes made to " + comp.getName() + "?</b><br>Your changes will be lost if you don't save them.</html>");
                int answer = JOptionPane.showOptionDialog(PixelitorWindow.getInstance(), message,
                        "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (answer == JOptionPane.YES_OPTION) { // save
                    boolean fileSaved = OpenSaveManager.save(comp, false);
                    if (fileSaved) {
                        imageComponent.close();
                    }
                } else if (answer == JOptionPane.NO_OPTION) { // don't save
                    imageComponent.close();
                } else if (answer == JOptionPane.CANCEL_OPTION) { // cancel
                    // do nothing
                } else {
                    // do nothing
                }
            } else {
                imageComponent.close();
            }
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }

    public static void warnAndCloseAllImages() {
        List<ImageComponent> imageComponents = ImageComponents.getImageComponents();
        // make a copy because items will be removed from the original while iterating
        Iterable<ImageComponent> tmpCopy = new ArrayList<>(imageComponents);
        for (ImageComponent component : tmpCopy) {
            warnAndCloseImage(component);
        }
    }

    public static void serializePXC(Composition comp, File f) {
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);

            fos.write(new byte[]{(byte) 0xAB, (byte) 0xC4, CURRENT_PXC_VERSION_NUMBER});

            GZIPOutputStream gz = new GZIPOutputStream(fos);
            oos = new ObjectOutputStream(gz);
            oos.writeObject(comp);
        } catch (IOException e) {
            Dialogs.showExceptionDialog(e);
        } finally {
            try {
                oos.flush();
                oos.close();
                fos.close();
            } catch (IOException e) {
                Dialogs.showExceptionDialog(e);
            }
        }
    }

    private static Composition deserializeComposition(File f) throws NotPxcFormatException {
        Composition comp = null;
        try {
            FileInputStream fis = new FileInputStream(f);

            int firstByte = fis.read();
            int secondByte = fis.read();
            if (firstByte == 0xAB && secondByte == 0xC4) {
                // identification bytes OK
            } else {
                throw new NotPxcFormatException(f.getName() + " is not in the pxc format.");
            }
            int versionByte = fis.read();
            if (versionByte == 0) {
                throw new NotPxcFormatException(f.getName() + " is in an obsolete pxc format, it can only be opened in the old beta Pixelitor versions 0.9.2-0.9.7");
            }
            if (versionByte == 1) {
                throw new NotPxcFormatException(f.getName() + " is in an obsolete pxc format, it can only be opened in the old beta Pixelitor version 0.9.8");
            }
            if (versionByte == 2) {
                throw new NotPxcFormatException(f.getName() + " is in an obsolete pxc format, it can only be opened in the old Pixelitor versions 0.9.9-1.1.2");
            }
            if (versionByte > 3) {
                throw new NotPxcFormatException(f.getName() + " has unknown version byte " + versionByte);
            }

            GZIPInputStream gs = new GZIPInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(gs);
            comp = (Composition) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            Dialogs.showExceptionDialog(e);
        }
        return comp;
    }

    private static void openLayered(final File selectedFile, final String type) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    Composition comp;
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
                    PixelitorWindow.getInstance().addLayeredComposition(comp, selectedFile);
                } catch (NotPxcFormatException | ParserConfigurationException | IOException | SAXException e) {
                    Dialogs.showExceptionDialog(e);
                }
            }
        };
        Utils.executeWithBusyCursor(r, false);
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

        Composition comp = ImageComponents.getActiveComp();
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getBufferedImage();

                File outputDir = FileChooser.getLastSaveDir();

                String fileName = String.format("%03d_%s.%s", i, Utils.toFileName(layer.getName()), "png");

                File file = new File(outputDir, fileName);
                saveImageToFile(file, image, "png");
            }
        }
    }

    public static void saveCurrentImageInAllFormats() {
        Composition comp = ImageComponents.getActiveComp();

        boolean cancelled = !SingleDirChooserPanel.selectOutputDir(false);
        if (cancelled) {
            return;
        }
        final File saveDir = FileChooser.getLastSaveDir();
        if (saveDir != null) {
            OutputFormat[] outputFormats = OutputFormat.values();
            for (OutputFormat outputFormat : outputFormats) {
                File f = new File(saveDir, "all_formats." + outputFormat.toString());
                outputFormat.saveComposition(comp, f);
            }
        }
    }

    public static void saveAllImagesToDir() {
        boolean cancelled = !SingleDirChooserPanel.selectOutputDir(true);
        if (cancelled) {
            return;
        }

        final OutputFormat outputFormat = OutputFormat.getLastOutputFormat();
        final File saveDir = FileChooser.getLastSaveDir();
        final List<ImageComponent> imageComponents = ImageComponents.getImageComponents();

        final ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Saving All Images to Directory");

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
                    outputFormat.saveComposition(comp, f);
                }
                progressMonitor.close();
                return null;
            } // end of doInBackground()
        };
        worker.execute();
    }

    public static void saveJpegWithQuality(float quality) {
        try {
            FileChooser.initSaveFileChooser();
            FileChooser.setOnlyOneSaveExtension(FileChooser.jpegFilter);

            jpegQuality = quality;
            FileChooser.showSaveFileChooserAndSaveComp(ImageComponents.getActiveComp());
        } finally {
            FileChooser.setDefaultSaveExtensions();
            jpegQuality = DEFAULT_JPEG_QUALITY;
        }
    }

    public static void afterSaveActions(Composition comp, File file) {
        // TODO for a multilayered image this should be set only if it was saved in a layered format?
        comp.setDirty(false);

        comp.setFile(file);
        RecentFilesMenu.getInstance().addFile(file);
        AppLogic.showFileSavedMessage(file);
    }
}

