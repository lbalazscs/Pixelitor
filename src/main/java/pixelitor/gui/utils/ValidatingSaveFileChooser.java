/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.io.FileUtils;

import javax.swing.*;
import java.io.File;

import static java.nio.file.Files.isWritable;

/**
 * A saving file chooser that confirms before overwriting an existing file,
 * and also does additional validation, such as checking for valid file names and writable files.
 */
public class ValidatingSaveFileChooser extends JFileChooser {
    private static final char[] INVALID_CHARACTERS =
        {'`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public ValidatingSaveFileChooser(File currentDir) {
        super(currentDir);
    }

    @Override
    public void approveSelection() {
        File f = getSelectedFile();
        String fileName = f.getName();

        if (validateFileName(fileName)) {
            return;
        }

        if (!FileUtils.hasExtension(fileName)) {
            // this can happen when exporting with the "All Files"
            // file filter, because then getSelectedFile() won't
            // automatically add an extension based on the file filter.
            Dialogs.showNoExtensionError(this);
            return;
        }

        if (f.exists()) {
            String msg = "<html><b>" + fileName + "</b> already exists." +
                "<br>Do you want to replace it?";
            boolean overwrite = Dialogs.showYesNoQuestion(this, "Confirmation", msg);
            if (!overwrite) {
                return;
            }
            if (!isWritable(f.toPath())) {
                Dialogs.showFileNotWritableError(this, f);
                return;
            }
        }

        super.approveSelection();
    }

    // not a comprehensive check, but it covers the most common cases
    private boolean validateFileName(String fileName) {
        for (char ch : INVALID_CHARACTERS) {
            if (fileName.indexOf(ch) != -1) {
                // avoids HTML in the message because displaying
                // the < and > characters becomes problematic
                Dialogs.showError(this, "Invalid File Name",
                    "The file name cannot contain the character " + ch + ".");
                return true;
            }
        }
        return false;
    }
}
