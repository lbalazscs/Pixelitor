/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

/**
 * Information about a recently opened file
 */
public class RecentFile {
    private final File file;
    private int nr;

    public RecentFile(File file) {
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

        RecentFile recentFile = (RecentFile) o;

        if (file != null) {
            return file.equals(recentFile.file);
        } else {
            return recentFile.file == null;
        }
    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }
}
