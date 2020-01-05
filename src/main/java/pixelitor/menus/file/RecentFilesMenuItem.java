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
package pixelitor.menus.file;

import pixelitor.io.OpenSave;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.io.File;

import static java.lang.String.format;

/**
 * A menu item for the recent file entries
 */
public class RecentFilesMenuItem extends JMenuItem {
    private final RecentFile recentFile;

    public RecentFilesMenuItem(RecentFile recentFile) {
        super(recentFile.getMenuName());

        this.recentFile = recentFile;
        File file = recentFile.getFile();

//        URL url = null;
//        try {
//            url = file.toURI().toURL();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//
//        setToolTipText("<html><img src=\"" + url.toString() + "\" width=200 height=200 /> " + file.getAbsolutePath());

        setToolTipText(file.getAbsolutePath());
        addActionListener(e -> openAsync());
    }

    private void openAsync() {
        File f = recentFile.getFile();
        if (f.exists()) {
            OpenSave.openFileAsync(f);
        } else {
            // the file was deleted since Pixelitor started
            Messages.showError("Problem",
                    format("The file %s does not exist.", f));
        }
    }
}
