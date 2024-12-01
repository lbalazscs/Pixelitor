/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import static pixelitor.io.FileChooserConfig.SelectableFormats.ANY;
import static pixelitor.io.FileChooserConfig.SelectableFormats.SINGLE;
import static pixelitor.io.FileChoosers.OPEN_FILTERS;
import static pixelitor.io.FileChoosers.SAVE_FILTERS;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

public class SwingFilePicker implements FilePicker {
    private JFileChooser openChooser;
    private SaveFileChooser saveChooser;

    public SwingFilePicker() {
        UIManager.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("lookAndFeel")) {
                // force the reinitialization of the cached
                // file pickers when the look and feel changes
                openChooser = null;
                saveChooser = null;
            }
        });
    }

    @Override
    public File getSupportedOpenFile() {
        initOpenChooser();
        GlobalEvents.dialogOpened("Open");
        int userChoice = openChooser.showOpenDialog(PixelitorWindow.get());
        GlobalEvents.dialogClosed("Open");

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openChooser.getSelectedFile();
            Dirs.setLastOpen(selectedFile.getParentFile());
            return selectedFile;
        }
        return null;
    }

    @Override
    public File showSaveDialog(FileChooserConfig config) {
        initSaveChooser();

        String suggestedFileName = config.suggestedFileName();
        var selectableFormats = config.formats();
        if (selectableFormats == ANY) {
            saveChooser.setAcceptAllFileFilterUsed(true);
            setSingleSaveExtension(saveChooser.getAcceptAllFileFilter()); // remove all custom file filters
        } else {
            FileFilter fileFilter = config.defaultFileFilter();
            if (selectableFormats == SINGLE) {
                setSingleSaveExtension(fileFilter);
            } else {
                // If the file is saved normally, don't suggest an extension,
                // because the chooser should add it based on the selected file filter.
                suggestedFileName = FileUtils.removeExtension(suggestedFileName);

                saveChooser.setFileFilter(fileFilter);
            }
        }
        saveChooser.setSelectedFile(new File(suggestedFileName));

        assert saveChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY;

        int userChoice;
        try {
            GlobalEvents.dialogOpened("Save");
            userChoice = saveChooser.showSaveDialog(PixelitorWindow.get());
            GlobalEvents.dialogClosed("Save");
        } catch (Exception e) {
            Messages.showException(e);
            return null;
        } finally {
            if (selectableFormats == ANY || selectableFormats == SINGLE) {
                setDefaultSaveExtensions();
            }
        }

        //noinspection IfCanBeSwitch
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveChooser.getSelectedFile();
            Dirs.setLastSave(selectedFile.getParentFile());
            return selectedFile;
        } else if (userChoice == JFileChooser.CANCEL_OPTION) {
            // canceled
            return null;
        } else if (userChoice == JFileChooser.ERROR_OPTION) {
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
                File selectedFile = getSelectedFile();
                if (!selectedFile.exists()) {
                    Dialogs.showErrorDialog(this, "File not found",
                        "<html>The file <b>" + selectedFile.getAbsolutePath()
                            + " </b> doesn't exist. " +
                            "<br>Check the file name and try again."
                    );
                    return;
                }
                if (!Files.isReadable(selectedFile.toPath())) {
                    Dialogs.showFileNotReadableError(this, selectedFile);
                    return;
                }
                super.approveSelection();
            }
        };
        openChooser.setName("open");
        setDefaultOpenExtensions();

        var accessoryPanel = new JPanel(new BorderLayout());
        var progressPanel = new ProgressPanel();
        var previewPanel = new ImagePreviewPanel(progressPanel);
        accessoryPanel.add(previewPanel, CENTER);
        accessoryPanel.add(progressPanel, SOUTH);

        openChooser.setAccessory(accessoryPanel);
        openChooser.addPropertyChangeListener(previewPanel);
    }

    private void initSaveChooser() {
        assert calledOnEDT() : threadInfo();

        if (saveChooser == null) {
            saveChooser = new SaveFileChooser(Dirs.getLastSave());
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

    private void setSingleSaveExtension(FileFilter filter) {
        configureSingleFileFilter(saveChooser, filter);
    }

    private void setSingleOpenExtension(FileFilter filter) {
        configureSingleFileFilter(openChooser, filter);
    }

    private void configureSingleFileFilter(JFileChooser chooser,
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
            setSingleOpenExtension(openChooser.getAcceptAllFileFilter()); // remove all custom file filters
            return getSupportedOpenFile();
        } catch (Exception e) {
            Messages.showException(e);
            return null;
        } finally {
            setDefaultOpenExtensions();
        }
    }
}
