/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.Directories;
import pixelitor.io.OutputFormat;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.io.File;

import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;

/**
 * A panel that can be used to select a single directory
 * and optionally an output format
 */
public class SingleDirChooserPanel extends ValidatedPanel {
    private final BrowseFilesSupport dirChooser;
    private OutputFormatSelector outputFormatSelector;

    private SingleDirChooserPanel(String label, String initialPath,
                                  String fileChooserTitle, boolean addOutputChooser) {
        dirChooser = new BrowseFilesSupport(initialPath, fileChooserTitle, DIRECTORY);
        JTextField dirTF = dirChooser.getNameTF();
        JButton browseButton = dirChooser.getBrowseButton();

        if (addOutputChooser) {
            setLayout(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(this);
            gbh.addLabelWithTwoControls(label, dirTF, browseButton);

            outputFormatSelector = new OutputFormatSelector();

            gbh.addLabelWithControlNoStretch("Output Format:", outputFormatSelector);
        } else {
            setLayout(new BorderLayout());
            add(new JLabel(label), BorderLayout.WEST);
            add(dirTF, BorderLayout.CENTER);
            add(browseButton, BorderLayout.EAST);
        }
    }

    private OutputFormat getSelectedFormat() {
        return outputFormatSelector.getSelectedFormat();
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

    /**
     * Lets the user select the output directory property of the application.
     * Returns true if a selection was made, false if the operation was cancelled.
     */
    public static boolean selectOutputDir(boolean addOutputChooser) {
        SingleDirChooserPanel chooserPanel = new SingleDirChooserPanel("Output Folder:",
                Directories.getLastSaveDir().getAbsolutePath(),
                "Select Output Folder", addOutputChooser);

        boolean[] selectionWasMade = {false};
        new DialogBuilder()
                .validatedContent(chooserPanel)
                .title("Select Output Folder")
                .okAction(() -> {
                    File dir = chooserPanel.getSelectedDir();
                    Directories.setLastSaveDir(dir);
                    selectionWasMade[0] = true;
                })
                .show();

        if (addOutputChooser) {
            OutputFormat outputFormat = chooserPanel.getSelectedFormat();
            OutputFormat.setLastUsed(outputFormat);
        }

        return selectionWasMade[0];
    }
}
