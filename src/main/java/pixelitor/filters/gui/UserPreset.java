/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import com.bric.util.JVM;
import pixelitor.AppMode;
import pixelitor.colors.Colors;
import pixelitor.filters.Truchet;
import pixelitor.gui.utils.TaskAction;
import pixelitor.io.FileUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Usually represents a user-created preset that stores configuration
 * settings in a flat text file. In some cases it can also represent
 * a built-in (hardcoded) preset.
 * <p>
 * Unlike {@link FilterState}, {@link UserPreset} is optimized
 * for saving to and loading from text files.
 */
public class UserPreset implements Preset {
    private final String name;
    private File file; // can be null for new or built-in presets
    private final String directoryName; // subdirectory for this type of preset
    private boolean contentLoaded; // whether the preset content is loaded in memory
    private final Map<String, String> content = new LinkedHashMap<>();

    public static final String PRESETS_DIR = initPresetsDirectory();

    private static String initPresetsDirectory() {
        String baseDir = null;

        if (JVM.isWindows) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                baseDir = appData + File.separator + "Pixelitor";
            }
        }

        if (baseDir == null) { // Linux, macOS, and Windows with missing APPDATA
            baseDir = System.getProperty("user.home") + File.separator + ".pixelitor";
        }

        return baseDir + File.separator + "presets";
    }

    /**
     * Creates a preset for a built-in configuration.
     * This preset is not associated with a file.
     */
    public UserPreset(String name) {
        this(name, null);
    }

    /**
     * Creates a new preset from user settings, ready to be saved.
     */
    public UserPreset(String name, String directoryName) {
        this.name = name;
        this.directoryName = directoryName;
        contentLoaded = true;
    }

    /**
     * Creates a preset object for an existing preset file on disk.
     * The preset's content is not loaded until it's first accessed.
     */
    public UserPreset(File file, String directoryName) {
        this.name = FileUtils.stripExtension(file.getName());
        this.file = file;
        this.directoryName = directoryName;
        contentLoaded = false;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the setting value for a given key.
     */
    public String get(String key) {
        String value = content.get(key);
        if (value == null) {
            // legacy migration support
            if ("Ray Colors".equals(key)) {
                // oct 2021: temporary hack for compatible color list upgrade of starburst
                value = content.get("Ray Color");
            } else if ("Sides".equals(key)) {
                // sept 2023: 4.3.0 => 4.3.1 migration in Concentric Shapes
                value = content.get("Polygon Sides");
            }

            if (AppMode.isDevelopment()) {
                System.out.println("UserPreset::get: no value found for the key " + key);
            }
        } else {
            // sept 2023: migration in Truchet Tiles
            if (Truchet.migration_helper.containsKey(value)) {
                value = Truchet.migration_helper.get(value);
            }
        }

        return value;
    }

    /**
     * Stores a setting with the given key and value.
     */
    public void put(String key, String value) {
        assert !key.isBlank();

        content.put(key, value);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putInt(String key, int value) {
        put(key, String.valueOf(value));
    }

    public void putLong(String key, long value) {
        put(key, String.valueOf(value));
    }

    public boolean getBoolean(String key) {
        return "yes".equalsIgnoreCase(get(key));
    }

    public void putBoolean(String key, boolean b) {
        put(key, b ? "yes" : "no");
    }

    public float getFloat(String key) {
        return Float.parseFloat(get(key));
    }

    public float getFloat(String key, float defaultValue) {
        String value = get(key);
        try {
            return (value != null) ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putFloat(String key, float f) {
        put(key, String.format(Locale.ROOT, "%.4f", f));
    }

    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public double getDouble(String key, double defaultValue) {
        String value = get(key);
        try {
            return (value != null) ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putDouble(String key, double d) {
        put(key, String.format(Locale.ROOT, "%.4f", d));
    }

    public Color getColor(String key) {
        return getColor(key, Color.BLACK);
    }

    public Color getColor(String key, Color defaultValue) {
        String color = get(key);
        try {
            return (color != null) ? Colors.fromHtmlHexRgba(color) : defaultValue;
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public void putColor(String key, Color c) {
        put(key, Colors.toHtmlHex(c, true));
    }

    /**
     * Finds an enum constant by matching its toString() value.
     */
    public <T extends Enum<T>> T getEnumByToString(String key, Class<T> clazz) {
        return getEnumHelper(key, clazz, T::toString);
    }

    /**
     * Finds an enum constant by matching its name() value. This method
     * should be preferred to {@link #getEnumByToString(String, Class<T>)}
     * if the enum's toString() is localized.
     */
    public <T extends Enum<T>> T getEnum(String key, Class<T> clazz) {
        return getEnumHelper(key, clazz, Enum::name);
    }

    private <T extends Enum<T>> T getEnumHelper(String key, Class<T> clazz,
                                                Function<T, String> valueExtractor) {
        String storedValue = get(key);
        T[] enumConstants = clazz.getEnumConstants();

        if (storedValue != null) {
            for (T constant : enumConstants) {
                if (valueExtractor.apply(constant).equals(storedValue)) {
                    return constant;
                }
            }
        }

        // default to the first enum constant if no match is found
        return enumConstants[0];
    }

    //  uses a simple key=value format instead of
    //  java.util.Properties to avoid escaping spaces in keys
    private void loadFromFile() throws IOException {
        assert !contentLoaded;

        try (InputStream input = new FileInputStream(file);
             Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {

            loadFromReader(br);
        }

        contentLoaded = true;
    }

    private void loadFromReader(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            int index = line.indexOf('=');
            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                content.put(key, value);
            }
        }
    }

    public void loadFromString(String s) {
        try {
            loadFromReader(new BufferedReader(new StringReader(s)));
        } catch (IOException e) {
            Messages.showException(e);
        }
    }

    /**
     * Saves the preset settings to disk in the appropriate directory.
     * Creates the directory if it doesn't exist.
     */
    public void save() {
        assert file == null;
        assert contentLoaded;

        File presetFile = getSaveFile(true);
        try (PrintWriter writer = new PrintWriter(presetFile, StandardCharsets.UTF_8)) {
            writeTo(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.file = presetFile;
        Messages.showStatusMessage("Preset saved to <b>" + presetFile.getAbsolutePath() + "</b>");
    }

    private void writeTo(PrintWriter writer) {
        for (Map.Entry<String, String> entry : content.entrySet()) {
            writer.println(entry.getKey() + "=" + entry.getValue());
        }
    }

    public String writeToString() {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        writeTo(printWriter);
        printWriter.flush();
        return writer.toString();
    }

    @Override
    public Action createAction(PresetOwner owner) {
        return new TaskAction(name, () -> {
            if (!contentLoaded) {
                try {
                    loadFromFile();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            owner.loadUserPreset(this);
        });
    }

    /**
     * Detects all presets in the given directory by listing files
     * ending in ".txt". The presets' contents are not loaded into memory.
     */
    public static List<UserPreset> detectPresetNames(String presetDirName) {
        File presetsDir = getSaveDir(presetDirName);
        if (!presetsDir.exists()) {
            return List.of();
        }

        String[] fileNames = presetsDir.list((dir, name) -> name.endsWith(".txt"));
        if (fileNames == null || fileNames.length == 0) {
            return List.of();
        }

        return Arrays.stream(fileNames)
            .sorted()
            .map(fileName -> new File(presetsDir, fileName))
            .map(file -> new UserPreset(file, presetDirName))
            .toList();
    }

    public boolean fileExists() {
        return getSaveFile(false).exists();
    }

    private File getSaveFile(boolean createDirs) {
        File dir = getSaveDir(directoryName);
        if (createDirs && !dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, name + ".txt");
    }

    private static File getSaveDir(String presetDirName) {
        return new File(PRESETS_DIR + File.separator + presetDirName);
    }

    @Override
    public String toString() {
        return name + " " + content;
    }
}
