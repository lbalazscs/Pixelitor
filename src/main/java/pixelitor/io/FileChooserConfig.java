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

package pixelitor.io;

import pixelitor.Composition;

import javax.swing.filechooser.FileFilter;

/**
 * Configuration information for the save file chooser dialog.
 */
public record FileChooserConfig(String suggestedFileName,
                                FileFilter defaultFileFilter,
                                FormatSelection formatSelection) {
    public FileChooserConfig {
        if (suggestedFileName == null) {
            throw new IllegalArgumentException();
        }
        if (!formatSelection.isValid(defaultFileFilter)) {
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
            fileFilter, FormatSelection.SUPPORTED);
    }

    /**
     * The file formats that can be chosen in the save dialog.
     */
    public enum FormatSelection {
        /**
         * A single file format, determined by the default file filter.
         */
        SINGLE {
            @Override
            public boolean isValid(FileFilter defaultFileFilter) {
                return defaultFileFilter != null;
            }
        },
        /**
         * All save file formats supported by Pixelitor.
         */
        SUPPORTED {
            @Override
            public boolean isValid(FileFilter defaultFileFilter) {
                return true;
            }
        },
        /**
         * Any format (used for ImageMagick export).
         */
        ANY {
            @Override
            public boolean isValid(FileFilter defaultFileFilter) {
                // no default file filter should be set for this mode
                return defaultFileFilter == null;
            }
        };

        public abstract boolean isValid(FileFilter defaultFileFilter);
    }
}
