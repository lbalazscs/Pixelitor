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

import pixelitor.PixelitorWindow;
import pixelitor.io.FileChooser;
import pixelitor.io.OutputFormat;
import pixelitor.utils.BrowseFilesSupport;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.ValidatedDialog;
import pixelitor.utils.ValidatedForm;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.io.File;

/**
 * A panel that can be used to select a single directory and optionally an output format
 */
public class SingleDirChooserPanel extends ValidatedForm {
    private final BrowseFilesSupport directoryChooser;
    private OutputFormatSelector outputFormatSelector;

    public SingleDirChooserPanel(String label, String dialogTitle, String initialPath, boolean addOutputChooser) {
        directoryChooser = new BrowseFilesSupport(initialPath, dialogTitle, true);
        JLabel dirLabel = new JLabel(label);
        JTextField dirTF = directoryChooser.getNameTF();
        JButton browseButton = directoryChooser.getBrowseButton();

        if (addOutputChooser) {
            setLayout(new GridBagLayout());
            GridBagHelper.addLabel(this, dirLabel, 0, 0);
            GridBagHelper.addControl(this, dirTF);
            GridBagHelper.addNextControl(this, browseButton);

            outputFormatSelector = new OutputFormatSelector();

            GridBagHelper.addLabel(this, outputFormatSelector.getLabelText(), 0, 1);
            GridBagHelper.addControlNoFill(this, outputFormatSelector.getFormatCombo());
        } else {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(dirLabel);
            add(dirTF);
            add(browseButton);
        }
    }

    private OutputFormat getSelectedFormat() {
        OutputFormat outputFormat = outputFormatSelector.getSelectedFormat();
        return outputFormat;
    }

    private File getSelectedDir() {
        return directoryChooser.getSelectedFile();
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public boolean validateData() {
        return true;
    }

    /**
     * Lets the user select the output directory property of the application.
     *
     * @param addOutputChooser
     * @return true if a selection was made, false if the operation was cancelled
     */
    public static boolean selectOutputDir(boolean addOutputChooser) {
        SingleDirChooserPanel chooserPanel = new SingleDirChooserPanel("Output Directory:", "Select Output Directory", FileChooser.getLastSaveDir().getAbsolutePath(), addOutputChooser);
        ValidatedDialog chooser = new ValidatedDialog(chooserPanel, PixelitorWindow.getInstance(), "Select Output Folder");
        chooser.setVisible(true);

        if (addOutputChooser) {
            OutputFormat outputFormat = chooserPanel.getSelectedFormat();
            OutputFormat.setLastOutputFormat(outputFormat);
        }

        if (!chooser.isOkPressed()) {
            return false;
        }
        File selectedDir = chooserPanel.getSelectedDir();
        if (selectedDir != null) {
            FileChooser.setLastSaveDir(selectedDir);
            return true;
        }

        return false;
    }
}
