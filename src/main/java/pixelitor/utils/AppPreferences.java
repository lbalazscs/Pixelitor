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

package pixelitor.utils;

import pixelitor.NewImage;
import pixelitor.Pixelitor;
import pixelitor.TipsOfTheDay;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.gui.ImageArea;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.history.History;
import pixelitor.io.Directories;
import pixelitor.layers.LayerButtonLayout;
import pixelitor.menus.file.RecentFile;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.menus.view.ShowHideHistogramsAction;
import pixelitor.menus.view.ShowHideLayersAction;
import pixelitor.menus.view.ShowHideStatusBarAction;
import pixelitor.menus.view.ShowHideToolsAction;

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
import java.util.function.Predicate;
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
    private static final String UI_KEY = "ui";
    private static Dimension newImageSize = null;

    private static final String RECENT_FILE_PREFS_KEY = "recent_file_";

    private static final Preferences mainNode = Preferences.userNodeForPackage(Pixelitor.class);
    private static final Preferences recentFilesNode = Preferences.userNodeForPackage(RecentFilesMenu.class);

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

        mainNode.putInt(FRAME_X_KEY, x);
        mainNode.putInt(FRAME_Y_KEY, y);
        mainNode.putInt(FRAME_WIDTH_KEY, width);
        mainNode.putInt(FRAME_HEIGHT_KEY, height);
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
        Dimension desktopSize = ImageArea.getSize();
        if (desktopSize != null) {
            defaultWidth = Math.max(600, desktopSize.width - 30);
            defaultHeight = Math.max(400, desktopSize.height - 50);
        }
        int width = mainNode.getInt(NEW_IMAGE_WIDTH, defaultWidth);
        int height = mainNode.getInt(NEW_IMAGE_HEIGHT, defaultHeight);
        newImageSize = new Dimension(width, height);
    }

    private static void saveNewImageSize() {
        Dimension lastNew = NewImage.getLastNew();
        if (lastNew != null) {
            mainNode.putInt(NEW_IMAGE_WIDTH, lastNew.width);
            mainNode.putInt(NEW_IMAGE_HEIGHT, lastNew.height);
        }
    }

    public static void loadFramePosition(Window window) {
        int x = mainNode.getInt(FRAME_X_KEY, 0);
        int y = mainNode.getInt(FRAME_Y_KEY, 0);
        int width = mainNode.getInt(FRAME_WIDTH_KEY, 0);
        int height = mainNode.getInt(FRAME_HEIGHT_KEY, 0);

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        if ((width <= 0) || (height <= 0)) {
            width = screen.width;
            height = screen.height;
        }
        if (width > screen.width) {
            width = screen.width;
        }
        if (height > screen.height) {
            height = screen.height;
        }

        if ((x < 0) || (y < 0)) {
            x = 0;
            y = 0;
        }

        window.setLocation(new Point(x, y));
        window.setSize(width, height);
    }

    public static BoundedUniqueList<RecentFile> loadRecentFiles() {
        BoundedUniqueList<RecentFile> retVal = new BoundedUniqueList<>(RecentFilesMenu.MAX_RECENT_FILES);
        for (int i = 0; i < RecentFilesMenu.MAX_RECENT_FILES; i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String fileName = recentFilesNode.get(key, null);
            if (fileName == null) {
                break;
            }
            File file = new File(fileName);

            if (file.exists()) {
                RecentFile recentFile = new RecentFile(file);
                retVal.addIfNotThere(recentFile);
            }
        }
        return retVal;
    }

    private static void saveRecentFiles(BoundedUniqueList<RecentFile> recentFiles) {
        for (int i = 0; i < recentFiles.size(); i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String value = recentFiles.get(i).getSavedName();
            recentFilesNode.put(key, value);
        }
    }

    public static void removeRecentFiles() {
        for (int i = 0; i < RecentFilesMenu.MAX_RECENT_FILES; i++) {
            recentFilesNode.remove(RECENT_FILE_PREFS_KEY + i);
        }
    }

    public static File loadLastOpenDir() {
        return loadDir(LAST_OPEN_DIR_KEY);
    }

    public static File loadLastSaveDir() {
        return loadDir(LAST_SAVE_DIR_KEY);
    }

    private static File loadDir(String key) {
        String s = mainNode.get(key, null);

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
        saveDir(Directories.getLastOpenDir(), LAST_OPEN_DIR_KEY);
    }

    private static void saveLastSaveDir() {
        saveDir(Directories.getLastSaveDir(), LAST_SAVE_DIR_KEY);
    }

    private static void saveDir(File f, String key) {
        if (f != null) {
            mainNode.put(key, f.getAbsolutePath());
        } else {
            mainNode.put(key, null);
        }
    }

    public static int loadUndoLevels() {
        int retVal = mainNode.getInt(UNDO_LEVELS_KEY, -1);
        if (retVal == -1) {
            return Math.min(5, getDefaultUndoLevels());
        }
        return retVal;
    }

    private static void saveUndoLevels() {
        mainNode.putInt(UNDO_LEVELS_KEY, History.getUndoLevels());
    }

    public static int loadThumbSize() {
        return mainNode.getInt(THUMB_SIZE_KEY, LayerButtonLayout.SMALL_THUMB_SIZE);
    }

    private static void saveThumbSize() {
        mainNode.putInt(THUMB_SIZE_KEY, LayerButtonLayout.getThumbSize());
    }

    public static void savePrefsAndExit() {
        savePreferencesBeforeExit();
        System.exit(0);
    }

    private static void savePreferencesBeforeExit() {
        saveDesktopMode();
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
        int fgInt = mainNode.getInt(FG_COLOR_KEY, 0xFF000000);
        return new Color(fgInt);
    }

    public static Color loadBgColor() {
        int bgInt = mainNode.getInt(BG_COLOR_KEY, 0xFFFFFFFF);
        return new Color(bgInt);
    }

    private static void saveFgBgColors() {
        Color fgColor = FgBgColors.getFG();
        if (fgColor != null) {
            mainNode.putInt(FG_COLOR_KEY, fgColor.getRGB());
        }

        Color bgColor = FgBgColors.getBG();
        if (bgColor != null) {
            mainNode.putInt(BG_COLOR_KEY, bgColor.getRGB());
        }
    }

    public static Preferences getMainNode() {
        return mainNode;
    }

    private static int getDefaultUndoLevels() {
        int sizeInMegaBytes = Utils.getMaxHeapInMegabytes();
        int retVal = 1 + (sizeInMegaBytes / 50);

        // rounds up to the nearest multiple of 5
        return ((retVal + 4) / 5) * 5;
    }

    public static ImageArea.Mode loadDesktopMode() {
        String value = mainNode.get(UI_KEY, "Tabs");
        return ImageArea.Mode.fromString(value);
    }

    private static void saveDesktopMode() {
        mainNode.put(UI_KEY, ImageArea.getMode().toString());
    }

    /**
     * Static utility methods for managing the visibility of
     * various UI areas
     */
    public static class WorkSpace {
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
            histogramsVisibility = mainNode.getBoolean(HISTOGRAMS_SHOWN_KEY, DEFAULT_HISTOGRAMS_VISIBILITY);
            toolsVisibility = mainNode.getBoolean(TOOLS_SHOWN_KEY, DEFAULT_TOOLS_VISIBILITY);
            layersVisibility = mainNode.getBoolean(LAYERS_SHOWN_KEY, DEFAULT_LAYERS_VISIBILITY);
            statusBarVisibility = mainNode.getBoolean(STATUS_BAR_SHOWN_KEY, DEFAULT_STATUS_BAR_VISIBILITY);
            loaded = true;
        }

        public static void resetDefaults(PixelitorWindow pw) {
            pw.setHistogramsVisibility(DEFAULT_HISTOGRAMS_VISIBILITY, false);
            pw.setToolsVisibility(DEFAULT_TOOLS_VISIBILITY, false);
            pw.setLayersVisibility(DEFAULT_LAYERS_VISIBILITY, false);
            pw.setStatusBarVisibility(DEFAULT_STATUS_BAR_VISIBILITY, false);

            pw.getContentPane().revalidate();

            histogramsVisibility = DEFAULT_HISTOGRAMS_VISIBILITY;
            toolsVisibility = DEFAULT_TOOLS_VISIBILITY;
            layersVisibility = DEFAULT_LAYERS_VISIBILITY;
            statusBarVisibility = DEFAULT_STATUS_BAR_VISIBILITY;

            ShowHideHistogramsAction.INSTANCE.updateName(DEFAULT_HISTOGRAMS_VISIBILITY);
            ShowHideToolsAction.INSTANCE.updateName(DEFAULT_TOOLS_VISIBILITY);
            ShowHideLayersAction.INSTANCE.updateName(DEFAULT_LAYERS_VISIBILITY);
            ShowHideStatusBarAction.INSTANCE.updateName(DEFAULT_STATUS_BAR_VISIBILITY);
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
            mainNode.putBoolean(HISTOGRAMS_SHOWN_KEY, histogramsVisibility);
            mainNode.putBoolean(LAYERS_SHOWN_KEY, layersVisibility);
            mainNode.putBoolean(TOOLS_SHOWN_KEY, toolsVisibility);
            mainNode.putBoolean(STATUS_BAR_SHOWN_KEY, statusBarVisibility);
        }

        public static void setLayersVisibility(boolean v) {
            layersVisibility = v;
            PixelitorWindow.getInstance().setLayersVisibility(v, true);
        }

        public static void setHistogramsVisibility(boolean v) {
            histogramsVisibility = v;
            PixelitorWindow.getInstance().setHistogramsVisibility(v, true);
        }

        public static void setToolsVisibility(boolean v) {
            toolsVisibility = v;
            PixelitorWindow.getInstance().setToolsVisibility(v, true);
        }

        public static void setStatusBarVisibility(boolean v) {
            statusBarVisibility = v;
            PixelitorWindow.getInstance().setStatusBarVisibility(v, true);
        }
    }

    /**
     * The GUI for the preferences dialog
     */
    public static class Panel extends JPanel {
        private final JTextField undoLevelsTF;
        private final JComboBox<Value> thumbSizeCB;

        Panel() {
            setLayout(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(this);

            JComboBox uiChooser = new JComboBox(ImageArea.Mode.values());
            uiChooser.setSelectedItem(ImageArea.getMode());
            gbh.addLabelWithControl("Images In: ", uiChooser);
            uiChooser.addActionListener(e -> {
                ImageArea.Mode mode = (ImageArea.Mode) uiChooser.getSelectedItem();
                ImageArea.changeUI(mode);
            });

            undoLevelsTF = new JTextField(3);
            undoLevelsTF.setText(String.valueOf(History.getUndoLevels()));
            gbh.addLabelWithControl("Undo/Redo Levels: ",
                    TextFieldValidator.createIntOnlyLayerFor(undoLevelsTF));

            Value[] thumbSizes = {
                    new Value("24x24 pixels", 24),
                    new Value("48x48 pixels", 48),
                    new Value("72x72 pixels", 72),
                    new Value("96x96 pixels", 96),
            };
            thumbSizeCB = new JComboBox<>(thumbSizes);

            int currentSize = LayerButtonLayout.getThumbSize();
            thumbSizeCB.setSelectedIndex(currentSize / 24 - 1);

            gbh.addLabelWithControl("Layer/Mask Thumb Sizes: ", thumbSizeCB);
            thumbSizeCB.addActionListener(e -> updateThumbSize());
        }

        private int getUndoLevels() {
            String s = undoLevelsTF.getText();
            return Integer.parseInt(s);
        }

        private void updateThumbSize() {
            int newSize = ((Value) thumbSizeCB.getSelectedItem()).getValue();
            LayerButtonLayout.setThumbSize(newSize);
        }

        public static void showInDialog() {
            Panel panel = new Panel();

            // we don't want to continuously set the undo levels
            // as the user edits the text field, because low levels
            // erase the history, so we set it in the validator
            Predicate<JDialog> validator = d -> {
                int undoLevels = 0;
                boolean couldParse = true;
                try {
                    undoLevels = panel.getUndoLevels();
                } catch (NumberFormatException ex) {
                    couldParse = false;
                }
                if (couldParse) {
                    History.setUndoLevels(undoLevels);
                    return true;
                } else {
                    Dialogs.showErrorDialog(d, "Error",
                            "<html>The <b>Undo/Redo Levels</b> must be an integer.");
                    return false;
                }
            };

            new DialogBuilder()
                    .form(panel)
                    .noCancelButton()
                    .title("Preferences")
                    .okText("Close")
                    .validator(validator)
                    .validateWhenCanceled()
                    .show();
        }
    }
}
