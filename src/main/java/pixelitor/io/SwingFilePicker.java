/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static pixelitor.io.FileChooserConfig.FormatSelection.ANY;
import static pixelitor.io.FileChooserConfig.FormatSelection.SINGLE;
import static pixelitor.io.FileChoosers.OPEN_FILTERS;
import static pixelitor.io.FileChoosers.SAVE_FILTERS;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * An implementation of {@link FilePicker} that uses Swing's JFileChooser.
 */
public class SwingFilePicker implements FilePicker {
    private JFileChooser openChooser;
    private SaveFileChooser saveChooser;
    private final PropertyChangeListener lafListener;

    public SwingFilePicker() {
        lafListener = evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                // force the reinitialization of the cached
                // file pickers when the look and feel changes
                openChooser = null;
                saveChooser = null;
            }
        };
        UIManager.addPropertyChangeListener(lafListener);
    }

    @Override
    public File selectSupportedOpenFile() {
        initOpenDialog();
        GlobalEvents.modalDialogOpened();
        int userChoice = openChooser.showOpenDialog(PixelitorWindow.get());
        GlobalEvents.modalDialogClosed();

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openChooser.getSelectedFile();
            RecentDirs.setLastOpen(selectedFile.getParentFile());
            return selectedFile;
        }
        return null;
    }

    @Override
    public File selectSaveFile(FileChooserConfig config) {
        initSaveDialog();

        String suggestedFileName = config.suggestedFileName();
        var selectableFormats = config.formatSelection();
        if (selectableFormats == ANY) {
            saveChooser.setAcceptAllFileFilterUsed(true);
            useSingleSaveFilter(saveChooser.getAcceptAllFileFilter()); // remove all custom file filters
        } else {
            FileFilter fileFilter = config.defaultFileFilter();
            if (selectableFormats == SINGLE) {
                useSingleSaveFilter(fileFilter);
            } else {
                // If the file is saved normally, don't suggest an extension,
                // because the chooser should add it based on the selected file filter.
                suggestedFileName = FileUtils.stripExtension(suggestedFileName);

                saveChooser.setFileFilter(fileFilter);
            }
        }
        saveChooser.setSelectedFile(new File(suggestedFileName));

        assert saveChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY;

        int userChoice;
        File selectedFile = null;
        try {
            GlobalEvents.modalDialogOpened();
            userChoice = saveChooser.showSaveDialog(PixelitorWindow.get());
            GlobalEvents.modalDialogClosed();

            if (userChoice == JFileChooser.APPROVE_OPTION) {
                // has to be retrieved here, because the cleanup in the
                // finally block overwrites the selected file in the picker
                selectedFile = saveChooser.getSelectedFile();
            }
        } catch (Exception e) {
            Messages.showException(e);
            return null;
        } finally {
            if (selectableFormats == ANY) {
                saveChooser.setAcceptAllFileFilterUsed(false);
            }
            if (selectableFormats == ANY || selectableFormats == SINGLE) {
                addDefaultSaveFileFilters();
            }
        }

        //noinspection IfCanBeSwitch
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            RecentDirs.setLastSave(selectedFile.getParentFile());
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

    private void initOpenDialog() {
        assert calledOnEDT() : callInfo();
        if (openChooser != null) {
            return;
        }
        openChooser = new JFileChooser(RecentDirs.getLastOpen()) {
            @Override
            public void approveSelection() {
                File selectedFile = getSelectedFile();
                if (!selectedFile.exists()) {
                    Dialogs.showError(this, "File not found",
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
        addDefaultOpenFileFilters();

        var accessoryPanel = new JPanel(new BorderLayout());
        var progressPanel = new ProgressPanel();
        var previewPanel = new ImagePreviewPanel(progressPanel);
        accessoryPanel.add(previewPanel, CENTER);
        accessoryPanel.add(progressPanel, SOUTH);

        openChooser.setAccessory(accessoryPanel);
        openChooser.addPropertyChangeListener(previewPanel);
    }

    private void initSaveDialog() {
        assert calledOnEDT() : callInfo();

        if (saveChooser == null) {
            saveChooser = new SaveFileChooser(RecentDirs.getLastSave());
            saveChooser.setName("save");
            saveChooser.setDialogTitle(i18n("save_as"));
            addDefaultSaveFileFilters();
        }
    }

    private void addDefaultOpenFileFilters() {
        for (FileFilter filter : OPEN_FILTERS) {
            openChooser.addChoosableFileFilter(filter);
        }
    }

    private void addDefaultSaveFileFilters() {
        for (FileFilter filter : SAVE_FILTERS) {
            saveChooser.addChoosableFileFilter(filter);
        }
    }

    private void useSingleSaveFilter(FileFilter filter) {
        useSingleFileFilter(saveChooser, filter, SAVE_FILTERS);
    }

    private void useSingleOpenFilter(FileFilter filter) {
        useSingleFileFilter(openChooser, filter, OPEN_FILTERS);
    }

    private static void useSingleFileFilter(JFileChooser chooser,
                                            FileFilter chosenFilter,
                                            FileFilter[] allFilters) {
        for (FileFilter filter : allFilters) {
            if (filter != chosenFilter) {
                chooser.removeChoosableFileFilter(filter);
            }
        }

        chooser.setFileFilter(chosenFilter);
    }

    @Override
    public File selectAnyOpenFile() {
        try {
            initOpenDialog();
            openChooser.setAcceptAllFileFilterUsed(true);
            useSingleOpenFilter(openChooser.getAcceptAllFileFilter()); // remove all custom file filters
            return selectSupportedOpenFile();
        } catch (Exception e) {
            Messages.showException(e);
            return null;
        } finally {
            addDefaultOpenFileFilters();
        }
    }

    @Override
    public void dispose() {
        UIManager.removePropertyChangeListener(lafListener);
    }
}
