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

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * A saving JFileChooser that automatically adds a file
 * extension based on the selected file filter.
 */
public class SaveFileChooser extends ValidatingSaveFileChooser {
    public SaveFileChooser(File currentDir) {
        super(currentDir);

        setAcceptAllFileFilterUsed(false);
    }

    @Override
    public File getSelectedFile() {
        File f = super.getSelectedFile();
        if (f == null) {
            return null;
        }

        String ext = FileUtils.getExtension(f.getName());
        if (ext == null) {
            // the user has entered no extension, so
            // determine it from the selected file filter
            f = withExtension(f);
        } else {
            boolean supported = FileUtils.isSupportedOutputExt(ext);
            if (!supported) {
                f = withExtension(f);
            }
        }

        return f;
    }

    private File withExtension(File f) {
        String extension = getExtensionFromFileFilter();
        if (extension != null) {
            f = new File(f.getAbsolutePath() + '.' + extension);
        }
        return f;
    }

    private String getExtensionFromFileFilter() {
        FileFilter currentFilter = getFileFilter();
        if (currentFilter instanceof FileNameExtensionFilter extFilter) { // not "Accept All"
            return extFilter.getExtensions()[0];
        }
        return null;
    }
}
