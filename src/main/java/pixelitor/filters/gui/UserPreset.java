/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;
import pixelitor.gui.utils.PAction;
import pixelitor.io.FileUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a user-created preset for a filter.
 * It is similar to but different from {@link FilterState} because
 * its internal structure is more suitable for saving as a flat text file.
 */
public class UserPreset {
    private final String name;
    private File inFile;
    private final String presetDirName;
    private boolean loaded = false;
    private final Map<String, String> map = new LinkedHashMap<>();

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String PRESETS_DIR = System.getProperty("user.home") +
        FILE_SEPARATOR + ".pixelitor" + FILE_SEPARATOR + "presets";

    /**
     * Used when a new preset is created by the user
     */
    public UserPreset(String name, String presetDirName) {
        this.name = name;
        this.presetDirName = presetDirName;
        loaded = true;
    }

    /**
     * Used then the existence of a preset file is detected
     */
    public UserPreset(File inFile, String presetDirName) {
        this.name = FileUtils.stripExtension(inFile.getName());
        this.inFile = inFile;
        this.presetDirName = presetDirName;
        loaded = false;
    }

    public String getName() {
        return name;
    }

    public String get(String key) {
        String s = map.get(key);
        if (s == null) {
            System.out.println("UserPreset::get: no value found for the key " + key);
        }
        return s;
    }

    public void put(String key, String value) {
        assert !key.isBlank();

        map.put(key, value);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public void putInt(String key, int i) {
        put(key, String.valueOf(i));
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

    public void putFloat(String key, float f) {
        put(key, "%.4f".formatted(f));
    }

    public Color getColor(String key) {
        return Colors.fromHTMLHex(get(key));
    }

    public void putColor(String key, Color c) {
        put(key, Colors.toHTMLHex(c, true));
    }

    // not using Properties because it is ugly to escape the spaces in keys
    private void load() throws IOException {
        assert !loaded;
        InputStream input = new FileInputStream(inFile);
        Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf('=');
                if (index > 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    map.put(key, value);
                }
            }
        }
        loaded = true;
    }

    public void save() {
        assert inFile == null;
        assert loaded;

        File outFile = calcSaveFile(true);
        try (PrintWriter writer = new PrintWriter(outFile, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Messages.showInStatusBar("Preset saved to <b>" + outFile.getAbsolutePath() + "</b>");
    }

    public Action asAction(DialogMenuOwner owner) {
        return new PAction(name) {
            @Override
            public void onClick() {
                if (!loaded) {
                    try {
                        load();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
                owner.loadUserPreset(UserPreset.this);
            }
        };
    }

    @Override
    public String toString() {
        return name + " " + map.toString();
    }

    public static List<UserPreset> loadPresets(String presetDirName) {
        File presetsDir = calcSaveDir(presetDirName);
        if (!presetsDir.exists()) {
            return List.of();
        }

        String[] fileNames = presetsDir.list((dir, name) -> name.endsWith(".txt"));
        if (fileNames == null || fileNames.length == 0) {
            return List.of();
        }

        List<UserPreset> list = new ArrayList<>();
        for (String fileName : fileNames) {
            File presetFile = new File(presetsDir, fileName);
            list.add(new UserPreset(presetFile, presetDirName));
        }
        return list;
    }

    public boolean fileExists() {
        return calcSaveFile(false).exists();
    }

    private File calcSaveFile(boolean createDirs) {
        File dir = calcSaveDir(presetDirName);
        if (createDirs && !dir.exists()) {
            dir.mkdirs();
        }
        File outFile = new File(dir, name + ".txt");
        return outFile;
    }

    private static File calcSaveDir(String presetDirName) {
        return new File(PRESETS_DIR + FILE_SEPARATOR + presetDirName);
    }
}
