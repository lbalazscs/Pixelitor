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

package pixelitor.gui.utils;

import pixelitor.gui.PixelitorWindow;
import pixelitor.io.FileUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import static pixelitor.gui.utils.BrowseFilesSupport.SelectionMode.FILE;

/**
 * The GUI elements of a file/directory chooser (a textfield and a
 * "Browse..." button) are separated into this non-component class
 * so that they can be reused with different layout managers
 */
public class BrowseFilesSupport {
    private static final String BROWSE_BUTTON_TEXT = "Browse...";
    private static final int PATH_FIELD_COLUMNS = 25;

    private JTextField pathTF;
    private final JButton browseButton = new JButton(BROWSE_BUTTON_TEXT);
    private String chooserDialogTitle;
    private FileNameExtensionFilter fileFilter; // used for filtering when in file selection mode

    public enum SelectionMode {
        DIRECTORY {
            @Override
            public JFileChooser createChooser(String path, FileNameExtensionFilter fileFilter) {
                var chooser = new JFileChooser(path);
                chooser.setApproveButtonText("Select Folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                return chooser;
            }
        }, FILE {
            @Override
            public JFileChooser createChooser(String path, FileNameExtensionFilter fileFilter) {
                File currentFile = new File(path);
                File startingDirectory = currentFile.isDirectory() ?
                    currentFile :
                    currentFile.getParentFile();

                var chooser = new JFileChooser(startingDirectory);
                chooser.setApproveButtonText("Select File");
                if (fileFilter != null) {
                    configureFilter(fileFilter, chooser);
                }
                return chooser;
            }

            private static void configureFilter(FileNameExtensionFilter fileFilter, JFileChooser chooser) {
                // First remove the "All Files" option...
                chooser.setAcceptAllFileFilterUsed(false);
                // ... then add the extension filter corresponding to the saved file type...
                chooser.addChoosableFileFilter(fileFilter);
//            // ... then add back the "All Files" option so that it is at the end
//            chooser.setAcceptAllFileFilterUsed(true);
            }
        };

        public abstract JFileChooser createChooser(String path, FileNameExtensionFilter fileFilter);
    }

    private SelectionMode mode;

    public BrowseFilesSupport(String initialPath) {
        init(initialPath);
    }

    public BrowseFilesSupport(String defaultPath,
                              String chooserDialogTitle,
                              SelectionMode mode) {
        this.chooserDialogTitle = chooserDialogTitle;
        this.mode = mode;
        init(defaultPath);
    }

    private void init(String defaultPath) {
        pathTF = new JTextField(PATH_FIELD_COLUMNS);
        pathTF.setText(defaultPath);
        browseButton.addActionListener(e -> openChooserDialog(chooserDialogTitle));
    }

    public void setSelectionMode(SelectionMode mode) {
        this.mode = mode;
    }

    private void openChooserDialog(String title) {
        JFileChooser chooser = mode.createChooser(pathTF.getText(), fileFilter);

        chooser.setDialogTitle(title);
        chooser.showOpenDialog(PixelitorWindow.get());
        updateSelectedPath(chooser.getSelectedFile());
    }

    private void updateSelectedPath(File selectedFile) {
        if (selectedFile != null) {
            String filePath = selectedFile.toString();

            if (mode == FILE) {
                boolean noExtGivenByUser = !FileUtils.hasExtension(selectedFile.getName());
                if (noExtGivenByUser && fileFilter != null) {
                    filePath = filePath + '.' + fileFilter.getExtensions()[0];
                }
            }

            pathTF.setText(filePath);
        }
    }

    public JTextField getPathTextField() {
        return pathTF;
    }

    public JButton getBrowseButton() {
        return browseButton;
    }

    public File getSelectedFile() {
        return new File(pathTF.getText());
    }

    public void setChooserDialogTitle(String chooserDialogTitle) {
        this.chooserDialogTitle = chooserDialogTitle;
    }

    public void setFileFilter(FileNameExtensionFilter fileFilter) {
        this.fileFilter = fileFilter;
    }
}
