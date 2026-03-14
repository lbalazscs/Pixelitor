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

package pixelitor.automate;

import pixelitor.gui.utils.BrowseFilesSupport;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.Validated;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.FileFormat;
import pixelitor.io.RecentDirs;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.io.File;

import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;
import static pixelitor.gui.utils.TextFieldValidator.createExistingDirLayer;
import static pixelitor.gui.utils.TextFieldValidator.requireExistingDir;

/**
 * A panel for selecting input/output folders and an output file format.
 */
class OpenSaveDirsPanel extends JPanel implements Validated {
    private static final String INPUT_FOLDER_LABEL = "Input Folder";
    private final BrowseFilesSupport inputDirSelector
        = new BrowseFilesSupport(RecentDirs.getLastOpenPath(),
        INPUT_FOLDER_LABEL, DIRECTORY);

    private static final String OUTPUT_FOLDER_LABEL = "Output Folder";
    private final BrowseFilesSupport outputDirSelector
        = new BrowseFilesSupport(RecentDirs.getLastSavePath(),
        OUTPUT_FOLDER_LABEL, DIRECTORY);

    private final JComboBox<FileFormat> outputFormatSelector;

    OpenSaveDirsPanel() {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        addDirSelectorRow(INPUT_FOLDER_LABEL + ":", INPUT_FOLDER_LABEL, inputDirSelector, gbh);
        addDirSelectorRow(OUTPUT_FOLDER_LABEL + ":", OUTPUT_FOLDER_LABEL, outputDirSelector, gbh);

        outputFormatSelector = new JComboBox<>(FileFormat.values());
        outputFormatSelector.setSelectedItem(FileFormat.getLastSaved());
        gbh.addLabelAndControlNoStretch("Output Format:", outputFormatSelector);
    }

    private static void addDirSelectorRow(String label,
                                          String validationLabel,
                                          BrowseFilesSupport chooser,
                                          GridBagHelper gbh) {
        gbh.addLabelAndTwoControls(label,
            createExistingDirLayer(validationLabel, chooser.getTextField()),
            chooser.getBrowseButton());
    }

    private FileFormat getSelectedFormat() {
        return (FileFormat) outputFormatSelector.getSelectedItem();
    }

    /**
     * Validates that both directories exist and are different.
     */
    @Override
    public ValidationResult validateSettings() {
        ValidationResult result = requireExistingDir(inputDirSelector.getTextField(), INPUT_FOLDER_LABEL)
            .and(requireExistingDir(outputDirSelector.getTextField(), OUTPUT_FOLDER_LABEL));

        if (result.isValid()) {
            File inputDir = inputDirSelector.getSelectedFile();
            File outputDir = outputDirSelector.getSelectedFile();

            result = result.addErrorIf(inputDir.equals(outputDir),
                "The input and output folders must be different.");
        }

        return result;
    }

    /**
     * Saves the chosen directories and format for future use.
     */
    public void rememberSettings() {
        RecentDirs.setLastOpen(inputDirSelector.getSelectedFile());
        RecentDirs.setLastSave(outputDirSelector.getSelectedFile());
        FileFormat.setLastSaved(getSelectedFormat());
    }
}
