/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.utils;

import pixelitor.NewImage;
import pixelitor.Pixelitor;
import pixelitor.TipsOfTheDay;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.gui.Desktop;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.IntTextField;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.history.History;
import pixelitor.io.FileChoosers;
import pixelitor.layers.LayerButtonLayout;
import pixelitor.menus.file.RecentFileInfo;
import pixelitor.menus.file.RecentFilesMenu;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * Static methods for saving and loading application preferences
 */
public final class AppPreferences {
    private static final String FRAME_X_KEY = "window_x";
    private static final String FRAME_Y_KEY = "window_y";
    private static final String FRAME_WIDTH_KEY = "window_width";
    private static final String FRAME_HEIGHT_KEY = "window_height";

    private static final String NEW_IMAGE_WIDTH = "new_image_width";
    private static final String NEW_IMAGE_HEIGHT = "new_image_height";
    private static Dimension newImageSize = null;

    private static final String RECENT_FILE_PREFS_KEY = "recent_file_";

    private static final Preferences mainUserNode = Preferences.userNodeForPackage(Pixelitor.class);
    private static final Preferences recentFilesUserNode = Preferences.userNodeForPackage(RecentFilesMenu.class);

    private static final String FG_COLOR_KEY = "fg_color";
    private static final String BG_COLOR_KEY = "bg_color";

    private static final String LAST_OPEN_DIR_KEY = "last_open_dir";
    private static final String LAST_SAVE_DIR_KEY = "last_save_dir";
    private static final String HISTOGRAMS_SHOWN_KEY = "histograms_shown";
    private static final String LAYERS_SHOWN_KEY = "layers_shown";
    private static final String TOOLS_SHOWN_KEY = "tools_shown";
    private static final String STATUS_BAR_SHOWN_KEY = "status_bar_shown";

    private static final String UNDO_LEVELS_KEY = "undo_levels";

    private static final String THUMB_SIZE_KEY = "thumb_size";

    private AppPreferences() {
    }

    private static void saveFramePosition(Window window) {
        int x = window.getX();
        int y = window.getY();
        int width = window.getWidth();
        int height = window.getHeight();

        mainUserNode.putInt(FRAME_X_KEY, x);
        mainUserNode.putInt(FRAME_Y_KEY, y);
        mainUserNode.putInt(FRAME_WIDTH_KEY, width);
        mainUserNode.putInt(FRAME_HEIGHT_KEY, height);
    }

    public static Dimension getNewImageSize() {
        if (newImageSize == null) {
            loadNewImageSize();
        }
        return newImageSize;
    }

    private static void loadNewImageSize() {
        int defaultWidth = 600;
        int defaultHeight = 400;
        Dimension desktopSize = Desktop.INSTANCE.getDesktopSize();
        if (desktopSize != null) {
            defaultWidth = Math.max(600, desktopSize.width - 30);
            defaultHeight = Math.max(400, desktopSize.height - 50);
        }
        int width = mainUserNode.getInt(NEW_IMAGE_WIDTH, defaultWidth);
        int height = mainUserNode.getInt(NEW_IMAGE_HEIGHT, defaultHeight);
        newImageSize = new Dimension(width, height);
    }

    private static void saveNewImageSize() {
        Dimension lastNew = NewImage.getLastNew();
        if (lastNew != null) {
            mainUserNode.putInt(NEW_IMAGE_WIDTH, lastNew.width);
            mainUserNode.putInt(NEW_IMAGE_HEIGHT, lastNew.height);
        }
    }

    public static void loadFramePosition(Window window) {
        int x = mainUserNode.getInt(FRAME_X_KEY, 0);
        int y = mainUserNode.getInt(FRAME_Y_KEY, 0);
        int width = mainUserNode.getInt(FRAME_WIDTH_KEY, 0);
        int height = mainUserNode.getInt(FRAME_HEIGHT_KEY, 0);

        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        if ((width <= 0) || (height <= 0)) {
            width = screenBounds.width;
            height = screenBounds.height;
        }
        if (width > screenBounds.width) {
            width = screenBounds.width;
        }
        if (height > screenBounds.height) {
            height = screenBounds.height;
        }

        if ((x < 0) || (y < 0)) {
            x = 0;
            y = 0;
        }

        window.setLocation(new Point(x, y));
        window.setSize(width, height);
    }

    public static BoundedUniqueList<RecentFileInfo> loadRecentFiles() {
        BoundedUniqueList<RecentFileInfo> retVal = new BoundedUniqueList<>(RecentFilesMenu.MAX_RECENT_FILES);
        for (int i = 0; i < RecentFilesMenu.MAX_RECENT_FILES; i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String fileName = recentFilesUserNode.get(key, null);
            if (fileName == null) {
                break;
            }
            File file = new File(fileName);

            if (file.exists()) {
                RecentFileInfo fileInfo = new RecentFileInfo(file);
                retVal.addIfNotThere(fileInfo);
            }
        }
        return retVal;
    }

    private static void saveRecentFiles(BoundedUniqueList<RecentFileInfo> fileInfos) {
        for (int i = 0; i < fileInfos.size(); i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String value = fileInfos.get(i).getSavedName();
            recentFilesUserNode.put(key, value);
        }
    }

    public static void removeRecentFiles() {
        for (int i = 0; i < RecentFilesMenu.MAX_RECENT_FILES; i++) {
            recentFilesUserNode.remove(RECENT_FILE_PREFS_KEY + i);
        }
    }

    public static File loadLastOpenDir() {
        return loadDir(LAST_OPEN_DIR_KEY);
    }

    public static File loadLastSaveDir() {
        return loadDir(LAST_SAVE_DIR_KEY);
    }

    private static File loadDir(String key) {
        String s = mainUserNode.get(key, null);

        if (s == null) {
            return getDocumentsDir();
        }
        File dir = new File(s);
        if (!dir.exists()) {
            return getDocumentsDir();
        }
        if (!dir.isDirectory()) {
            return getDocumentsDir();
        }
        return dir;
    }

    private static File getDocumentsDir() {
        return FileSystemView.getFileSystemView().getDefaultDirectory();
    }

    private static void saveLastOpenDir() {
        saveDir(FileChoosers.getLastOpenDir(), LAST_OPEN_DIR_KEY);
    }

    private static void saveLastSaveDir() {
        saveDir(FileChoosers.getLastSaveDir(), LAST_SAVE_DIR_KEY);
    }

    private static void saveDir(File f, String key) {
        if (f != null) {
            mainUserNode.put(key, f.getAbsolutePath());
        } else {
            mainUserNode.put(key, null);
        }
    }

    public static int loadUndoLevels() {
        int retVal = mainUserNode.getInt(UNDO_LEVELS_KEY, -1);
        if (retVal == -1) {
            return Math.min(5, getDefaultUndoLevels());
        }
        return retVal;
    }

    private static void saveUndoLevels() {
        mainUserNode.putInt(UNDO_LEVELS_KEY, History.getUndoLevels());
    }

    public static int loadThumbSize() {
        int retVal = mainUserNode.getInt(THUMB_SIZE_KEY, LayerButtonLayout.SMALL_THUMB_SIZE);
        return retVal;
    }

    private static void saveThumbSize() {
        mainUserNode.putInt(THUMB_SIZE_KEY, LayerButtonLayout.getThumbSize());
    }

    private static void savePreferencesBeforeExit() {
        saveRecentFiles(RecentFilesMenu.getInstance().getRecentFileInfosForSaving());
        saveFramePosition(PixelitorWindow.getInstance());
        saveLastOpenDir();
        saveLastSaveDir();
        saveFgBgColors();
        WorkSpace.saveVisibility();
        saveUndoLevels();
        saveThumbSize();
        TipsOfTheDay.saveNextTipNr();
        saveNewImageSize();
    }

    public static Color loadFgColor() {
        int fgInt = mainUserNode.getInt(FG_COLOR_KEY, 0xFF000000);
        return new Color(fgInt);
    }

    public static Color loadBgColor() {
        int bgInt = mainUserNode.getInt(BG_COLOR_KEY, 0xFFFFFFFF);
        return new Color(bgInt);
    }

    private static void saveFgBgColors() {
        Color fgColor = FgBgColors.getFG();
        if (fgColor != null) {
            mainUserNode.putInt(FG_COLOR_KEY, fgColor.getRGB());
        }

        Color bgColor = FgBgColors.getBG();
        if (bgColor != null) {
            mainUserNode.putInt(BG_COLOR_KEY, bgColor.getRGB());
        }
    }

    public static String getLookAndFeelClass() {
        UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels) {
            if (lookAndFeel.getName().equals("Nimbus")) {
                return lookAndFeel.getClassName();
            }
        }
        return UIManager.getSystemLookAndFeelClassName();
    }

    public static void savePrefsAndExit() {
        savePreferencesBeforeExit();
        System.exit(0);
    }

    public static class WorkSpace {
        /**
         * Utility class with static methods
         */
        private WorkSpace() {
        }

        private static final boolean DEFAULT_HISTOGRAMS_VISIBILITY = false;
        private static final boolean DEFAULT_TOOLS_VISIBILITY = true;
        private static final boolean DEFAULT_LAYERS_VISIBILITY = true;
        private static final boolean DEFAULT_STATUS_BAR_VISIBILITY = true;

        static boolean loaded = false;
        private static boolean histogramsVisibility;
        private static boolean toolsVisibility;
        private static boolean layersVisibility;
        private static boolean statusBarVisibility;

        private static void load() {
            if (loaded) {
                return;
            }
            histogramsVisibility = mainUserNode.getBoolean(HISTOGRAMS_SHOWN_KEY, DEFAULT_HISTOGRAMS_VISIBILITY);
            toolsVisibility = mainUserNode.getBoolean(TOOLS_SHOWN_KEY, DEFAULT_TOOLS_VISIBILITY);
            layersVisibility = mainUserNode.getBoolean(LAYERS_SHOWN_KEY, DEFAULT_LAYERS_VISIBILITY);
            statusBarVisibility = mainUserNode.getBoolean(STATUS_BAR_SHOWN_KEY, DEFAULT_STATUS_BAR_VISIBILITY);
            loaded = true;
        }

        public static void setDefault(PixelitorWindow pw) {
            pw.setHistogramsVisibility(DEFAULT_HISTOGRAMS_VISIBILITY, false);
            pw.setToolsVisibility(DEFAULT_TOOLS_VISIBILITY, false);
            pw.setLayersVisibility(DEFAULT_LAYERS_VISIBILITY, false);
            pw.setStatusBarVisibility(DEFAULT_STATUS_BAR_VISIBILITY, false);

            pw.getContentPane().revalidate();

            histogramsVisibility = DEFAULT_HISTOGRAMS_VISIBILITY;
            toolsVisibility = DEFAULT_TOOLS_VISIBILITY;
            layersVisibility = DEFAULT_LAYERS_VISIBILITY;
            statusBarVisibility = DEFAULT_STATUS_BAR_VISIBILITY;
        }

        public static boolean getHistogramsVisibility() {
            load();
            return histogramsVisibility;
        }

        public static boolean getLayersVisibility() {
            load();
            return layersVisibility;
        }

        public static boolean getStatusBarVisibility() {
            load();
            return statusBarVisibility;
        }

        public static boolean getToolsVisibility() {
            load();
            return toolsVisibility;
        }

        private static void saveVisibility() {
            mainUserNode.putBoolean(HISTOGRAMS_SHOWN_KEY, histogramsVisibility);
            mainUserNode.putBoolean(LAYERS_SHOWN_KEY, layersVisibility);
            mainUserNode.putBoolean(TOOLS_SHOWN_KEY, toolsVisibility);
            mainUserNode.putBoolean(STATUS_BAR_SHOWN_KEY, statusBarVisibility);
        }

        public static void setLayersVisibility(boolean v) {
            layersVisibility = v;
            PixelitorWindow pixelitorWindow = PixelitorWindow.getInstance();
            pixelitorWindow.setLayersVisibility(v, true);
        }

        public static void setHistogramsVisibility(boolean v) {
            histogramsVisibility = v;
            PixelitorWindow pixelitorWindow = PixelitorWindow.getInstance();
            pixelitorWindow.setHistogramsVisibility(v, true);
        }

        public static void setToolsVisibility(boolean v) {
            toolsVisibility = v;
            PixelitorWindow pixelitorWindow = PixelitorWindow.getInstance();
            pixelitorWindow.setToolsVisibility(v, true);
        }

        public static void setStatusBarVisibility(boolean v) {
            statusBarVisibility = v;
            PixelitorWindow pixelitorWindow = PixelitorWindow.getInstance();
            pixelitorWindow.setStatusBarVisibility(v, true);
        }
    }

    public static class Panel extends JPanel {
        private final JTextField undoLevelsTF;
        private final JComboBox<IntChoiceParam.Value> thumbSizeCB;

        Panel() {
            setLayout(new GridBagLayout());
            GridBagHelper gridBagHelper = new GridBagHelper(this);

            undoLevelsTF = new IntTextField(3);
            undoLevelsTF.setText(String.valueOf(History.getUndoLevels()));
            gridBagHelper.addLabelWithControl("Undo/Redo Levels: ", undoLevelsTF);

            IntChoiceParam.Value[] thumbSizes = {
                    new IntChoiceParam.Value("24x24 pixels", 24),
                    new IntChoiceParam.Value("48x48 pixels", 48),
                    new IntChoiceParam.Value("72x72 pixels", 72),
                    new IntChoiceParam.Value("96x96 pixels", 96),
            };
            thumbSizeCB = new JComboBox<>(thumbSizes);

            int currentSize = LayerButtonLayout.getThumbSize();
            thumbSizeCB.setSelectedIndex(currentSize / 24 - 1);

            gridBagHelper.addLabelWithControl("Layer/Mask Thumb Sizes: ", thumbSizeCB);
            thumbSizeCB.addActionListener(e -> updateThumbSize());
        }

        private int getUndoLevels() {
            String s = undoLevelsTF.getText();
            int retVal = Integer.parseInt(s);
            return retVal;
        }

        private void updateThumbSize() {
            int newSize = ((IntChoiceParam.Value) thumbSizeCB.getSelectedItem()).getIntValue();
            LayerButtonLayout.setThumbSize(newSize);
        }

        public static void showInDialog() {
            Panel p = new Panel();
            OKCancelDialog d = new OKCancelDialog(p, "Preferences") {
                @Override
                protected void dialogAccepted() {
                    int undoLevels = p.getUndoLevels();
                    History.setUndoLevels(undoLevels);

                    p.updateThumbSize();

                    close();
                }
            };
            d.setVisible(true);
        }
    }

    public static Preferences getMainUserNode() {
        return mainUserNode;
    }

    private static int getDefaultUndoLevels() {
        int sizeInMegaBytes = Utils.getMaxHeapInMegabytes();
        int retVal = 1 + (sizeInMegaBytes / 50);

        // rounds up to the nearest multiple of 5
        return ((retVal + 4) / 5) * 5;
    }
}
