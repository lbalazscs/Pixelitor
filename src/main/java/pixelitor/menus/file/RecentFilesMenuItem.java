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
package pixelitor.menus.file;

import pixelitor.io.IO;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.io.File;

import static java.lang.String.format;

/**
 * A menu item for a recently opened file.
 */
public class RecentFilesMenuItem extends JMenuItem {
    private final RecentFileEntry fileEntry;

    public RecentFilesMenuItem(RecentFileEntry fileEntry) {
        super(fileEntry.getDisplayText());
        this.fileEntry = fileEntry;
        setToolTipText(fileEntry.getToolTipText());
        addActionListener(e -> openRecentFileAsync());
    }

    private void openRecentFileAsync() {
        File file = fileEntry.getFile();
        if (file.exists()) {
            IO.openFileAsync(file, true);
        } else {
            // the file was deleted since Pixelitor started
            String errorMessage = format("The file %s doesn't exist.", file);
            Messages.showError("Error", errorMessage);
        }
    }
}
