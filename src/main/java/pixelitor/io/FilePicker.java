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

import java.io.File;

/**
 * A common interface for the Swing and native (AWT) file pickers.
 */
public interface FilePicker {
    /**
     * Shows an "open" file picker that allows the selection of supported input files.
     */
    File getSupportedOpenFile();

    /**
     * Shows an "open" file picker that allows the selection of any file.
     */
    File getAnyOpenFile();

    File showSaveDialog(FileChooserConfig config);

    String getSelectedSaveExtension(File selectedFile);
}
