/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import java.io.File;
import java.nio.file.Files;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * Utility class with static methods related to file choosers
 */
public class FileChoosers {
    private static JFileChooser openChooser;
    private static SaveFileChooser saveChooser;

    public static final FileNameExtensionFilter bmpFilter = new FileNameExtensionFilter(
        "BMP files", "bmp");
    public static final FileNameExtensionFilter gifFilter = new FileNameExtensionFilter(
        "GIF files", "gif");
    public static final FileNameExtensionFilter jpegFilter = new FileNameExtensionFilter(
        "JPEG files", "jpg", "jpeg");
    public static final FileNameExtensionFilter oraFilter = new FileNameExtensionFilter(
        "OpenRaster files", "ora");
    public static final FileNameExtensionFilter pamFilter = new FileNameExtensionFilter(
        "PAM files", "pam");
    public static final FileNameExtensionFilter pngFilter = new FileNameExtensionFilter(
        "PNG files", "png");
    private static final FileNameExtensionFilter netPBMFilters = new FileNameExtensionFilter(
        "NetPBM files", "pam", "pbm", "pgm", "ppm", "pfm");
    public static final FileNameExtensionFilter ppmFilter = new FileNameExtensionFilter(
        "PPM files", "ppm");
    public static final FileNameExtensionFilter pxcFilter = new FileNameExtensionFilter(
        "PXC files", "pxc");
    public static final FileNameExtensionFilter tgaFilter = new FileNameExtensionFilter(
        "TGA files", "tga");
    public static final FileNameExtensionFilter tiffFilter = new FileNameExtensionFilter(
        "TIFF files", "tiff", "tif");
    public static final FileNameExtensionFilter svgFilter = new FileNameExtensionFilter(
        "SVG files", "svg");

    // the difference is that all NetPBM files can be opened,
    // but only PAM and PPM can be saved
    public static final FileNameExtensionFilter[] OPEN_FILTERS = {
        bmpFilter, gifFilter, jpegFilter, netPBMFilters, oraFilter,
        pngFilter, pxcFilter, tiffFilter, tgaFilter};
    public static final FileNameExtensionFilter[] SAVE_FILTERS = {
        bmpFilter, gifFilter, jpegFilter, oraFilter, pamFilter,
        pngFilter, ppmFilter, pxcFilter, tiffFilter, tgaFilter};

    private FileChoosers() {
    }

    private static void initOpenChooser() {
        assert calledOnEDT() : threadInfo();

        if (openChooser == null) {
            createOpenChooser();
        }
    }

    private static void createOpenChooser() {
        openChooser = new JFileChooser(Dirs.getLastOpen()) {
            @Override
            public void approveSelection() {
                File f = getSelectedFile();
                if (!f.exists()) {
                    Dialogs.showErrorDialog(this, "File not found",
                        "<html>The file <b>" + f.getAbsolutePath()
                            + " </b> doesn't exist. " +
                            "<br>Check the file name and try again."
                    );
                    return;
                }
                if (!Files.isReadable(f.toPath())) {
                    Dialogs.showFileNotReadableError(this, f);
                    return;
                }
                super.approveSelection();
            }
        };
        openChooser.setName("open");

        setDefaultOpenExtensions();

        var p = new JPanel();
        p.setLayout(new BorderLayout());
        var progressPanel = new ProgressPanel();
        var preview = new ImagePreviewPanel(progressPanel);
        p.add(preview, CENTER);
        p.add(progressPanel, SOUTH);

        openChooser.setAccessory(p);
        openChooser.addPropertyChangeListener(preview);
    }

    public static void initSaveChooser() {
        assert calledOnEDT() : threadInfo();

        File lastSaveDir = Dirs.getLastSave();
        if (saveChooser == null) {
            createSaveChooser(lastSaveDir);
        } else {
            saveChooser.setCurrentDirectory(lastSaveDir);
        }
    }

    private static void createSaveChooser(File lastSaveDir) {
        saveChooser = new SaveFileChooser(lastSaveDir);
        saveChooser.setName("save");
        saveChooser.setDialogTitle(i18n("save_as"));

        setDefaultSaveExtensions();
    }

    public static void openAsync() {
        initOpenChooser();

        File selectedFile = askUserForOpenFile();
        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            if (FileUtils.hasSupportedInputExt(fileName)) {
                IO.openFileAsync(selectedFile, true);
            } else { // unsupported extension
                handleUnsupportedExtensionWhileOpening(fileName);
            }
        }
    }

    private static File askUserForOpenFile() {
        GlobalEvents.dialogOpened("Open");
        int result = openChooser.showOpenDialog(PixelitorWindow.get());
        GlobalEvents.dialogClosed("Open");

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openChooser.getSelectedFile();
            Dirs.setLastOpen(selectedFile.getParentFile());
            return selectedFile;
        } else if (result == JFileChooser.CANCEL_OPTION) {
            // cancelled
            return null;
        } else if (result == JFileChooser.ERROR_OPTION) {
            // error or dismissed
            return null;
        }
        return null;
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
                                                     Object extraInfo) {
        File selectedFile = askUserForSaveFile(comp);
        if (selectedFile != null) {
            String extension = saveChooser.getExtension();
            IO.saveToChosenFile(comp, selectedFile, extraInfo, extension);
            return true;
        }

        return false;
    }

    public static File getAnySaveFile(Composition comp) {
        try {
            initSaveChooser();
            saveChooser.setAcceptAllFileFilterUsed(true);
            setOnlyOneSaveExtension(saveChooser.getAcceptAllFileFilter()); // remove all custom file filters
            return askUserForSaveFile(comp);
        } finally {
            setDefaultSaveExtensions();
        }
    }

    public static File getAnyOpenFile() {
        try {
            initOpenChooser();
            openChooser.setAcceptAllFileFilterUsed(true);
            setOnlyOneOpenExtension(openChooser.getAcceptAllFileFilter()); // remove all custom file filters

            return askUserForOpenFile();
        } finally {
            setDefaultOpenExtensions();
        }
    }

    private static File askUserForSaveFile(Composition comp) {
        String defaultFileName = FileUtils.stripExtension(comp.getName());
        saveChooser.setSelectedFile(new File(defaultFileName));

        File file = comp.getFile();
        if (file != null && Dirs.getLastSave() == null) {
            File customSaveDir = file.getParentFile();
            saveChooser.setCurrentDirectory(customSaveDir);
        }
        assert saveChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY;

        GlobalEvents.dialogOpened("Save");
        int result = saveChooser.showSaveDialog(PixelitorWindow.get());
        GlobalEvents.dialogClosed("Save");

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveChooser.getSelectedFile();
            Dirs.setLastSave(selectedFile.getParentFile());
            return selectedFile;
        } else if (result == JFileChooser.CANCEL_OPTION) {
            // cancelled
            return null;
        } else if (result == JFileChooser.ERROR_OPTION) {
            // error or dismissed
            return null;
        }
        return null;
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean saveWithChooser(Composition comp) {
        initSaveChooser();

        String defaultExt = FileUtils
            .findExtension(comp.getName())
            .orElse(FileFormat.getLastOutput().toString());
        saveChooser.setFileFilter(getFileFilterForExtension(defaultExt));

        return showSaveChooserAndSaveComp(comp, null);
    }

    private static FileFilter getFileFilterForExtension(String ext) {
        return FileFormat.fromExtension(ext)
            .orElse(FileFormat.JPG)
            .getFileFilter();
    }

    private static void setDefaultOpenExtensions() {
        for (FileFilter filter : OPEN_FILTERS) {
            openChooser.addChoosableFileFilter(filter);
        }
    }

    public static void setDefaultSaveExtensions() {
        for (FileFilter filter : SAVE_FILTERS) {
            saveChooser.addChoosableFileFilter(filter);
        }
    }

    public static void setOnlyOneSaveExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(saveChooser, filter);
    }

    public static void setOnlyOneOpenExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(openChooser, filter);
    }

    private static void setupFilterToOnlyOneFormat(JFileChooser chooser,
                                                   FileFilter chosenFilter) {
        FileFilter[] allFilters;
        if (chooser == openChooser) {
            allFilters = OPEN_FILTERS;
        } else if (chooser == saveChooser) {
            allFilters = SAVE_FILTERS;
        } else {
            throw new IllegalArgumentException("chooser = " + chooser.getClass().getName());
        }
        for (FileFilter filter : allFilters) {
            if (filter != chosenFilter) {
                chooser.removeChoosableFileFilter(filter);
            }
        }

        chooser.setFileFilter(chosenFilter);
    }

    public static File selectSaveFileForSpecificFormat(FileFilter fileFilter) {
        try {
            initSaveChooser();
            setupFilterToOnlyOneFormat(saveChooser, fileFilter);

            GlobalEvents.dialogOpened("Save");
            int status = saveChooser.showSaveDialog(PixelitorWindow.get());
            GlobalEvents.dialogClosed("Save");

            File selectedFile = null;
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
