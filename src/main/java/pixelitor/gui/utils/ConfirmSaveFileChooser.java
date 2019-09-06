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

package pixelitor.gui.utils;

import javax.swing.*;
import java.io.File;

import static java.nio.file.Files.isWritable;

/**
 * A save file chooser that confirms before overwriting a file,
 * and also does some other validations (valid file name, writable file).
 */
public class ConfirmSaveFileChooser extends JFileChooser {
    private static final char[] INVALID_CHARACTERS =
        {'`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public ConfirmSaveFileChooser(File currentDir) {
        super(currentDir);
    }

    @Override
    public void approveSelection() {
        File f = getSelectedFile();
        String fileName = f.getName();

        if (invalidFileName(fileName)) {
            return;
        }

        if (f.exists()) {
            String msg = fileName + " exists already. Overwrite?";
            boolean overWrite = Dialogs.showYesNoQuestionDialog(this, "Confirmation", msg);
            if (!overWrite) {
                return;
            }
            if (!isWritable(f.toPath())) {
                Dialogs.showFileNotWritableDialog(this, f);
                return;
            }
        }

        super.approveSelection();
    }

    // an incomplete check but it should cover the most common cases
    private boolean invalidFileName(String fileName) {
        for (char ch : INVALID_CHARACTERS) {
            if (fileName.indexOf(ch) != -1) {
                // no HTML in the message, because then the display
                // of the < and > characters becomes problematic
                Dialogs.showErrorDialog(this, "Invalid filename",
                    "The file name cannot contain the character " + ch + ".");
                return true;
            }
        }
        return false;
    }
}
