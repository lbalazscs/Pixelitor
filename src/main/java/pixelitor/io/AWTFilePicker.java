/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FileDialog;
import java.io.File;

import static pixelitor.io.FileChooserConfig.SelectableFormats.SINGLE;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * An implementation of {@link FilePicker} that uses AWT's FileDialog to show native file choosers.
 */
public class AWTFilePicker implements FilePicker {
    private FileDialog openDialog;
    private FileDialog saveDialog;

    @Override
    public File selectSupportedOpenFile() {
        initOpenPicker();
        GlobalEvents.modalDialogOpened();
        openDialog.setVisible(true);
        GlobalEvents.modalDialogClosed();
        String file = openDialog.getFile();
        if (file != null) {
            String directory = openDialog.getDirectory();
            return new File(directory, file);
        }
        return null;
    }

    @Override
    public File showSaveDialog(FileChooserConfig config) {
        initSavePicker();

        if (config.formats() == SINGLE) {
            saveDialog.setFilenameFilter((dir, name) ->
                config.defaultFileFilter().accept(new File(dir, name)));
        }

        String suggestedFileName = config.suggestedFileName();
        if (suggestedFileName != null) {
            saveDialog.setFile(suggestedFileName);
        }

        GlobalEvents.modalDialogOpened();
        saveDialog.setVisible(true);
        GlobalEvents.modalDialogClosed();

        String selectedFileName = saveDialog.getFile();
        if (selectedFileName == null) {
            return null;
        }

        if (!FileUtils.hasExtension(selectedFileName)) {
            if (config.defaultFileFilter() instanceof FileNameExtensionFilter filter) {
                String extension = filter.getExtensions()[0];
                selectedFileName += "." + extension;
            } else {
                // this shouldn't happen with the current configuration options
                Dialogs.showNoExtensionDialog(null);
                return null;
            }
        }

        return new File(saveDialog.getDirectory(), selectedFileName);
    }

    private void initOpenPicker() {
        assert calledOnEDT() : callInfo();
        if (openDialog == null) {
            openDialog = new FileDialog(PixelitorWindow.get(), "Open File", FileDialog.LOAD);
        }
    }

    private void initSavePicker() {
        assert calledOnEDT() : callInfo();
        if (saveDialog == null) {
            saveDialog = new FileDialog(PixelitorWindow.get(), "Save File", FileDialog.SAVE);
            File lastSaveDir = Dirs.getLastSave();
            saveDialog.setDirectory(lastSaveDir.getAbsolutePath());
        }
    }

    @Override
    public File selectAnyOpenFile() {
        // the AWT chooser isn't configured with an extension
        // picker, so these two methods are identical in this class
        return selectSupportedOpenFile();
    }
}
