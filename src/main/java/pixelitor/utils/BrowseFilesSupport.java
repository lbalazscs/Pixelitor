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
package pixelitor.utils;

import pixelitor.PixelitorWindow;
import pixelitor.io.FileExtensionUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * The GUI elements of a file/directory chooser (a textfield and a
 * "Browse..." button) are separated into this non-component class
 * so that they can be reused with different layout managers
 */
public class BrowseFilesSupport {
    private JTextField nameTF;
    private final JButton button = new JButton("Browse...");
    private String dialogTitle;
    private boolean selectDirs;
    private FileNameExtensionFilter fileFilter; // used for filtering when in file selection mode

    public BrowseFilesSupport(String initialPath) {
        init(initialPath);
    }

    public BrowseFilesSupport(String initialPath, final String dialogTitle, final boolean selectDirs) {
        this.dialogTitle = dialogTitle;
        this.selectDirs = selectDirs;
        init(initialPath);
    }

    private void init(String initialPath) {
        nameTF = new JTextField(25);
        nameTF.setText(initialPath);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseButtonClicked(dialogTitle);
            }
        });
    }

    /**
     * Determines whether this will be a directory or file selector;
     * @param selectDirs
     */
    public void setSelectDirs(boolean selectDirs) {
        this.selectDirs = selectDirs;
    }

    private void browseButtonClicked(String dialogTitle) {
        JFileChooser chooser;

        if(selectDirs) {
            chooser = new JFileChooser(nameTF.getText());
            chooser.setApproveButtonText("Select Folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            // get the parent dir of the file
            File selectorCurrentDir;
            File f = new File(nameTF.getText());
            if (f.isDirectory()) {
                selectorCurrentDir = f;
            } else {
                selectorCurrentDir = f.getParentFile();
            }

            chooser = new JFileChooser(selectorCurrentDir);
            chooser.setApproveButtonText("Select File");
            if (fileFilter != null) {
                // First remove the All Files option...
                chooser.setAcceptAllFileFilterUsed(false);
                // ... then add the extension filter corresponding to the saved file type...
                chooser.addChoosableFileFilter(fileFilter);
                // ... then add back the All Files option so that it is at the end
                chooser.setAcceptAllFileFilterUsed(true);
            }
        }
        chooser.setDialogTitle(dialogTitle);

        chooser.showOpenDialog(PixelitorWindow.getInstance());
        File selectedFile = chooser.getSelectedFile();
        if (selectedFile != null) {
            String fileName = selectedFile.toString();

            if (!selectDirs) {
                String extension = FileExtensionUtils.getFileExtension(selectedFile.getName());
                if (extension == null) { // the user entered no extension
                    if (fileFilter != null) {
                        fileName = fileName + "." + fileFilter.getExtensions()[0];
                    }
                }
            }

            nameTF.setText(fileName);
        }
    }

    public JTextField getNameTF() {
        return nameTF;
    }

    public JButton getBrowseButton() {
        return button;
    }

    public File getSelectedFile() {
        String s = nameTF.getText();
        File f = new File(s);

        return f;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public void setFileFilter(FileNameExtensionFilter fileFilter) {
        this.fileFilter = fileFilter;
    }
}
