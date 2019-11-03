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
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.gui.utils.SaveFileChooser;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressPanel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;

import static pixelitor.utils.Utils.getJavaMainVersion;

/**
 * Utility class with static methods related to file choosers
 */
public class FileChoosers {
    private static JFileChooser openChooser;
    private static SaveFileChooser saveChooser;

    public static final FileFilter jpegFilter = new FileNameExtensionFilter("JPEG files", "jpg", "jpeg");
    private static final FileFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
    private static final FileFilter bmpFilter = new FileNameExtensionFilter("BMP files", "bmp");
    public static final FileNameExtensionFilter gifFilter = new FileNameExtensionFilter("GIF files", "gif");
    private static final FileFilter tiffFilter = new FileNameExtensionFilter("TIFF files", "tiff", "tif");
    private static final FileFilter pxcFilter = new FileNameExtensionFilter("PXC files", "pxc");
    public static final FileFilter oraFilter = new FileNameExtensionFilter("OpenRaster files", "ora");

    private static final FileFilter[] OPEN_SAVE_FILTERS;

    static {
        if (getJavaMainVersion() == 8) {
            OPEN_SAVE_FILTERS = new FileFilter[]{
                    bmpFilter, gifFilter, jpegFilter, oraFilter, pngFilter, pxcFilter};
        } else {
            OPEN_SAVE_FILTERS = new FileFilter[]{
                    bmpFilter, gifFilter, jpegFilter, oraFilter, pngFilter, pxcFilter, tiffFilter};
        }
    }

    private FileChoosers() {
    }

    private static void initOpenChooser() {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        if (openChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            openChooser = new JFileChooser(Dirs.getLastOpen()) {
                @Override
                public void approveSelection() {
                    File f = getSelectedFile();
                    if (!f.exists()) {
                        Dialogs.showErrorDialog("File not found",
                                "<html>The file <b>" + f.getAbsolutePath()
                                        + " </b> does not exist. " +
                                        "<br>Check the file name and try again."
                        );
                        return;
                    }
                    super.approveSelection();
                }
            };
            openChooser.setName("open");

            setDefaultOpenExtensions();

            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            ProgressPanel progressPanel = new ProgressPanel();
            ImagePreviewPanel preview = new ImagePreviewPanel(progressPanel);
            p.add(preview, BorderLayout.CENTER);
            p.add(progressPanel, BorderLayout.SOUTH);

            openChooser.setAccessory(p);
            openChooser.addPropertyChangeListener(preview);
        }
    }

    public static void initSaveChooser() {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        if (saveChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            saveChooser = new SaveFileChooser(Dirs.getLastSave());
            saveChooser.setName("save");
            saveChooser.setDialogTitle("Save As");

            setDefaultSaveExtensions();
        }
    }

    public static void openAsync() {
        initOpenChooser();

        GlobalEvents.dialogOpened("Open");
        int status = openChooser.showOpenDialog(PixelitorWindow.getInstance());
        GlobalEvents.dialogClosed("Open");

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            Dirs.setLastOpen(selectedFile.getParentFile());

            if (FileUtils.hasSupportedInputExt(fileName)) {
                OpenSave.openFileAsync(selectedFile);
            } else { // unsupported extension
                handleUnsupportedExtensionWhileOpening(fileName);
            }
        } else if (status == JFileChooser.CANCEL_OPTION) {
            // cancelled
        }
    }

    private static void handleUnsupportedExtensionWhileOpening(String fileName) {
        String extension = FileUtils.findExtension(fileName).orElse("");
        String msg = "Could not open " + fileName + ", because ";
        if (extension.isEmpty()) {
            msg += "it has no extension.";
        } else {
            msg += "files of type " + extension + " are not supported.";
        }
        Messages.showError("Error", msg);
    }

    public static boolean showSaveChooserAndSaveComp(Composition comp,
                                                     SaveSettings settings) {
        String defaultFileName = FileUtils.stripExtension(comp.getName());
        saveChooser.setSelectedFile(new File(defaultFileName));

        File customSaveDir = null;
        File file = comp.getFile();
        if (file != null) {
            customSaveDir = file.getParentFile();
            saveChooser.setCurrentDirectory(customSaveDir);
        }

        GlobalEvents.dialogOpened("Save");
        int status = saveChooser.showSaveDialog(PixelitorWindow.getInstance());
        GlobalEvents.dialogClosed("Save");

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveChooser.getSelectedFile();

            if (customSaveDir == null) {
                // if the comp had no file, and lastSaveDir was used,
                // then update lastSaveDir
                Dirs.setLastSave(selectedFile.getParentFile());
            } else {
                // if a custom save directory (the file dir) was used,
                // reset the directory stored inside the chooser
                saveChooser.setCurrentDirectory(Dirs.getLastSave());
            }

            String extension = saveChooser.getExtension();
            OutputFormat outputFormat = OutputFormat.fromExtension(extension);
            settings.setOutputFormat(outputFormat);
            settings.setFile(selectedFile);
            comp.saveAsync(settings, true);
            return true;
        }

        return false;
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean saveWithChooser(Composition comp) {
        initSaveChooser();

        String defaultExt = FileUtils
                .findExtension(comp.getName())
                .orElse(OutputFormat.getLastUsed().toString());
        saveChooser.setFileFilter(getFileFilterForExtension(defaultExt));

        return showSaveChooserAndSaveComp(comp, new SaveSettings());
    }

    private static FileFilter getFileFilterForExtension(String ext) {
        ext = ext.toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
                return jpegFilter;
            case "png":
                return pngFilter;
            case "bmp":
                return bmpFilter;
            case "gif":
                return gifFilter;
            case "pxc":
                return pxcFilter;
            case "tif":
            case "tiff":
                return tiffFilter;
        }
        return jpegFilter; // default
    }

    private static void setDefaultOpenExtensions() {
        addDefaultFilters(openChooser);
    }

    public static void setDefaultSaveExtensions() {
        addDefaultFilters(saveChooser);
    }

    public static void setOnlyOneSaveExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(saveChooser, filter);
    }

    public static void setOnlyOneOpenExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(openChooser, filter);
    }

    private static void addDefaultFilters(JFileChooser chooser) {
        for (FileFilter filter : OPEN_SAVE_FILTERS) {
            chooser.addChoosableFileFilter(filter);
        }
    }

    private static void setupFilterToOnlyOneFormat(JFileChooser chooser,
                                                   FileFilter chosenFilter) {
        for (FileFilter filter : OPEN_SAVE_FILTERS) {
            if(filter != chosenFilter) {
                chooser.removeChoosableFileFilter(filter);
            }
        }

        chooser.setFileFilter(chosenFilter);
    }

    public static File selectSaveFileForSpecificFormat(FileFilter fileFilter) {
        File selectedFile = null;
        try {
            initSaveChooser();
            setupFilterToOnlyOneFormat(saveChooser, fileFilter);

            GlobalEvents.dialogOpened("Save");
            int status = saveChooser.showSaveDialog(PixelitorWindow.getInstance());
            GlobalEvents.dialogClosed("Save");

            if (status == JFileChooser.APPROVE_OPTION) {
                selectedFile = saveChooser.getSelectedFile();
                Dirs.setLastSave(selectedFile.getParentFile());
            }
            if (status == JFileChooser.CANCEL_OPTION) {
                // save cancelled
                return null;
            }
            return selectedFile;
        } finally {
            setDefaultSaveExtensions();
        }
    }
}
