/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.gui.utils.SaveFileChooser;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressPanel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Files;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static pixelitor.io.FileChoosers.OPEN_FILTERS;
import static pixelitor.io.FileChoosers.SAVE_FILTERS;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

public class SwingFilePicker implements FilePicker {
    private JFileChooser openChooser;
    private SaveFileChooser saveChooser;

    @Override
    public File getSupportedOpenFile() {
        initOpenChooser();
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

    @Override
    public File showSaveDialog(FileChooserInfo chooserInfo) {
        initSaveChooser();

        String suggestedFileName = chooserInfo.suggestedFileName();
        if (chooserInfo.anyFormat()) {
            saveChooser.setAcceptAllFileFilterUsed(true);
            setOnlyOneSaveExtension(saveChooser.getAcceptAllFileFilter()); // remove all custom file filters
        } else {
            FileFilter fileFilter = chooserInfo.defaultFileFilter();
            if (chooserInfo.singleFormat()) {
                setOnlyOneSaveExtension(fileFilter);
            } else {
                // If the file is saved normally, don't suggest an extension,
                // because the chooser should add it based on the selected file filter.
                suggestedFileName = FileUtils.stripExtension(suggestedFileName);

                saveChooser.setFileFilter(fileFilter);
            }
        }
        saveChooser.setSelectedFile(new File(suggestedFileName));

        assert saveChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY;

        int result;
        try {
            GlobalEvents.dialogOpened("Save");
            result = saveChooser.showSaveDialog(PixelitorWindow.get());
            GlobalEvents.dialogClosed("Save");
        } catch (Exception e) {
            Messages.showException(e);
            return null;
        } finally {
            if (chooserInfo.anyFormat() || chooserInfo.singleFormat()) {
                setDefaultSaveExtensions();
            }
        }

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

    private void initOpenChooser() {
        assert calledOnEDT() : threadInfo();
        if (openChooser != null) {
            return;
        }
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

    private void initSaveChooser() {
        assert calledOnEDT() : threadInfo();

        if (saveChooser == null) {
            File lastSaveDir = Dirs.getLastSave();
            saveChooser = new SaveFileChooser(lastSaveDir);
//            saveChooser.setCurrentDirectory(lastSaveDir);
            saveChooser.setName("save");
            saveChooser.setDialogTitle(i18n("save_as"));
            setDefaultSaveExtensions();
        }
    }

    private void setDefaultOpenExtensions() {
        for (FileFilter filter : OPEN_FILTERS) {
            openChooser.addChoosableFileFilter(filter);
        }
    }

    private void setDefaultSaveExtensions() {
        for (FileFilter filter : SAVE_FILTERS) {
            saveChooser.addChoosableFileFilter(filter);
        }
    }

    private void setOnlyOneSaveExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(saveChooser, filter);
    }

    private void setOnlyOneOpenExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(openChooser, filter);
    }

    private void setupFilterToOnlyOneFormat(JFileChooser chooser,
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

    @Override
    public String getSelectedSaveExtension(File selectedFile) {
        return saveChooser.getExtension();
    }

    @Override
    public File getAnyOpenFile() {
        try {
            initOpenChooser();
            openChooser.setAcceptAllFileFilterUsed(true);
            setOnlyOneOpenExtension(openChooser.getAcceptAllFileFilter()); // remove all custom file filters
            return getSupportedOpenFile();
        } catch (Exception e) {
            Messages.showException(e);
            return null;
        } finally {
            setDefaultOpenExtensions();
        }
    }
}
