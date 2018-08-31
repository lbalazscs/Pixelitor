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

import pixelitor.utils.AppPreferences;
import pixelitor.utils.BoundedUniqueList;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;

/**
 * The "File/Recent Files" menu
 */
public final class RecentFilesMenu extends JMenu {
    public static final int MAX_RECENT_FILES = 10;

    private static RecentFilesMenu singleInstance;

    private final JMenuItem clearMenuItem;

    private BoundedUniqueList<RecentFile> recentFiles;

    private RecentFilesMenu() {
        super("Recent Files");

        clearMenuItem = new JMenuItem("Clear Recent Files");
        clearMenuItem.addActionListener(e -> {
            try {
                clear();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        });
        load();
        rebuildGUI();
    }

    private void clear() {
        AppPreferences.removeRecentFiles();
        recentFiles.clear();
        clearGUI();
    }

    public static RecentFilesMenu getInstance() {
        assert EventQueue.isDispatchThread() : "not EDT thread";

        if (singleInstance == null) {
            //noinspection NonThreadSafeLazyInitialization
            singleInstance = new RecentFilesMenu();
        }
        return singleInstance;
    }

    public void addFile(File f) {
        if (f.exists()) {
            RecentFile recentFile = new RecentFile(f);
            recentFiles.addToFront(recentFile);
            rebuildGUI();
        }
    }

    private void load() {
        recentFiles = AppPreferences.loadRecentFiles();
    }

    private void clearGUI() {
        removeAll();
    }

    public BoundedUniqueList<RecentFile> getRecentFileInfosForSaving() {
        return recentFiles;
    }

    private void rebuildGUI() {
        clearGUI();
        for (int i = 0; i < recentFiles.size(); i++) {
            RecentFile recentFile = recentFiles.get(i);
            recentFile.setNr(i + 1);
            RecentFilesMenuItem item = new RecentFilesMenuItem(recentFile);
            add(item);
        }
        if (!recentFiles.isEmpty()) {
            addSeparator();
            add(clearMenuItem);
        }
    }
}

