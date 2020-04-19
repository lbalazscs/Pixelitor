/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.automate;

import pixelitor.gui.utils.BrowseFilesSupport;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.Dirs;
import pixelitor.io.FileFormat;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.io.File;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static java.awt.BorderLayout.WEST;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;

/**
 * A panel that can be used to select a single directory
 * and optionally an output format
 */
public class SingleDirChooser extends ValidatedPanel {
    private final BrowseFilesSupport dirChooser;
    private FileFormatSelector fileFormatSelector;

    private SingleDirChooser(String label, String initialPath,
                             String fileChooserTitle,
                             FileFormat outputFormat) {
        dirChooser = new BrowseFilesSupport(initialPath, fileChooserTitle, DIRECTORY);
        JTextField dirTF = dirChooser.getNameTF();
        JButton browseButton = dirChooser.getBrowseButton();

        boolean addOutputChooser = outputFormat != null;
        if (addOutputChooser) {
            setLayout(new GridBagLayout());
            var gbh = new GridBagHelper(this);
            gbh.addLabelAndTwoControls(label, dirTF, browseButton);

            fileFormatSelector = new FileFormatSelector(outputFormat);

            gbh.addLabelAndControlNoStretch("Output Format:", fileFormatSelector);
        } else {
            setLayout(new BorderLayout());
            add(new JLabel(label), WEST);
            add(dirTF, CENTER);
            add(browseButton, EAST);
        }
    }

    private FileFormat getSelectedFormat() {
        return fileFormatSelector.getSelectedFormat();
    }

    private File getSelectedDir() {
        return dirChooser.getSelectedFile();
    }

    @Override
    public ValidationResult checkValidity() {
        File selectedDir = getSelectedDir();
        boolean exists = selectedDir.exists();
        boolean isDir = selectedDir.isDirectory();
        if (exists && isDir) {
            return ValidationResult.ok();
        } else {
            if (exists) {
                return ValidationResult.error(
                        "The selected path "
                                + selectedDir.getAbsolutePath()
                                + " is not a folder.");
            } else {
                return ValidationResult.error(
                        "The selected folder "
                                + selectedDir.getAbsolutePath()
                                + " does not exist.");
            }
        }
    }

    public static boolean selectOutputDir() {
        return selectOutputDir(null);
    }

    /**
     * Lets the user select the output directory property of the application.
     * Returns true if a selection was made, false if the operation was cancelled.
     */
    public static boolean selectOutputDir(FileFormat defaultFormat) {
        var chooserPanel = new SingleDirChooser("Output Folder:",
                Dirs.getLastSave().getAbsolutePath(),
                "Select Output Folder", defaultFormat);

        boolean[] selectionWasMade = {false};
        new DialogBuilder()
                .validatedContent(chooserPanel)
                .title("Select Output Folder")
                .okAction(() -> {
                    File dir = chooserPanel.getSelectedDir();
                    Dirs.setLastSaveIfValid(dir);
                    selectionWasMade[0] = true;
                })
                .show();

        if (defaultFormat != null) {
            FileFormat.setLastOutput(chooserPanel.getSelectedFormat());
        }

        return selectionWasMade[0];
    }
}
