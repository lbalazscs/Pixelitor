/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.automate;

import pixelitor.io.FileChooser;
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
    private final BrowseFilesSupport inputChooser = new BrowseFilesSupport(FileChooser.getLastOpenDir().getAbsolutePath(), "Select Input Directory", true);
    private final BrowseFilesSupport outputChooser = new BrowseFilesSupport(FileChooser.getLastSaveDir().getAbsolutePath(), "Select Output Directory", true);
    private final boolean allowToBeTheSame;
    private String errMessage;

    private final OutputFormatSelector outputFormatSelector;

    OpenSaveDirsPanel(boolean allowToBeTheSame) {
        this.allowToBeTheSame = allowToBeTheSame;
        setLayout(new GridBagLayout());

        GridBagHelper.addLabel(this, "Input Directory:", 0, 0);
        GridBagHelper.addControl(this, inputChooser.getNameTF());
        GridBagHelper.addNextControl(this, inputChooser.getBrowseButton());

        GridBagHelper.addLabel(this, "Output Directory:", 0, 1);
        GridBagHelper.addControl(this, outputChooser.getNameTF());
        GridBagHelper.addNextControl(this, outputChooser.getBrowseButton());

        GridBagHelper.addLabel(this, "Output Format:", 0, 2);
        outputFormatSelector = new OutputFormatSelector();

        GridBagHelper.addControlNoFill(this, outputFormatSelector.getFormatCombo());
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
    public boolean validateData() {
        File selectedInputDir = inputChooser.getSelectedFile();
        File selectedOutDir = outputChooser.getSelectedFile();

        if (!selectedInputDir.exists()) {
            errMessage = "The selected input directory " + selectedInputDir.getAbsolutePath() + " does not exist.";
            return false;
        }
        if (!selectedOutDir.exists()) {
            errMessage = "The selected output directory " + selectedInputDir.getAbsolutePath() + " does not exist.";
            return false;
        }

        if (allowToBeTheSame) {
            return true;
        }
        if (selectedInputDir.equals(selectedOutDir)) {
            errMessage = "The input and output directories must be different";
            return false;
        }
        return true;
    }

    public void saveValues() {
        File in = inputChooser.getSelectedFile();
        if (in != null) {
            FileChooser.setLastOpenDir(in);
        }
        File out = outputChooser.getSelectedFile();
        if (out != null) {
            FileChooser.setLastSaveDir(out);
        }

        OutputFormat.setLastOutputFormat(getSelectedFormat());
    }
}
