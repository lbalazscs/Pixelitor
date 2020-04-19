/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.automate;

import pixelitor.io.FileFormat;

import javax.swing.*;

/**
 * A combo box for selecting an output format
 */
public class FileFormatSelector extends JComboBox<FileFormat> {
    public FileFormatSelector(FileFormat defaultFormat) {
        super(FileFormat.values());
        setSelectedItem(defaultFormat);
    }

    public FileFormat getSelectedFormat() {
        return (FileFormat) getSelectedItem();
    }
}
