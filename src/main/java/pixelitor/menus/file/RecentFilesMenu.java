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

import pixelitor.io.OpenSaveManager;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;

public final class RecentFilesMenu extends JMenu {

    private static RecentFilesMenu singleInstance;

    private final int maxRecentFiles;
    private final JMenuItem clearMenuItem;

    private RecentFileInfos recentFileInfos;

    private final ActionListener fileOpener = e -> {
        try {
            RecentFilesMenuItem mi = (RecentFilesMenuItem) e.getSource();
            File f = mi.getFileInfo().getFile();
            if (f.exists()) {
                OpenSaveManager.openFile(f);
            } else {
                // the file was deleted since Pixelitor started
                String message = String.format("The file %s does not exist.", f.toString());
                Dialogs.showErrorDialog("Problem", message);
            }
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    };

    private RecentFilesMenu() {
        super("Recent Files");
        maxRecentFiles = RecentFileInfos.MAX_RECENT_FILES;
        clearMenuItem = new JMenuItem("Clear Recent Files");
        ActionListener clearer = e -> {
            try {
                clear();
            } catch (Exception ex) {
                Dialogs.showExceptionDialog(ex);
            }
        };
        clearMenuItem.addActionListener(clearer);
        load();
        rebuildGUI();

    }

    private void clear() {
        AppPreferences.removeRecentFiles(maxRecentFiles);
        recentFileInfos.clear();
        clearGUI();
    }

    public static RecentFilesMenu getInstance() {
        assert SwingUtilities.isEventDispatchThread();
        if (singleInstance == null) {
            //noinspection NonThreadSafeLazyInitialization
            singleInstance = new RecentFilesMenu();
        }
        return singleInstance;
    }

    public void addFile(File f) {
        if (f.exists()) {
            RecentFileInfo fileInfo = new RecentFileInfo(f);
            recentFileInfos.addToFront(fileInfo);
            rebuildGUI();
        }
    }

    private void load() {
        recentFileInfos = AppPreferences.loadRecentFiles();
    }

    private void clearGUI() {
        removeAll();
    }

    public RecentFileInfos getRecentFileInfosForSaving() {
        return recentFileInfos;
    }

    private void rebuildGUI() {
        clearGUI();
        for (int i = 0, recentFileNamesSize = recentFileInfos.size(); i < recentFileNamesSize; i++) {
            RecentFileInfo fileInfo = recentFileInfos.get(i);
            fileInfo.setNr(i + 1);
            RecentFilesMenuItem item = new RecentFilesMenuItem(fileInfo);
            add(item);
            item.addActionListener(fileOpener);
        }
        if (!recentFileInfos.isEmpty()) {
            addSeparator();
            add(clearMenuItem);
        }
    }
}

