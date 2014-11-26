/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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

import pixelitor.io.OpenSaveManager;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public final class RecentFilesMenu extends JMenu {
    private static final int DEFAULT_MAX_RECENT_FILES = 10;

    private static RecentFilesMenu singleInstance;

    private final int maxRecentFiles;
    private final JMenuItem clearMenuItem;

    private List<RecentFileInfo> recentFileInfos;

    private final ActionListener fileOpener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                RecentFilesMenuItem mi = (RecentFilesMenuItem) e.getSource();
                File f = mi.getFileInfo().getFile();
                if (f.exists()) {
                    OpenSaveManager.openFile(f);
                } else {
                    JOptionPane.showMessageDialog(null, "The file " + f + " does not exist.", "Problem", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                Dialogs.showExceptionDialog(ex);
            }
        }
    };

    private RecentFilesMenu() {
        super("Recent Files");
        maxRecentFiles = DEFAULT_MAX_RECENT_FILES;
        clearMenuItem = new JMenuItem("Clear Recent Files");
        ActionListener clearer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    clear();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
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

    public static synchronized RecentFilesMenu getInstance() {
        if (singleInstance == null) {
            singleInstance = new RecentFilesMenu();
        }
        return singleInstance;
    }

    public void addFile(File f) {
        if (f.exists()) {
            RecentFileInfo fileInfo = new RecentFileInfo(f);
            if (recentFileInfos.contains(fileInfo)) {
                recentFileInfos.remove(fileInfo);
            }
            recentFileInfos.add(0, fileInfo); // add to the front

            if (recentFileInfos.size() > maxRecentFiles) { // it is now too large
                recentFileInfos.remove(maxRecentFiles);
            }

            rebuildGUI();
        }
    }

    private void load() {
        recentFileInfos = AppPreferences.loadRecentFiles(maxRecentFiles);
    }

    private void clearGUI() {
        removeAll();
    }

    public List<RecentFileInfo> getRecentFileNamesForSaving() {
        if (recentFileInfos.size() > maxRecentFiles) {
            return recentFileInfos.subList(0, maxRecentFiles);
        }

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

