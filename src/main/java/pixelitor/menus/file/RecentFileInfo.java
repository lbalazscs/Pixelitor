/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.menus.file;

import java.io.File;
import java.io.IOException;

/**
 * Information about a recently opened file
 */
public class RecentFileInfo {
    private final File file;
    private int nr;

    public RecentFileInfo(File file) {
        this.file = file;
    }

    public String getMenuName() {
        return nr + ". " + file.getName();
    }

    public File getFile() {
        return file;
    }

    public void setNr(int nr) {
        this.nr = nr;
    }

    public String getSavedName() {
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

        RecentFileInfo fileInfo = (RecentFileInfo) o;


        if (file != null) {
            return file.equals(fileInfo.file);
        } else {
            return fileInfo.file == null;
        }

    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }
}
