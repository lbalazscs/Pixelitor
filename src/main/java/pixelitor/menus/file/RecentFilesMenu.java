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

import pixelitor.io.OpenSaveManager;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.BoundedUniqueList;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * The "File/Recent Files" menu
 */
public final class RecentFilesMenu extends JMenu {
    public static final int MAX_RECENT_FILES = 10;

    private static RecentFilesMenu singleInstance;

    private final JMenuItem clearMenuItem;

    private BoundedUniqueList<RecentFile> recentFiles;

    private final ActionListener fileOpener = e -> {
        try {
            RecentFilesMenuItem mi = (RecentFilesMenuItem) e.getSource();
            File f = mi.getRecentFile().getFile();
            if (f.exists()) {
                OpenSaveManager.openFileAsync(f);
            } else {
                // the file was deleted since Pixelitor started
                Messages.showError("Problem",
                        String.format("The file %s does not exist.", f.toString()));
            }
        } catch (Exception ex) {
            Messages.showException(ex);
        }
    };

    private RecentFilesMenu() {
        super("Recent Files");

        clearMenuItem = new JMenuItem("Clear Recent Files");
        ActionListener clearer = e -> {
            try {
                clear();
            } catch (Exception ex) {
                Messages.showException(ex);
            }
        };
        clearMenuItem.addActionListener(clearer);
        load();
        rebuildGUI();

    }

    private void clear() {
        AppPreferences.removeRecentFiles();
        recentFiles.clear();
        clearGUI();
    }

    public static RecentFilesMenu getInstance() {
        assert SwingUtilities.isEventDispatchThread() : "not EDT thread";

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
        for (int i = 0, recentFileNamesSize = recentFiles.size(); i < recentFileNamesSize; i++) {
            RecentFile recentFile = recentFiles.get(i);
            recentFile.setNr(i + 1);
            RecentFilesMenuItem item = new RecentFilesMenuItem(recentFile);
            add(item);
            item.addActionListener(fileOpener);
        }
        if (!recentFiles.isEmpty()) {
            addSeparator();
            add(clearMenuItem);
        }
    }
}

