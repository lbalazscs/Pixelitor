/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

public record FileChooserInfo(String suggestedFileName,
                              FileFilter defaultFileFilter,
                              boolean singleFormat,
                              boolean anyFormat) {
    public FileChooserInfo {
        assert suggestedFileName != null;
        if (singleFormat) {
            assert defaultFileFilter != null;
        }
        if (anyFormat) {
            assert defaultFileFilter == null;
        }
    }

    public static FileChooserInfo forSavingComp(Composition comp) {
        String suggestedFileName = comp.getName();
        String defaultExt = FileUtils
            .findExtension(comp.getName())
            .orElse(FileFormat.getLastSaved().toString());
        FileFilter fileFilter = FileFormat.fromExtension(defaultExt)
            .orElse(FileFormat.JPG)
            .getFileFilter();

        return new FileChooserInfo(suggestedFileName,
            fileFilter, false, false);
    }

    public static FileChooserInfo forMagickExport(Composition comp) {
        String suggestedFileName = FileUtils.stripExtension(comp.getName());
        return new FileChooserInfo(suggestedFileName,
            null, false, true);
    }

    public static FileChooserInfo forSingleFormat(String suggestedFileName,
                                                  FileFilter fileFilter) {
        return new FileChooserInfo(suggestedFileName, fileFilter, true, false);
    }
}
