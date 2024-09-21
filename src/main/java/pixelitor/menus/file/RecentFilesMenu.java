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

import pixelitor.utils.AppPreferences;
import pixelitor.utils.BoundedUniqueList;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.io.File;

import static pixelitor.utils.Texts.i18n;

/**
 * The "File/Recent Files" menu
 */
public final class RecentFilesMenu extends JMenu {
    public static final int MAX_RECENT_FILES = 10;

    public static final RecentFilesMenu INSTANCE = new RecentFilesMenu();

    private final JMenuItem clearMenuItem;

    private final BoundedUniqueList<RecentFile> recentFiles;

    private RecentFilesMenu() {
        super(i18n("recent_files"));

        clearMenuItem = new JMenuItem(i18n("clear_recent"));
        clearMenuItem.addActionListener(e -> clear());
        recentFiles = AppPreferences.loadRecentFiles();
        rebuildGUI();
    }

    private void clear() {
        try {
            AppPreferences.removeRecentFiles();
            recentFiles.clear();
            rebuildGUI();
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    public void addFile(File f) {
        if (f.exists()) {
            recentFiles.addToFront(new RecentFile(f));
            rebuildGUI();
        }
    }

    private void removeAllMenuItems() {
        removeAll();
    }

    public BoundedUniqueList<RecentFile> getRecentFiles() {
        return recentFiles;
    }

    private void rebuildGUI() {
        removeAllMenuItems();

        for (int i = 0; i < recentFiles.size(); i++) {
            RecentFile recentFile = recentFiles.get(i);
            recentFile.setListPosition(i + 1);
            RecentFilesMenuItem item = new RecentFilesMenuItem(recentFile);
            add(item);
        }

        if (!recentFiles.isEmpty()) {
            addSeparator();
        }

        add(clearMenuItem);
    }
}

