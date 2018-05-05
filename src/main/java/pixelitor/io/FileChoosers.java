/*
 * Copyright 2018 Laszlo Balazs-Csiki
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
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.CustomFileChooser;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.utils.Messages;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * Utility class with static methods related to file choosers
 */
public class FileChoosers {
    private static JFileChooser openChooser;
    private static CustomFileChooser saveChooser;

    public static final FileFilter jpegFilter = new FileNameExtensionFilter("JPEG files", "jpg", "jpeg");
    private static final FileFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
    private static final FileFilter bmpFilter = new FileNameExtensionFilter("BMP files", "bmp");
    public static final FileNameExtensionFilter gifFilter = new FileNameExtensionFilter("GIF files", "gif");
    private static final FileFilter pxcFilter = new FileNameExtensionFilter("PXC files", "pxc");
    public static final FileFilter oraFilter = new FileNameExtensionFilter("OpenRaster files", "ora");

    private static final FileFilter[] DEFAULT_OPEN_SAVE_FILTERS = {bmpFilter, gifFilter, jpegFilter, oraFilter, pngFilter, pxcFilter};

    private static final FileFilter[] NON_DEFAULT_OPEN_SAVE_FILTERS = {};

    private FileChoosers() {
    }

    private static void initOpenChooser() {
        assert SwingUtilities.isEventDispatchThread();
        if (openChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            openChooser = new JFileChooser(Directories.getLastOpenDir());
            openChooser.setName("open");

            setDefaultOpenExtensions();

            ImagePreviewPanel preview = new ImagePreviewPanel();
            openChooser.setAccessory(preview);
            openChooser.addPropertyChangeListener(preview);
        }
    }

    public static void initSaveChooser() {
        assert SwingUtilities.isEventDispatchThread();
        if (saveChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            saveChooser = new CustomFileChooser(Directories.getLastSaveDir());
            saveChooser.setName("save");
            saveChooser.setDialogTitle("Save As");

            setDefaultSaveExtensions();
        }
    }

    public static void open() {
        initOpenChooser();

        GlobalKeyboardWatch.setDialogActive(true);
        int status = openChooser.showOpenDialog(PixelitorWindow.getInstance());
        GlobalKeyboardWatch.setDialogActive(false);

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            Directories.setLastOpenDir(selectedFile.getParentFile());

            if (FileExtensionUtils.hasSupportedInputExt(fileName)) {
                OpenSaveManager.openFile(selectedFile);
            } else { // unsupported extension
                handleUnsupportedExtensionWhileOpening(fileName);
            }
        } else if (status == JFileChooser.CANCEL_OPTION) {
            // cancelled
        }
    }

    private static void handleUnsupportedExtensionWhileOpening(String fileName) {
        String extension = FileExtensionUtils.getExt(fileName);
        String msg = "Could not open " + fileName + ", because ";
        if (extension == null) {
            msg += "it has no extension.";
        } else {
            msg += "files of type " + extension + " are not supported.";
        }
        Messages.showError("Error", msg);
    }

    public static boolean showSaveChooserAndSaveComp(Composition comp) {
        String defaultFileName = FileExtensionUtils.stripExtension(comp.getName());
        saveChooser.setSelectedFile(new File(defaultFileName));

        File customSaveDir = null;
        File file = comp.getFile();
        if (file != null) {
            customSaveDir = file.getParentFile();
            saveChooser.setCurrentDirectory(customSaveDir);
        }

        GlobalKeyboardWatch.setDialogActive(true);
        int status = saveChooser.showSaveDialog(PixelitorWindow.getInstance());
        GlobalKeyboardWatch.setDialogActive(false);

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveChooser.getSelectedFile();

            if (customSaveDir == null) {
                // if the comp had no file, and lastSaveDir was used,
                // then update lastSaveDir
                Directories.setLastSaveDir(selectedFile.getParentFile());
            } else {
                // if a custom save directory (the file dir) was used,
                // reset the directory stored inside the chooser
                saveChooser.setCurrentDirectory(Directories.getLastSaveDir());
            }

            String extension = saveChooser.getExtension();
            OutputFormat outputFormat = OutputFormat.fromExtension(extension);
            outputFormat.saveComp(comp, selectedFile, true);
            return true;
        }

        return false;
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean saveWithChooser(Composition comp) {
        initSaveChooser();

        String defaultExt = FileExtensionUtils.getExt(comp.getName());
        saveChooser.setFileFilter(getFileFilterForExtension(defaultExt));

        return showSaveChooserAndSaveComp(comp);
    }

    private static FileFilter getFileFilterForExtension(String ext) {
        if(ext == null) {
            return jpegFilter; // default
        }
        ext = ext.toLowerCase();
        switch (ext) {
            case "jpg":
                return jpegFilter;
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
        setupFilterToOnlyOneFormat(saveChooser, filter);
    }

    private static void addDefaultFilters(JFileChooser chooser) {
        // remove first the non-default filters in case they are there
        for (FileFilter filter : NON_DEFAULT_OPEN_SAVE_FILTERS) {
            chooser.removeChoosableFileFilter(filter);
        }

        for (FileFilter filter : DEFAULT_OPEN_SAVE_FILTERS) {
            chooser.addChoosableFileFilter(filter);
        }
    }

    private static void setupFilterToOnlyOneFormat(JFileChooser chooser, FileFilter chosenFilter) {
        for (FileFilter filter : DEFAULT_OPEN_SAVE_FILTERS) {
            if(filter != chosenFilter) {
                chooser.removeChoosableFileFilter(filter);
            }
        }

        // if we want to set up a non-default filter, it has to be added now
        for (FileFilter filter : NON_DEFAULT_OPEN_SAVE_FILTERS) {
            if(chosenFilter == filter) {
                chooser.addChoosableFileFilter(chosenFilter);
            }
        }

        chooser.setFileFilter(chosenFilter);
    }

    public static File selectSaveFileForSpecificFormat(FileFilter fileFilter) {
        File selectedFile = null;
        try {
            initSaveChooser();
            setupFilterToOnlyOneFormat(saveChooser, fileFilter);

            GlobalKeyboardWatch.setDialogActive(true);
            int status = saveChooser.showSaveDialog(PixelitorWindow.getInstance());
            GlobalKeyboardWatch.setDialogActive(false);

            if (status == JFileChooser.APPROVE_OPTION) {
                selectedFile = saveChooser.getSelectedFile();
                Directories.setLastSaveDir(selectedFile.getParentFile());
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
