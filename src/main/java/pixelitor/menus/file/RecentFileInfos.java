/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import java.util.ArrayList;
import java.util.List;

/**
 * The list of RecentFileInfo objects
 */
public class RecentFileInfos {
    public static final int MAX_RECENT_FILES = 10;

    private List<RecentFileInfo> fileInfos = new ArrayList<>(MAX_RECENT_FILES);

    public void addToFront(RecentFileInfo fileInfo) {
        if (fileInfos.contains(fileInfo)) {
            fileInfos.remove(fileInfo);
        }
        fileInfos.add(0, fileInfo); // add to the front

        if (fileInfos.size() > MAX_RECENT_FILES) {
            fileInfos.remove(MAX_RECENT_FILES);
        }
    }

    public void addIfNotPresent(RecentFileInfo fileInfo) {
        if (!fileInfos.contains(fileInfo)) {
            fileInfos.add(fileInfo);
        }
    }

    public void clear() {
        fileInfos.clear();
    }

    public int size() {
        return fileInfos.size();
    }

    public RecentFileInfo get(int index) {
        return fileInfos.get(index);
    }

    public boolean isEmpty() {
        return fileInfos.isEmpty();
    }
}
