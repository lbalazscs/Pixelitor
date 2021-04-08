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

package pixelitor.gui.utils;

import pixelitor.io.FileUtils;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Optional;

/**
 * The file chooser used for saving files. It tries to
 * handle file extensions intelligently.
 */
public class SaveFileChooser extends ConfirmSaveFileChooser {
    private String extension;

    public SaveFileChooser(File currentDirPath) {
        super(currentDirPath);
        setAcceptAllFileFilterUsed(false);
    }

    @Override
    public File getSelectedFile() {
        File f = super.getSelectedFile();
        if (f == null) {
            return null;
        }

        Optional<String> foundExt = FileUtils.findExtension(f.getName());
        if (foundExt.isEmpty()) {
            // the user has entered no extension
            // determine it from the active FileFilter
            f = addExtension(f);
        } else {
            boolean supported = FileUtils.hasSupportedOutputExt(f.getName());
            if (!supported) {
                f = addExtension(f);
            } else {
                extension = foundExt.get();
            }
        }

        return f;
    }

    private File addExtension(File f) {
        extension = getExtensionFromFileFilter();
        if (extension != null) {
            f = new File(f.getAbsolutePath() + '.' + extension);
        }
        return f;
    }

    public String getExtension() {
        return extension;
    }

    private String getExtensionFromFileFilter() {
        FileFilter currentFilter = getFileFilter();
        if (currentFilter instanceof FileNameExtensionFilter extFilter) { // not "Accept All"
            return extFilter.getExtensions()[0];
        }
        return null;
    }
}
