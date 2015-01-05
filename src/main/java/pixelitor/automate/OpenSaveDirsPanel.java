/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.io.FileChoosers;
import pixelitor.io.OutputFormat;
import pixelitor.utils.BrowseFilesSupport;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.ValidatedForm;

import java.awt.GridBagLayout;
import java.io.File;

/**
 * A panel for selecting the opening and the saving directory
 */
class OpenSaveDirsPanel extends ValidatedForm {
    private final BrowseFilesSupport inputChooser = new BrowseFilesSupport(FileChoosers.getLastOpenDir().getAbsolutePath(), "Select Input Folder", true);
    private final BrowseFilesSupport outputChooser = new BrowseFilesSupport(FileChoosers.getLastSaveDir().getAbsolutePath(), "Select Output Folder", true);
    private final boolean allowToBeTheSame;
    private String errMessage;

    private final OutputFormatSelector outputFormatSelector;

    OpenSaveDirsPanel(boolean allowToBeTheSame) {
        this.allowToBeTheSame = allowToBeTheSame;
        setLayout(new GridBagLayout());
        GridBagHelper gridBagHelper = new GridBagHelper(this);

        gridBagHelper.addLabel("Input Folder:", 0, 0);
        gridBagHelper.addControl(inputChooser.getNameTF());
        gridBagHelper.addNextControl(inputChooser.getBrowseButton());

        gridBagHelper.addLabel("Output Folder:", 0, 1);
        gridBagHelper.addControl(outputChooser.getNameTF());
        gridBagHelper.addNextControl(outputChooser.getBrowseButton());

        gridBagHelper.addLabel("Output Format:", 0, 2);
        outputFormatSelector = new OutputFormatSelector();

        gridBagHelper.addControlNoFill(outputFormatSelector.getFormatCombo());
    }

    private OutputFormat getSelectedFormat() {
        return outputFormatSelector.getSelectedFormat();
    }

    @Override
    public String getErrorMessage() {
        return errMessage;
    }

    /**
     * @return true if the data is valid
     */
    @Override
    public boolean isDataValid() {
        File selectedInputDir = inputChooser.getSelectedFile();
        File selectedOutDir = outputChooser.getSelectedFile();

        if (!selectedInputDir.exists()) {
            errMessage = "The selected input folder " + selectedInputDir.getAbsolutePath() + " does not exist.";
            return false;
        }
        if (!selectedOutDir.exists()) {
            errMessage = "The selected output folder " + selectedInputDir.getAbsolutePath() + " does not exist.";
            return false;
        }

        if (allowToBeTheSame) {
            return true;
        }
        if (selectedInputDir.equals(selectedOutDir)) {
            errMessage = "The input and output folders must be different";
            return false;
        }
        return true;
    }

    public void saveValues() {
        File in = inputChooser.getSelectedFile();
        if (in != null) {
            FileChoosers.setLastOpenDir(in);
        }
        File out = outputChooser.getSelectedFile();
        if (out != null) {
            FileChoosers.setLastSaveDir(out);
        }

        OutputFormat.setLastOutputFormat(getSelectedFormat());
    }
}
