/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.*;
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
 * and optionally an output format.
 */
public class DirectoryChooser extends ValidatedPanel {
    private final BrowseFilesSupport chooserSupport;
    private JComboBox<FileFormat> outputFormatSelector;

    private DirectoryChooser(String label, String initialPath,
                             String chooserDialogTitle,
                             FileFormat defaultOutputFormat) {
        chooserSupport = new BrowseFilesSupport(initialPath, chooserDialogTitle, DIRECTORY);
        JTextField dirTF = chooserSupport.getNameTF();
        JButton browseButton = chooserSupport.getBrowseButton();

        boolean addOutputChooser = defaultOutputFormat != null;
        if (addOutputChooser) {
            setLayout(new GridBagLayout());
            var gbh = new GridBagHelper(this);
            gbh.addLabelAndTwoControls(label, dirTF, browseButton);

            outputFormatSelector = new JComboBox<>(FileFormat.values());
            outputFormatSelector.setSelectedItem(defaultOutputFormat);

            gbh.addLabelAndControlNoStretch("Output Format:", outputFormatSelector);
        } else {
            setLayout(new BorderLayout());
            add(new JLabel(label), WEST);
            add(dirTF, CENTER);
            add(browseButton, EAST);
        }
    }

    private FileFormat getSelectedFormat() {
        return (FileFormat) outputFormatSelector.getSelectedItem();
    }

    private File getSelectedDir() {
        return chooserSupport.getSelectedFile();
    }

    @Override
    public ValidationResult validateSettings() {
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
                        + " isn't a folder.");
            } else {
                return ValidationResult.error(
                    "The selected folder "
                        + selectedDir.getAbsolutePath()
                        + " doesn't exist.");
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
        var chooserPanel = new DirectoryChooser("Output Folder:",
            Dirs.getLastSave().getAbsolutePath(),
            "Select Output Folder", defaultFormat);

        boolean[] selectionWasMade = {false};
        new DialogBuilder()
            .validatedContent(chooserPanel)
            .title("Select Output Folder")
            .okAction(() -> {
                File dir = chooserPanel.getSelectedDir();
                Dirs.setLastSave(dir);
                selectionWasMade[0] = true;
            })
            .show();

        if (defaultFormat != null) {
            FileFormat.setLastSaved(chooserPanel.getSelectedFormat());
        }

        return selectionWasMade[0];
    }
}
