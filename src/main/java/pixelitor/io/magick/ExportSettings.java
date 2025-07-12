/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.io.magick;

import java.util.List;

/**
 * Configuration for ImageMagick export.
 */
public interface ExportSettings {
    /**
     * The settings for file types that don't have a customization dialog.
     */
    ExportSettings DEFAULTS = new ExportSettings() {
        @Override
        public void addMagickOptions(List<String> command) {
            // no extra options added
        }

        @Override
        public String getFormatName() {
            // has no GUI => this method should not be called
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Adds format-specific options to the ImageMagick command line.
     */
    void addMagickOptions(List<String> command);

    /**
     * Returns the ImageMagick format specifier (such as "png32:)
     * or an empty string for the default extension-based format.
     */
    default String getFormatSpecifier() {
        return "";
    }

    String getFormatName();
}
