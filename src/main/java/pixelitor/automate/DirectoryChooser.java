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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * The GUI elements of a directory chooser are separated into this non-component class
 * so that they can be reused with different layout managers
 */
class DirectoryChooser {
    private final JTextField dirTF;
    private final JButton button = new JButton("Browse...");

    DirectoryChooser(String initialPath, final String dialogTitle) {
        dirTF = new JTextField(20);
        dirTF.setText(initialPath);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(dirTF.getText());
                chooser.setApproveButtonText("Select Directory");
                chooser.setDialogTitle(dialogTitle);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                chooser.showOpenDialog(PixelitorWindow.getInstance());
                File selectedFile = chooser.getSelectedFile();
                if (selectedFile != null) {
                    String fileName = selectedFile.toString();
                    dirTF.setText(fileName);
                }
            }
        });
    }

    public JTextField getDirTF() {
        return dirTF;
    }

    public JButton getButton() {
        return button;
    }

    public File getSelectedDir() {
        String s = dirTF.getText();
        File f = new File(s);

        return f;
    }
}
