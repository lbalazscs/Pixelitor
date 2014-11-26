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

import javax.swing.*;
import java.io.File;

/**
 * A menu item for the recent file entries
 */
public class RecentFilesMenuItem extends JMenuItem {
    private final RecentFileInfo fileInfo;

    public RecentFilesMenuItem(RecentFileInfo fileInfo) {
        super(fileInfo.getMenuName());

        this.fileInfo = fileInfo;
        File file = fileInfo.getFile();

//        URL url = null;
//        try {
//            url = file.toURI().toURL();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//
//        setToolTipText("<html><img src=\"" + url.toString() + "\" width=200 height=200 /> " + file.getAbsolutePath());

        setToolTipText(file.getAbsolutePath());
    }

    public RecentFileInfo getFileInfo() {
        return fileInfo;
    }
}
