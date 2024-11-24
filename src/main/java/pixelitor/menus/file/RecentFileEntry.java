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

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Information about a recently opened file.
 */
public class RecentFileEntry {
    private final File file;

    // the position of this entry in the recent files menu
    private int menuPosition;

    public RecentFileEntry(File file) {
        this.file = file;
    }

    public String getDisplayText() {
        return menuPosition + ". " + file.getName();
    }

    public File getFile() {
        return file;
    }

    public void setMenuPosition(int menuPosition) {
        this.menuPosition = menuPosition;
    }

    public String getToolTipText() {
        return file.getAbsolutePath();
    }

    public String getFullPath() {
        String retVal;
        try {
            retVal = file.getCanonicalPath();
        } catch (IOException e) {
            retVal = file.getAbsolutePath();
        }
        return retVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecentFileEntry that = (RecentFileEntry) o;
        return Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(file);
    }
}
