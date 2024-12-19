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

package pixelitor.utils;

import com.bric.util.JVM;
import pixelitor.Features;
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
import pixelitor.io.FileChoosers;
import pixelitor.io.FileFormat;
import pixelitor.layers.LayerGUILayout;
import pixelitor.menus.file.BoundedUniqueList;
import pixelitor.menus.file.RecentFileEntry;
import pixelitor.menus.file.RecentFilesMenu;
import pixelitor.tools.BrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.util.Locale;
import java.util.prefs.Preferences;

import static javax.swing.SwingConstants.BOTTOM;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.RIGHT;
import static javax.swing.SwingConstants.TOP;
import static pixelitor.gui.ImageArea.Mode.FRAMES;
import static pixelitor.gui.ImageArea.Mode.TABS;
import static pixelitor.menus.file.RecentFilesMenu.MAX_RECENT_FILES;

/**
 * Static methods for saving and loading application preferences.
 */
public final class AppPreferences {
    public static final Preferences mainPrefs
        = Preferences.userNodeForPackage(Pixelitor.class);
    private static final Preferences recentFilesPrefs
        = Preferences.userNodeForPackage(RecentFilesMenu.class);
    private static final Preferences toolsPrefs
        = Preferences.userNodeForPackage(Tool.class);

    private static final String FRAME_X_KEY = "window_x";
    private static final String FRAME_Y_KEY = "window_y";
    private static final String FRAME_WIDTH_KEY = "window_width";
    private static final String FRAME_HEIGHT_KEY = "window_height";
    private static final String MAXIMIZED_KEY = "maximized";
    private static final int MIN_WINDOW_WIDTH = 300;
    private static final int MIN_WINDOW_HEIGHT = 200;

    private static final String NEW_IMAGE_WIDTH = "new_image_width";
    private static final String NEW_IMAGE_HEIGHT = "new_image_height";
    private static Dimension newImageSize = null;

    private static final String UI_KEY = "ui";
    private static final String NATIVE_CHOOSERS_KEY = "native_choosers";

    private static final String RECENT_FILE_PREFS_KEY = "recent_file_";

    private static final String FG_COLOR_KEY = "fg_color";
    private static final String BG_COLOR_KEY = "bg_color";

    private static final String LAST_OPEN_DIR_KEY = "last_open_dir";
    private static final String LAST_SAVE_DIR_KEY = "last_save_dir";
    private static final String LAST_SAVE_FORMAT_KEY = "last_save_fmt";

    private static final String UNDO_LEVELS_KEY = "undo_levels";
    private static final String THUMB_SIZE_KEY = "thumb_size";
    private static final String LAST_TOOL_KEY = "last_tool";
    private static final String THEME_KEY = "theme";
    private static final String LANG_KEY = "lang";
    private static final String MOUSE_ZOOM_KEY = "mouse_zoom";
    private static final String PAN_KEY = "pan";

    private static final String GUIDE_COLOR_KEY = "guide_color";
    private static final String GUIDE_STROKE_KEY = "guide_stroke";
    private static final String CROP_GUIDE_COLOR_KEY = "crop_guide_color";
    private static final String CROP_GUIDE_STROKE_KEY = "crop_guide_stroke";

    private static GuideStyle guideStyle;
    private static GuideStyle cropGuideStyle;

    private static final String MAGICK_DIR_KEY = "magick_dir";
    private static final String GMIC_DIR_KEY = "gmic_dir";
    private static final String EXPERIMENTAL_KEY = "experimental";

    private static final String UI_FONT_SIZE_KEY = "ui_font_size";
    private static final String UI_FONT_TYPE_KEY = "ui_font_type";

    // each bit of the "flags" represents a boolean flag in the app
    private static final String FLAGS_KEY = "flags";
    private static long flags = 0;

    // binary masks for the flags
    public static final long FLAG_PIXEL_SNAP = 1L;
    // subsequent flag masks would be 1L << 1, 1L << 2, etc.

    // the default settings for the flags (binary OR between
    // the masks of the flags that are true by default)
    private static final long FLAG_DEFAULTS = 0;

    // loaded and stored here to avoid initializing the ImageMagick class
    // (which also searches for this directory), if ImageMagick is not needed
    public static String magickDirName = "";
    public static String gmicDirName = "";

    static {
        loadPaths();
        loadFlags();
    }

    private AppPreferences() {
    }

    public static void loadFramePreferences(PixelitorWindow pw, Dimension screen) {
        int x = mainPrefs.getInt(FRAME_X_KEY, 0);
        int y = mainPrefs.getInt(FRAME_Y_KEY, 0);
        int width = mainPrefs.getInt(FRAME_WIDTH_KEY, 0);
        int height = mainPrefs.getInt(FRAME_HEIGHT_KEY, 0);

        if (width <= 0 || height <= 0) {
            width = screen.width;
            height = screen.height;
        }

        if (!Screens.isMultiMonitorSetup()) {
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

        if (width < MIN_WINDOW_WIDTH) { // something went wrong
            width = MIN_WINDOW_WIDTH;
        }
        if (height < MIN_WINDOW_HEIGHT) { // something went wrong
            height = MIN_WINDOW_HEIGHT;
        }

        boolean maximized = mainPrefs.getBoolean(MAXIMIZED_KEY, false);
        if (maximized && shouldSaveMaximizedState()) {
            pw.setSavedNormalBounds(new Rectangle(x, y, width, height));
            pw.maximize();
        } else {
            pw.setBounds(x, y, width, height);
        }
    }

    private static boolean shouldSaveMaximizedState() {
        // With multiple monitors it would maximize to the primary one
        // even if it has saved coordinates in another one
        // The active screen index or GraphicsDevice.getIDstring()
        // could be also saved, but it seems error-prone...
        return JVM.isWindows && !Screens.isMultiMonitorSetup();
    }

    private static void saveFramePreferences(PixelitorWindow pw) {
        boolean maximized = pw.isMaximized();
        Rectangle bounds;
        if (maximized && shouldSaveMaximizedState()) {
            bounds = pw.getNormalBounds();
            if (bounds == null) { // Fallback for safety. Should not be necessary.
                bounds = pw.getBounds();
            }
        } else {
            bounds = pw.getBounds();
        }

        mainPrefs.putInt(FRAME_X_KEY, bounds.x);
        mainPrefs.putInt(FRAME_Y_KEY, bounds.y);
        mainPrefs.putInt(FRAME_WIDTH_KEY, bounds.width);
        mainPrefs.putInt(FRAME_HEIGHT_KEY, bounds.height);

        mainPrefs.putBoolean(MAXIMIZED_KEY, maximized);
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
            // make sure the default new image fits at 100% zoom
            defaultWidth = Math.max(defaultWidth, desktopSize.width - 30);
            defaultHeight = Math.max(defaultHeight, desktopSize.height - 50);
        }
        int width = mainPrefs.getInt(NEW_IMAGE_WIDTH, defaultWidth);
        int height = mainPrefs.getInt(NEW_IMAGE_HEIGHT, defaultHeight);
        newImageSize = new Dimension(width, height);
    }

    private static void saveNewImageSize() {
        if (NewImage.lastSize != null) {
            mainPrefs.putInt(NEW_IMAGE_WIDTH, NewImage.lastSize.width);
            mainPrefs.putInt(NEW_IMAGE_HEIGHT, NewImage.lastSize.height);
        }
    }

    public static BoundedUniqueList<RecentFileEntry> loadRecentFiles() {
        var retVal = new BoundedUniqueList<RecentFileEntry>(MAX_RECENT_FILES);
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String fileName = recentFilesPrefs.get(key, null);
            if (fileName == null) {
                break;
            }
            File file = new File(fileName);

            if (file.exists()) {
                retVal.addIfAbsent(new RecentFileEntry(file));
            }
        }
        return retVal;
    }

    private static void saveRecentFiles(BoundedUniqueList<RecentFileEntry> recentFiles) {
        for (int i = 0; i < recentFiles.size(); i++) {
            String key = RECENT_FILE_PREFS_KEY + i;
            String filePath = recentFiles.get(i).getFullPath();
            recentFilesPrefs.put(key, filePath);
        }
    }

    public static void removeRecentFiles() {
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            recentFilesPrefs.remove(RECENT_FILE_PREFS_KEY + i);
        }
    }

    public static File loadLastOpenDir() {
        return loadDir(LAST_OPEN_DIR_KEY);
    }

    public static File loadLastSaveDir() {
        return loadDir(LAST_SAVE_DIR_KEY);
    }

    private static File loadDir(String key) {
        String path = mainPrefs.get(key, null);
        if (path == null) {
            return getDocumentsDir();
        }

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
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
            mainPrefs.put(key, f.getAbsolutePath());
        } else {
            mainPrefs.put(key, null);
        }
    }

    public static FileFormat loadLastSaveFormat() {
        String name = mainPrefs.get(LAST_SAVE_FORMAT_KEY, null);
        if (name == null) {
            return FileFormat.JPG;
        }
        FileFormat fileFormat;
        try {
            fileFormat = FileFormat.valueOf(name.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            // can happen only if the preferences were manually edited
            return FileFormat.JPG;
        }
        return fileFormat;
    }

    private static void saveLastSaveFormat() {
        FileFormat lastOutput = FileFormat.getLastSaved();
        mainPrefs.put(LAST_SAVE_FORMAT_KEY, lastOutput.toString());
    }

    public static int loadUndoLevels() {
        int retVal = mainPrefs.getInt(UNDO_LEVELS_KEY, -1);
        if (retVal == -1) {
            return Math.min(5, calcDefaultUndoLevels());
        }
        return retVal;
    }

    private static void saveUndoLevels() {
        mainPrefs.putInt(UNDO_LEVELS_KEY, History.getUndoLevels());
    }

    public static int loadThumbSize() {
        return mainPrefs.getInt(THUMB_SIZE_KEY, LayerGUILayout.SMALL_THUMB_SIZE);
    }

    private static void saveThumbSize() {
        mainPrefs.putInt(THUMB_SIZE_KEY, LayerGUILayout.getThumbSize());
    }

    public static GuideStyle getGuideStyle() {
        if (guideStyle == null) {
            int colorRGB = mainPrefs.getInt(GUIDE_COLOR_KEY, Color.BLACK.getRGB());
            int strokeId = mainPrefs.getInt(GUIDE_STROKE_KEY, GuideStrokeType.DASHED.ordinal());
            guideStyle = new GuideStyle();
            guideStyle.setColorA(new Color(colorRGB));
            guideStyle.setStrokeType(GuideStrokeType.values()[strokeId]);
        }

        return guideStyle;
    }

    public static GuideStyle getCropGuideStyle() {
        if (cropGuideStyle == null) {
            int colorRGB = mainPrefs.getInt(CROP_GUIDE_COLOR_KEY, Color.BLACK.getRGB());
            int strokeId = mainPrefs.getInt(CROP_GUIDE_STROKE_KEY, GuideStrokeType.SOLID.ordinal());
            cropGuideStyle = new GuideStyle();
            cropGuideStyle.setColorA(new Color(colorRGB));
            cropGuideStyle.setStrokeType(GuideStrokeType.values()[strokeId]);
        }

        return cropGuideStyle;
    }

    private static void saveGuideStyles() {
        GuideStyle style = getGuideStyle();
        mainPrefs.putInt(GUIDE_COLOR_KEY, style.getColorA().getRGB());
        mainPrefs.putInt(GUIDE_STROKE_KEY, style.getStrokeType().ordinal());
    }

    private static void saveCropGuideStyles() {
        GuideStyle style = getCropGuideStyle();
        mainPrefs.putInt(CROP_GUIDE_COLOR_KEY, style.getColorA().getRGB());
        mainPrefs.putInt(CROP_GUIDE_STROKE_KEY, style.getStrokeType().ordinal());
    }

    public static void savePreferences() {
        saveDesktopMode();
        saveRecentFiles(RecentFilesMenu.INSTANCE.getRecentFileEntries());
        saveFramePreferences(PixelitorWindow.get());
        saveLastOpenDir();
        saveLastSaveDir();
        saveLastSaveFormat();
        saveFgBgColors();
        PixelitorWindow.get().getWorkSpace().savePreferences();
        saveUndoLevels();
        saveThumbSize();
        TipsOfTheDay.saveNextTipIndex();
        saveNewImageSize();
        saveLastToolName();
        saveGuideStyles();
        saveCropGuideStyles();
        saveTheme();
        saveUIFont();
        saveLanguage();
        saveMouseZoom();
        savePan();
        savePaths();
        saveFlags();
        saveExperimentalFeatures();
        saveNativeChoosers();
    }

    public static Color loadFgColor() {
        int fgInt = mainPrefs.getInt(FG_COLOR_KEY, 0xFF_00_00_00);
        return new Color(fgInt);
    }

    public static Color loadBgColor() {
        int bgInt = mainPrefs.getInt(BG_COLOR_KEY, 0xFF_FF_FF_FF);
        return new Color(bgInt);
    }

    private static void saveFgBgColors() {
        Color fgColor = FgBgColors.getActualFgColor();
        if (fgColor != null) {
            mainPrefs.putInt(FG_COLOR_KEY, fgColor.getRGB());
        }

        Color bgColor = FgBgColors.getActualBgColor();
        if (bgColor != null) {
            mainPrefs.putInt(BG_COLOR_KEY, bgColor.getRGB());
        }
    }

    public static Preferences getMainPrefs() {
        return mainPrefs;
    }

    private static int calcDefaultUndoLevels() {
        int sizeInMegaBytes = MemoryInfo.getMaxHeapMb();
        int retVal = 1 + sizeInMegaBytes / 50;

        // rounds up to the nearest multiple of 5
        return ((retVal + 4) / 5) * 5;
    }

    public static ImageAreaConfig loadDesktopMode() {
        String value = mainPrefs.get(UI_KEY, "TabsN");
        if (value.startsWith("Tabs")) {
            return loadSavedTabsInfo(value);
        } else {
            // return TOP tab placement so that if the user
            // changes the UI via preferences, this will be set
            return new ImageAreaConfig(FRAMES, TOP);
        }
    }

    private static ImageAreaConfig loadSavedTabsInfo(String value) {
        int tabPlacement = switch (value) {
            case "TabsL" -> LEFT;
            case "TabsR" -> RIGHT;
            case "TabsB" -> BOTTOM;
            default -> TOP;
        };
        return new ImageAreaConfig(TABS, tabPlacement);
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
        mainPrefs.put(UI_KEY, savedString);
    }

    public static String loadLastToolName() {
        return toolsPrefs.get(LAST_TOOL_KEY, BrushTool.NAME);
    }

    private static void saveLastToolName() {
        toolsPrefs.put(LAST_TOOL_KEY, Tools.getActive().getShortName());
    }

    public static Theme loadTheme() {
        String code = mainPrefs.get(THEME_KEY, Themes.DEFAULT.getSaveCode());
        for (Theme theme : Theme.values()) {
            if (code.equals(theme.getSaveCode())) {
                return theme;
            }
        }
        return Themes.DEFAULT;
    }

    private static void saveTheme() {
        mainPrefs.put(THEME_KEY, Themes.getActive().getSaveCode());
    }

    public static int loadUIFontSize() {
        return mainPrefs.getInt(UI_FONT_SIZE_KEY, 0);
    }

    public static String loadUIFontType() {
        return mainPrefs.get(UI_FONT_TYPE_KEY, "");
    }

    private static void saveUIFont() {
        Font font = UIManager.getFont("defaultFont");
        String type;
        int size;
        if (font == null) {
            type = "";
            size = 0;
        } else {
            type = font.getName();
            size = font.getSize();
        }

        mainPrefs.putInt(UI_FONT_SIZE_KEY, size);
        mainPrefs.put(UI_FONT_TYPE_KEY, type);
    }

    public static String loadLanguageCode() {
        return mainPrefs.get(LANG_KEY, "en");
    }

    private static void saveLanguage() {
        mainPrefs.put(LANG_KEY, Language.getActive().getCode());
    }

    public static String loadMouseZoom() {
        return mainPrefs.get(MOUSE_ZOOM_KEY, MouseZoomMethod.WHEEL.saveCode());
    }

    private static void saveMouseZoom() {
        mainPrefs.put(MOUSE_ZOOM_KEY, MouseZoomMethod.ACTIVE.saveCode());
    }

    public static String loadPan() {
        return mainPrefs.get(PAN_KEY, PanMethod.SPACE_DRAG.saveCode());
    }

    private static void savePan() {
        mainPrefs.put(PAN_KEY, PanMethod.ACTIVE.saveCode());
    }

    private static void loadPaths() {
        magickDirName = mainPrefs.get(MAGICK_DIR_KEY, "");
        gmicDirName = mainPrefs.get(GMIC_DIR_KEY, "");
    }

    private static void savePaths() {
        mainPrefs.put(MAGICK_DIR_KEY, magickDirName);
        mainPrefs.put(GMIC_DIR_KEY, gmicDirName);
    }

    private static void loadFlags() {
        flags = mainPrefs.getLong(FLAGS_KEY, FLAG_DEFAULTS);
    }

    private static void saveFlags() {
        mainPrefs.putLong(FLAGS_KEY, flags);
    }

    public static boolean getFlag(long mask) {
        return (flags & mask) != 0;
    }

    public static void setFlag(long mask, boolean newValue) {
        flags = newValue ? (flags | mask) : (flags & ~mask);
    }

    public static boolean loadExperimentalFeatures() {
        return mainPrefs.getBoolean(EXPERIMENTAL_KEY, false);
    }

    private static void saveExperimentalFeatures() {
        mainPrefs.putBoolean(EXPERIMENTAL_KEY, Features.enableExperimental);
    }

    public static boolean loadNativeChoosers() {
        return mainPrefs.getBoolean(NATIVE_CHOOSERS_KEY, false);
    }

    private static void saveNativeChoosers() {
        mainPrefs.putBoolean(NATIVE_CHOOSERS_KEY, FileChoosers.useNativeDialogs());
    }
}
