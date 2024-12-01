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
 * A panel that can be used to select a directory and optionally
 * specify an output format.
 */
public class DirectoryChooser extends ValidatedPanel {
    private final BrowseFilesSupport directoryBrowser;
    private JComboBox<FileFormat> formatComboBox;

    private DirectoryChooser(String label, String defaultPath,
                             String chooserDialogTitle,
                             FileFormat defaultOutputFormat) {
        directoryBrowser = new BrowseFilesSupport(defaultPath, chooserDialogTitle, DIRECTORY);
        JTextField dirTF = directoryBrowser.getPathTextField();
        JButton browseButton = directoryBrowser.getBrowseButton();

        boolean includeFormatSelector = defaultOutputFormat != null;
        if (includeFormatSelector) {
            setLayout(new GridBagLayout());
            var gbh = new GridBagHelper(this);
            gbh.addLabelAndTwoControls(label, dirTF, browseButton);

            formatComboBox = new JComboBox<>(FileFormat.values());
            formatComboBox.setSelectedItem(defaultOutputFormat);

            gbh.addLabelAndControlNoStretch("Output Format:", formatComboBox);
        } else {
            setLayout(new BorderLayout());
            add(new JLabel(label), WEST);
            add(dirTF, CENTER);
            add(browseButton, EAST);
        }
    }

    private FileFormat getSelectedFormat() {
        if (formatComboBox == null) {
            throw new IllegalStateException();
        }
        return (FileFormat) formatComboBox.getSelectedItem();
    }

    private File getSelectedDir() {
        return directoryBrowser.getSelectedFile();
    }

    @Override
    public ValidationResult validateSettings() {
        File selectedDir = getSelectedDir();

        if (!selectedDir.exists()) {
            return ValidationResult.invalid(
                "The selected folder <b>"
                    + selectedDir.getAbsolutePath()
                    + "</b> doesn't exist.");
        }

        if (!selectedDir.isDirectory()) {
            return ValidationResult.invalid(
                "The selected path <b>"
                    + selectedDir.getAbsolutePath()
                    + "</b> isn't a folder.");
        }

        return ValidationResult.valid();
    }

    public static boolean selectOutputDir() {
        return selectOutputDir(null);
    }

    /**
     * Lets the user select the output directory property of the application.
     * Returns true if a selection was made, false if the operation was canceled.
     */
    public static boolean selectOutputDir(FileFormat defaultFormat) {
        var chooserPanel = new DirectoryChooser("Output Folder:",
            Dirs.getLastSavePath(),
            "Select Output Folder", defaultFormat);

        boolean[] selectionConfirmed = {false};
        new DialogBuilder()
            .validatedContent(chooserPanel)
            .title("Select Output Folder")
            .okAction(() -> {
                File dir = chooserPanel.getSelectedDir();
                Dirs.setLastSave(dir);
                selectionConfirmed[0] = true;
            })
            .show();

        if (defaultFormat != null) {
            FileFormat.setLastSaved(chooserPanel.getSelectedFormat());
        }

        return selectionConfirmed[0];
    }
}
