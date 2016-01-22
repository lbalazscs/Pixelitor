/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.io.FileExtensionUtils;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class CustomFileChooser extends ConfirmSaveFileChooser {
    private String extension;

    public CustomFileChooser(File currentDirectoryPath) {
        super(currentDirectoryPath);
        setAcceptAllFileFilterUsed(false);
    }

    @Override
    public File getSelectedFile() {
        File f = super.getSelectedFile();
        if (f == null) {
            return null;
        }
        extension = FileExtensionUtils.getFileExtension(f.getName());

        if (extension == null) {
            // the user has entered no extension
            // determine it from the active FileFilter
            extension = getExtensionFromFileFilter();
            f = new File(f.getAbsolutePath() + '.' + extension);
        } else {
            boolean supportedExtension = FileExtensionUtils.isSupportedExtension(f.getName(), FileExtensionUtils.SUPPORTED_OUTPUT_EXTENSIONS);
            if (!supportedExtension) {
                extension = getExtensionFromFileFilter();
                f = new File(f.getAbsolutePath() + '.' + extension);
            }
        }

        return f;
    }

    public String getExtension() {
        return extension;
    }

    private String getExtensionFromFileFilter() {
        FileFilter currentFilter = getFileFilter();
        return ((FileNameExtensionFilter) currentFilter).getExtensions()[0];
    }
}
