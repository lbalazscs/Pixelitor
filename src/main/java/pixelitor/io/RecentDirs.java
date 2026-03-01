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

import pixelitor.utils.AppPreferences;

import java.io.File;

/**
 * Static methods for tracking the most recently used open and save directories.
 */
public class RecentDirs {
    private static volatile File lastOpenDir = AppPreferences.loadLastOpenDir();
    private static volatile File lastSaveDir = AppPreferences.loadLastSaveDir();

    private RecentDirs() {
    }

    public static File getLastOpen() {
        assert lastOpenDir != null;
        return lastOpenDir;
    }

    public static File getLastSave() {
        assert lastSaveDir != null;
        return lastSaveDir;
    }

    public static String getLastOpenPath() {
        return lastOpenDir.getAbsolutePath();
    }

    public static String getLastSavePath() {
        return lastSaveDir.getAbsolutePath();
    }

    public static void setLastOpen(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            lastOpenDir = dir;
        }
    }

    public static void setLastSave(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            lastSaveDir = dir;
        }
    }
}
