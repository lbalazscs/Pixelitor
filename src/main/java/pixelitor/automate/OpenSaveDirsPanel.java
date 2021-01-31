/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import java.awt.GridBagLayout;
import java.io.File;

import static java.lang.String.format;
import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.DIRECTORY;

/**
 * A panel for selecting an opening directory,
 * a saving directory, and a saving format
 */
class OpenSaveDirsPanel extends ValidatedPanel {
    private final BrowseFilesSupport inputChooser
        = new BrowseFilesSupport(Dirs.getLastOpenPath(),
        "Select Input Folder", DIRECTORY);
    private final BrowseFilesSupport outputChooser
        = new BrowseFilesSupport(Dirs.getLastSavePath(),
        "Select Output Folder", DIRECTORY);

    private final FileFormatSelector outputFormatSelector;

    OpenSaveDirsPanel() {
        setLayout(new GridBagLayout());
        var gbh = new GridBagHelper(this);

        addDirChooser("Input Folder:", inputChooser, gbh);
        addDirChooser("Output Folder:", outputChooser, gbh);

        outputFormatSelector = new FileFormatSelector(FileFormat.getLastOutput());
        gbh.addLabelAndControlNoStretch("Output Format:", outputFormatSelector);
    }

    private static void addDirChooser(String label,
                                      BrowseFilesSupport chooser,
                                      GridBagHelper gbh) {
        gbh.addLabelAndTwoControls(label,
            chooser.getNameTF(),
            chooser.getBrowseButton());
    }

    private FileFormat getSelectedFormat() {
        return outputFormatSelector.getSelectedFormat();
    }

    /**
     * @return true if the data is valid
     */
    @Override
    public ValidationResult checkValidity() {
        File inputDir = inputChooser.getSelectedFile();
        File outputDir = outputChooser.getSelectedFile();

        var retVal = ValidationResult.ok();
        retVal = addDirExistenceCheck(retVal, inputDir, "input");
        retVal = addDirExistenceCheck(retVal, outputDir, "output");

        if (inputDir.equals(outputDir)) {
            ValidationResult err = ValidationResult.error(
                "The input and output folders must be different.");
            return retVal.and(err);
        }
        return retVal;
    }

    private static ValidationResult addDirExistenceCheck(ValidationResult v,
                                                         File dir, String type) {
        if (!dir.exists()) {
            String msg = format("The selected %s folder %s does not exist.",
                type, dir.getAbsolutePath());
            v = v.and(ValidationResult.error(msg));
        }
        return v;
    }

    public void rememberValues() {
        File in = inputChooser.getSelectedFile();
        Dirs.setLastOpen(in);

        File out = outputChooser.getSelectedFile();
        Dirs.setLastSave(out);

        FileFormat.setLastOutput(getSelectedFormat());
    }
}
