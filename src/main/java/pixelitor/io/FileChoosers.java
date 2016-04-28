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

import pixelitor.Composition;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.CustomFileChooser;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FileChoosers {
    private static JFileChooser openFileChooser;
    private static CustomFileChooser saveFileChooser;

    private static File lastOpenDir = AppPreferences.loadLastOpenDir();
    private static File lastSaveDir = AppPreferences.loadLastSaveDir();

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

    private static void initOpenFileChooser() {
        assert SwingUtilities.isEventDispatchThread();
        if (openFileChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            openFileChooser = new JFileChooser(lastOpenDir);
            openFileChooser.setName("open");

            setDefaultOpenExtensions();

            ImagePreviewPanel preview = new ImagePreviewPanel();
            openFileChooser.setAccessory(preview);
            openFileChooser.addPropertyChangeListener(preview);
        }
    }

    public static void initSaveFileChooser() {
        assert SwingUtilities.isEventDispatchThread();
        if (saveFileChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            saveFileChooser = new CustomFileChooser(lastSaveDir);
            saveFileChooser.setName("save");
            saveFileChooser.setDialogTitle("Save As");

            setDefaultSaveExtensions();
        }
    }

    public static void open() {
        initOpenFileChooser();

        GlobalKeyboardWatch.setDialogActive(true);
        int status = openFileChooser.showOpenDialog(PixelitorWindow.getInstance());
        GlobalKeyboardWatch.setDialogActive(false);

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openFileChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            lastOpenDir = selectedFile.getParentFile();

            if (FileExtensionUtils.isSupportedExtension(fileName, FileExtensionUtils.SUPPORTED_INPUT_EXTENSIONS)) {
                OpenSaveManager.openFile(selectedFile);
            } else { // unsupported extension
                handleUnsupportedExtensionLoading(fileName);
            }
        } else if (status == JFileChooser.CANCEL_OPTION) {
            // cancelled
        }
    }

    private static void handleUnsupportedExtensionLoading(String fileName) {
        String extension = FileExtensionUtils.getFileExtension(fileName);
        String msg = "Could not load " + fileName + ", because ";
        if (extension == null) {
            msg += "it has no extension.";
        } else {
            msg += "files of type " + extension + " are not supported.";
        }
        Messages.showError("Error", msg);
    }

    public static boolean showSaveFileChooserAndSaveComp(Composition comp) {
        String defaultFileName = FileExtensionUtils.getFileNameWOExtension(comp.getName());
        saveFileChooser.setSelectedFile(new File(defaultFileName));

        File customSaveDir = null;
        File file = comp.getFile();
        if (file != null) {
            customSaveDir = file.getParentFile();
            saveFileChooser.setCurrentDirectory(customSaveDir);
        }

        GlobalKeyboardWatch.setDialogActive(true);
        int status = saveFileChooser.showSaveDialog(PixelitorWindow.getInstance());
        GlobalKeyboardWatch.setDialogActive(false);

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveFileChooser.getSelectedFile();

            if (customSaveDir == null) {
                // if the comp had no file, and lastSaveDir was used,
                // then update lastSaveDir
                lastSaveDir = selectedFile.getParentFile();
            } else {
                // if a custom save directory (the file dir) was used,
                // reset the directory stored inside the chooser
                saveFileChooser.setCurrentDirectory(lastSaveDir);
            }

            String extension = saveFileChooser.getExtension();
            OutputFormat outputFormat =  OutputFormat.valueFromExtension(extension);
            outputFormat.saveComposition(comp, selectedFile, true);
            return true;
        }

        return false;
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean saveWithFileChooser(Composition comp) {
        initSaveFileChooser();

        String defaultExtension = FileExtensionUtils.getFileExtension(comp.getName());
        saveFileChooser.setFileFilter(getFileFilterForExtension(defaultExtension));

        return showSaveFileChooserAndSaveComp(comp);
    }

    public static File getLastOpenDir() {
        return lastOpenDir;
    }

    public static File getLastSaveDir() {
        return lastSaveDir;
    }

    public static void setLastOpenDir(File newOpenDir) {
        assert newOpenDir.exists() && newOpenDir.isDirectory();
        FileChoosers.lastOpenDir = newOpenDir;
    }

    public static void setLastSaveDir(File newSaveDir) {
        assert newSaveDir.exists() && newSaveDir.isDirectory();
        FileChoosers.lastSaveDir = newSaveDir;
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
        addDefaultFilters(openFileChooser);
    }

    public static void setDefaultSaveExtensions() {
        addDefaultFilters(saveFileChooser);
    }

    public static void setOnlyOneSaveExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(saveFileChooser, filter);
    }

    public static void setOnlyOneOpenExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(saveFileChooser, filter);
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
            initSaveFileChooser();
            setupFilterToOnlyOneFormat(saveFileChooser, fileFilter);

            GlobalKeyboardWatch.setDialogActive(true);
            int status = saveFileChooser.showSaveDialog(PixelitorWindow.getInstance());
            GlobalKeyboardWatch.setDialogActive(false);

            if (status == JFileChooser.APPROVE_OPTION) {
                selectedFile = saveFileChooser.getSelectedFile();
                lastSaveDir = selectedFile.getParentFile();
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
