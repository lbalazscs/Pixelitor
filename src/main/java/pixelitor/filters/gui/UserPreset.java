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

package pixelitor.filters.gui;

import pixelitor.colors.Colors;
import pixelitor.io.FileUtils;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserPreset {
    private final String name;
    private File inFile;
    private final String filterName;
    private boolean loaded = false;
    private final Map<String, String> map = new LinkedHashMap<>();

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String PRESETS_DIR = System.getProperty("user.home") +
        FILE_SEPARATOR + ".pixelitor" + FILE_SEPARATOR + "presets";

    /**
     * Used when a new preset is created by the user
     */
    public UserPreset(String name, String filterName) {
        this.name = name;
        this.filterName = filterName;
        loaded = true;
    }

    /**
     * Used then the existence of a preset file is detected
     */
    public UserPreset(File inFile, String filterName) {
        this.name = FileUtils.stripExtension(inFile.getName());
        this.inFile = inFile;
        this.filterName = filterName;
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

    public float getFloat(String key) {
        return Float.parseFloat(get(key));
    }

    public void putFloat(String key, float f) {
        put(key, "%.2f".formatted(f));
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

        File dir = new File(PRESETS_DIR + FILE_SEPARATOR + filterName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outFile = new File(dir, name + ".txt");

        try (PrintWriter writer = new PrintWriter(outFile, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Messages.showInStatusBar("Preset saved to <b>" + outFile.getAbsolutePath() + "</b>");
    }

    public Action asAction(ParamSet paramSet) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!loaded) {
                    try {
                        load();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
                paramSet.loadPreset(UserPreset.this);
            }
        };
    }

    @Override
    public String toString() {
        return name + " " + map.toString();
    }

    public static List<UserPreset> loadPresets(String filterName) {
        File filterDir = new File(PRESETS_DIR + FILE_SEPARATOR + filterName);
        if (!filterDir.exists()) {
            return List.of();
        }

        String[] fileNames = filterDir.list((dir, name) -> name.endsWith(".txt"));
        if (fileNames == null || fileNames.length == 0) {
            return List.of();
        }

        List<UserPreset> list = new ArrayList<>();
        for (String fileName : fileNames) {
            File presetFile = new File(filterDir, fileName);
            list.add(new UserPreset(presetFile, filterName));
        }
        return list;
    }
}
