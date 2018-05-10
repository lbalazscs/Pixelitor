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
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.ValidatedForm;
import pixelitor.gui.utils.ValidationResult;
import pixelitor.io.Directories;
import pixelitor.io.OutputFormat;

import java.awt.GridBagLayout;
import java.io.File;

import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;

/**
 * A panel for selecting an opening directory,
 * a saving directory, and a saving format
 */
class OpenSaveDirsPanel extends ValidatedForm {
    private final BrowseFilesSupport inputChooser = new BrowseFilesSupport(Directories.getLastOpenDirPath(), "Select Input Folder", DIRECTORY);
    private final BrowseFilesSupport outputChooser = new BrowseFilesSupport(Directories.getLastSaveDirPath(), "Select Output Folder", DIRECTORY);
    private final boolean allowToBeTheSame;

    private final OutputFormatSelector outputFormatSelector;

    OpenSaveDirsPanel(boolean allowToBeTheSame) {
        this.allowToBeTheSame = allowToBeTheSame;
        setLayout(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper(this);

        gbh.addLabelWithTwoControls("Input Folder:",
                inputChooser.getNameTF(), inputChooser.getBrowseButton());

        gbh.addLabelWithTwoControls("Output Folder:",
                outputChooser.getNameTF(), outputChooser.getBrowseButton());

        outputFormatSelector = new OutputFormatSelector();
        gbh.addLabelWithControlNoFill("Output Format:", outputFormatSelector);
    }

    private OutputFormat getSelectedFormat() {
        return outputFormatSelector.getSelectedFormat();
    }

    /**
     * @return true if the data is valid
     */
    @Override
    public ValidationResult checkValidity() {
        File selectedInputDir = inputChooser.getSelectedFile();
        File selectedOutDir = outputChooser.getSelectedFile();

        ValidationResult v = ValidationResult.ok()
                .andTrue(selectedInputDir.exists(),
                        "The selected input folder " + selectedInputDir.getAbsolutePath() + " does not exist.")
                .andTrue(selectedOutDir.exists(),
                        "The selected output folder " + selectedInputDir.getAbsolutePath() + " does not exist.");

        if (!allowToBeTheSame && selectedInputDir.equals(selectedOutDir)) {
            ValidationResult err = ValidationResult.error("The input and output folders must be different.");
            return v.and(err);
        }
        return v;
    }

    public void saveValues() {
        File in = inputChooser.getSelectedFile();
        if (in != null) {
            Directories.setLastOpenDir(in);
        }
        File out = outputChooser.getSelectedFile();
        if (out != null) {
            Directories.setLastSaveDir(out);
        }

        OutputFormat.setLastUsed(getSelectedFormat());
    }
}
