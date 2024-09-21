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

import pixelitor.gui.utils.BrowseFilesSupport;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.ValidatedPanel;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.Dirs;
import pixelitor.io.FileFormat;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.io.File;

import static java.lang.String.format;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;

/**
 * A panel for selecting an input folder,
 * an output folder, and a saving file format.
 */
class OpenSaveDirsPanel extends ValidatedPanel {
    private final BrowseFilesSupport inputChooser
        = new BrowseFilesSupport(Dirs.getLastOpenPath(),
        "Select Input Folder", DIRECTORY);
    private final BrowseFilesSupport outputChooser
        = new BrowseFilesSupport(Dirs.getLastSavePath(),
        "Select Output Folder", DIRECTORY);

    private final JComboBox<FileFormat> outputFormatSelector;

    OpenSaveDirsPanel() {
        super(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        addDirChooserRow("Input Folder:", inputChooser, gbh);
        addDirChooserRow("Output Folder:", outputChooser, gbh);

        outputFormatSelector = new JComboBox<>(FileFormat.values());
        outputFormatSelector.setSelectedItem(FileFormat.getLastSaved());
        gbh.addLabelAndControlNoStretch("Output Format:", outputFormatSelector);
    }

    private static void addDirChooserRow(String label,
                                         BrowseFilesSupport chooser,
                                         GridBagHelper gbh) {
        gbh.addLabelAndTwoControls(label,
            chooser.getPathTextField(),
            chooser.getBrowseButton());
    }

    private FileFormat getSelectedFormat() {
        return (FileFormat) outputFormatSelector.getSelectedItem();
    }

    @Override
    public ValidationResult validateSettings() {
        File inputDir = inputChooser.getSelectedFile();
        File outputDir = outputChooser.getSelectedFile();

        var result = ValidationResult.valid();
        result = validateDirExists(result, inputDir, "input");
        result = validateDirExists(result, outputDir, "output");

        if (inputDir.equals(outputDir)) {
            ValidationResult sameDirError = ValidationResult.invalid(
                "The input and output folders must be different.");
            return result.and(sameDirError);
        }
        return result;
    }

    private static ValidationResult validateDirExists(ValidationResult currentResult,
                                                      File dir,
                                                      String directoryType) {
        if (!dir.exists()) {
            String msg = format("The selected %s folder %s doesn't exist.",
                directoryType, dir.getAbsolutePath());
            currentResult = currentResult.and(ValidationResult.invalid(msg));
        }
        return currentResult;
    }

    public void rememberValues() {
        Dirs.setLastOpen(inputChooser.getSelectedFile());
        Dirs.setLastSave(outputChooser.getSelectedFile());
        FileFormat.setLastSaved(getSelectedFormat());
    }
}
