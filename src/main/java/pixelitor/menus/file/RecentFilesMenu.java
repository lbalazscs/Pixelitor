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

    private final BoundedUniqueList<RecentFileEntry> recentFileEntries;

    private RecentFilesMenu() {
        super(i18n("recent_files"));

        // the menu item for clearing the recent files history
        clearMenuItem = new JMenuItem(i18n("clear_recent"));
        clearMenuItem.addActionListener(e -> clear());

        recentFileEntries = AppPreferences.loadRecentFiles();
        updateMenuItems();
    }

    /**
     * Clears the recent files history and updates the menu.
     */
    private void clear() {
        try {
            AppPreferences.removeRecentFiles();
            recentFileEntries.clear();
            updateMenuItems();
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    }

    public void addRecentFile(File f) {
        if (f.exists()) {
            recentFileEntries.addToFront(new RecentFileEntry(f));
            updateMenuItems();
        }
    }

    public BoundedUniqueList<RecentFileEntry> getRecentFileEntries() {
        return recentFileEntries;
    }

    /**
     * Rebuilds the menu items to reflect the current state of the list.
     */
    private void updateMenuItems() {
        removeAll(); // removes existing menu items

        for (int i = 0; i < recentFileEntries.size(); i++) {
            RecentFileEntry fileEntry = recentFileEntries.get(i);
            fileEntry.setMenuPosition(i + 1);
            add(new RecentFilesMenuItem(fileEntry));
        }

        if (!recentFileEntries.isEmpty()) {
            addSeparator();
        }

        add(clearMenuItem);
    }
}

