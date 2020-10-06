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

package pixelitor.utils;

import com.bric.util.JVM;
import pixelitor.NewImage;
import pixelitor.Pixelitor;
import pixelitor.TipsOfTheDay;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.*;
import pixelitor.gui.utils.Screens;
import pixelitor.gui.utils.Theme;
import pixelitor.gui.utils.Themes;
import pixelitor.guides.GuideStrokeType;
import pixelitor.guides.GuideStyle;
import pixelitor.history.History;
import pixelitor.io.Dirs;
import pixelitor.layers.LayerButtonLayout;
import pixelitor.menus.file.RecentFile;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.tools.BrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.filechooser.FileSystemView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.util.prefs.Preferences;

import static javax.swing.SwingConstants.*;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.ImageArea.Mode.TABS;
import static pixelitor.menus.file.RecentFilesMenu.MAX_RECENT_FILES;

/**
 * Static methods for saving and loading application preferences
 */
public final class AppPreferences {
    private static final String FRAME_X_KEY = "window_x";
    private static final String FRAME_Y_KEY = "window_y";
    private static final String FRAME_WIDTH_KEY = "window_width";
    private static final String FRAME_HEIGHT_KEY = "window_height";

    private static final String MAXIMIZED_KEY = "maximized";

    private static final String NEW_IMAGE_WIDTH = "new_image_width";
    private static final String NEW_IMAGE_HEIGHT = "new_image_height";
    private static Dimension newImageSize = null;

    private static final String UI_KEY = "ui";

    private static final String RECENT_FILE_PREFS_KEY = "recent_file_";

    public static final Preferences mainNode
        = Preferences.userNodeForPackage(Pixelitor.class);
    private static final Preferences recentFilesNode
        = Preferences.userNodeForPackage(RecentFilesMenu.class);
    private static final Preferences toolsNode
        = Preferences.userNodeForPackage(Tool.class);

    private static final String FG_COLOR_KEY = "fg_color";
    private static final String BG_COLOR_KEY = "bg_color";

    private static final String LAST_OPEN_DIR_KEY = "last_open_dir";
    private static final String LAST_SAVE_DIR_KEY = "last_save_dir";

    private static final String UNDO_LEVELS_KEY = "undo_levels";
    private static final String THUMB_SIZE_KEY = "thumb_size";
    private static final String LAST_TOOL_KEY = "last_tool";
    private static final String THEME_KEY = "theme";
    private static final String LANG_KEY = "lang";
    private static final String MOUSE_ZOOM_KEY = "mouse_zoom";

    private static final String GUIDE_COLOR_KEY = "guide_color";
    private static final String GUIDE_STROKE_KEY = "guide_stroke";
    private static final String CROP_GUIDE_COLOR_KEY = "crop_guide_color";
    private static final String CROP_GUIDE_STROKE_KEY = "crop_guide_stroke";

    private static final int GUIDE_COLOR_DEFAULT = Color.BLACK.getRGB();
    private static final int GUIDE_STROKE_DEFAULT = GuideStrokeType.DASHED.ordinal();
    private static final int CROP_GUIDE_COLOR_DEFAULT = Color.BLACK.getRGB();
    private static final int CROP_GUIDE_STROKE_DEFAULT = GuideStrokeType.SOLID.ordinal();

    private static GuideStyle guideStyle;
    private static GuideStyle cropGuideStyle;

    private AppPreferences() {
    }

    public static void loadFramePosition(PixelitorWindow pw, Dimension screen) {
        int x = mainNode.getInt(FRAME_X_KEY, 0);
        int y = mainNode.getInt(FRAME_Y_KEY, 0);
        int width = mainNode.getInt(FRAME_WIDTH_KEY, 0);
        int height = mainNode.getInt(FRAME_HEIGHT_KEY, 0);

        if (width <= 0 || height <= 0) {
            width = screen.width;
            height = screen.height;
        }

        if (!Screens.hasMultipleMonitors()) {
            // if there are multiple monitors, then negative coordinates
            // are fine if there is an extended desktop, with the
            // main monitor on the right side
            if (x < 0 || y < 0) {
                x = 0;
                y = 0;
            }

            // if there are multiple monitors, then screen refers to the
            // primary monitor while the actual coordinates could be
            // for the secondary one
            if (width > screen.width) {
                width = screen.width;
            }
            if (height > screen.height) {
                height = screen.height;
            }
        }

        if (width < 300) { // something went wrong
            width = 300;
        }
        if (height < 200) { // something went wrong
            height = 200;
        }

        boolean maximized = mainNode.getBoolean(MAXIMIZED_KEY, false);
        if (maximized && saveMaximizedState()) {
            pw.setSavedNormalBounds(new Rectangle(x, y, width, height));
            pw.maximize();
        } else {
            pw.setBounds(x, y, width, height);
        }
    }

    private static boolean saveMaximizedState() {
        // With multiple monitors it would maximize to the primary one
        // even if it has saved coordinates in another one
        // The active screen index or GraphicsDevice.getIDstring()
        // could be also saved, but it seems error-prone...
        return JVM.isWindows && !Screens.hasMultipleMonitors();
    }

    private static void saveFramePosition(PixelitorWindow pw) {
        boolean maximized = pw.isMaximized();
        Rectangle bounds;
        if (maximized && saveMaximizedState()) {
            bounds = pw.getNormalBounds();
            if (bounds == null) { // Fallback for safety. Should not be necessary.
                bounds = pw.getBounds();
            }
        } else {
            bounds = pw.getBounds();
        }

        mainNode.putInt(FRAME_X_KEY, bounds.x);
        mainNode.putInt(FRAME_Y_KEY, bounds.y);
        mainNode.putInt(FRAME_WIDTH_KEY, bounds.width);
        mainNode.putInt(FRAME_HEIGHT_KEY, bounds.height);

        mainNode.putBoolean(MAXIMIZED_KEY, maximized);
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
        Dimension lastSize = NewImage.getLastSize();
        if (lastSize != null) {
            mainNode.putInt(NEW_IMAGE_WIDTH, lastSize.width);
            mainNode.putInt(NEW_IMAGE_HEIGHT, lastSize.height);
        }
    }

    public static BoundedUniqueList<RecentFile> loadRecentFiles() {
        var retVal = new BoundedUniqueList<RecentFile>(MAX_RECENT_FILES);
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String fileName = recentFilesNode.get(key, null);
            if (fileName == null) {
                break;
            }
            File file = new File(fileName);

            if (file.exists()) {
                retVal.addIfNotThere(new RecentFile(file));
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
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
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
        saveDir(Dirs.getLastOpen(), LAST_OPEN_DIR_KEY);
    }

    private static void saveLastSaveDir() {
        saveDir(Dirs.getLastSave(), LAST_SAVE_DIR_KEY);
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

    public static GuideStyle getGuideStyle() {
        if (guideStyle == null) {
            int colorRGB = mainNode.getInt(GUIDE_COLOR_KEY, GUIDE_COLOR_DEFAULT);
            int strokeId = mainNode.getInt(GUIDE_STROKE_KEY, GUIDE_STROKE_DEFAULT);
            guideStyle = new GuideStyle();
            guideStyle.setColorA(new Color(colorRGB));
            guideStyle.setStrokeType(GuideStrokeType.values()[strokeId]);
        }

        return guideStyle;
    }

    public static GuideStyle getCropGuideStyle() {
        if (cropGuideStyle == null) {
            int colorRGB = mainNode.getInt(CROP_GUIDE_COLOR_KEY, CROP_GUIDE_COLOR_DEFAULT);
            int strokeId = mainNode.getInt(CROP_GUIDE_STROKE_KEY, CROP_GUIDE_STROKE_DEFAULT);
            cropGuideStyle = new GuideStyle();
            cropGuideStyle.setColorA(new Color(colorRGB));
            cropGuideStyle.setStrokeType(GuideStrokeType.values()[strokeId]);
        }

        return cropGuideStyle;
    }

    private static void saveGuideStyles() {
        GuideStyle style = getGuideStyle();
        mainNode.putInt(GUIDE_COLOR_KEY, style.getColorA().getRGB());
        mainNode.putInt(GUIDE_STROKE_KEY, style.getStrokeType().ordinal());
    }

    private static void saveCropGuideStyles() {
        GuideStyle style = getCropGuideStyle();
        mainNode.putInt(CROP_GUIDE_COLOR_KEY, style.getColorA().getRGB());
        mainNode.putInt(CROP_GUIDE_STROKE_KEY, style.getStrokeType().ordinal());
    }

    public static void savePrefsAndExit() {
        savePreferences();
        System.exit(0);
    }

    private static void savePreferences() {
        saveDesktopMode();
        saveRecentFiles(RecentFilesMenu.getInstance().getRecentFileInfosForSaving());
        saveFramePosition(PixelitorWindow.get());
        saveLastOpenDir();
        saveLastSaveDir();
        saveFgBgColors();
        WorkSpace.saveVisibility();
        saveUndoLevels();
        saveThumbSize();
        TipsOfTheDay.saveNextTipNr();
        saveNewImageSize();
        saveLastToolName();
        saveGuideStyles();
        saveCropGuideStyles();
        saveTheme();
        saveLanguage();
        saveMouseZoom();
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
        Color fgColor = FgBgColors.getRealFgColor();
        if (fgColor != null) {
            mainNode.putInt(FG_COLOR_KEY, fgColor.getRGB());
        }

        Color bgColor = FgBgColors.getRealBgColor();
        if (bgColor != null) {
            mainNode.putInt(BG_COLOR_KEY, bgColor.getRGB());
        }
    }

    public static Preferences getMainNode() {
        return mainNode;
    }

    private static int getDefaultUndoLevels() {
        int sizeInMegaBytes = Utils.getMaxHeapInMegabytes();
        int retVal = 1 + sizeInMegaBytes / 50;

        // rounds up to the nearest multiple of 5
        return ((retVal + 4) / 5) * 5;
    }

    public static ImageAreaSavedInfo loadDesktopMode() {
        String value = mainNode.get(UI_KEY, "TabsN");
        if (value.startsWith("Tabs")) {
            return loadSavedTabsInfo(value);
        } else {
            // return TOP tab placement so that if the user
            // changes the UI via preferences, this will be set
            return new ImageAreaSavedInfo(FRAMES, TOP);
        }
    }

    private static ImageAreaSavedInfo loadSavedTabsInfo(String value) {
        int tabPlacement = switch (value) {
            case "TabsL" -> LEFT;
            case "TabsR" -> RIGHT;
            case "TabsB" -> BOTTOM;
            default -> TOP;
        };
        return new ImageAreaSavedInfo(TABS, tabPlacement);
    }

    private static void saveDesktopMode() {
        String savedString;
        ImageArea.Mode mode = ImageArea.getMode();
        if (mode == FRAMES) {
            savedString = "Frames";
        } else {
            int tabPlacement = ImageArea.getTabPlacement();
            savedString = switch (tabPlacement) {
                case TOP -> "TabsT";
                case LEFT -> "TabsL";
                case RIGHT -> "TabsR";
                case BOTTOM -> "TabsB";
                default -> throw new IllegalStateException("tabPlacement = " + tabPlacement);
            };
        }
        mainNode.put(UI_KEY, savedString);
    }

    public static String loadLastToolName() {
        return toolsNode.get(LAST_TOOL_KEY, BrushTool.NAME);
    }

    private static void saveLastToolName() {
        toolsNode.put(LAST_TOOL_KEY, Tools.getCurrent().getName());
    }

    public static Theme getDefaultTheme() {
        String code = mainNode.get(THEME_KEY, "Nimbus");
        Theme[] themes = Theme.values();
        for (Theme theme : themes) {
            if (code.equals(theme.getSaveCode())) {
                return theme;
            }
        }
        return Theme.NIMBUS;
    }

    private static void saveTheme() {
        mainNode.put(THEME_KEY, Themes.getCurrent().getSaveCode());
    }

    public static String loadLanguageCode() {
        return mainNode.get(LANG_KEY, "en");
    }

    private static void saveLanguage() {
        mainNode.put(LANG_KEY, Language.getCurrent().getCode());
    }

    public static String loadMouseZoom() {
        return mainNode.get(MOUSE_ZOOM_KEY, MouseZoomMethod.CTRL_WHEEL.saveCode());
    }

    private static void saveMouseZoom() {
        mainNode.put(MOUSE_ZOOM_KEY, MouseZoomMethod.CURRENT.saveCode());
    }
}
