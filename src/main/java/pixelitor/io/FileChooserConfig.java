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

package pixelitor.io;

import pixelitor.Composition;

import javax.swing.filechooser.FileFilter;

/**
 * Configuration information for the save file chooser dialog.
 */
public record FileChooserConfig(String suggestedFileName,
                                FileFilter defaultFileFilter,
                                SelectableFormats formats) {
    public FileChooserConfig {
        if (suggestedFileName == null) {
            throw new IllegalArgumentException();
        }
        if (!formats.allows(defaultFileFilter)) {
            throw new IllegalArgumentException();
        }
    }

    public static FileChooserConfig forSavingComp(Composition comp) {
        String suggestedFileName = comp.getName();
        String defaultExt = FileUtils
            .findExtension(comp.getName())
            .orElse(FileFormat.getLastSaved().toString());
        FileFilter fileFilter = FileFormat.fromExtension(defaultExt)
            .orElse(FileFormat.JPG)
            .getFileFilter();

        return new FileChooserConfig(suggestedFileName,
            fileFilter, SelectableFormats.SUPPORTED);
    }

    /**
     * The file formats that can be chosen in the save dialog.
     */
    public enum SelectableFormats {
        /**
         * A single file format, determined by the default file filter.
         */
        SINGLE {
            @Override
            public boolean allows(FileFilter defaultFileFilter) {
                return defaultFileFilter != null;
            }
        },
        /**
         * All save file formats supported by Pixelitor.
         */
        SUPPORTED {
            @Override
            public boolean allows(FileFilter defaultFileFilter) {
                return true;
            }
        },
        /**
         * Any format (for the ImageMagick export).
         */
        ANY {
            @Override
            public boolean allows(FileFilter defaultFileFilter) {
                return defaultFileFilter == null;
            }
        };

        public abstract boolean allows(FileFilter defaultFileFilter);
    }
}
