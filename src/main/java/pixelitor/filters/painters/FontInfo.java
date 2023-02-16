/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.painters;

import pixelitor.filters.gui.UserPreset;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import static java.awt.font.TextAttribute.*;

/**
 * Wraps a {@link Font} into a more usable API.
 */
public class FontInfo {
    // basic properties
    private int size;
    private String name;
    private boolean bold;
    private boolean italic;

    // advanced properties
    private boolean strikethrough = false;
    private boolean kerning = false;
    private boolean underline = false;
    private boolean ligatures = false;
    private int tracking = 0;

    public FontInfo(Font font) {
        size = font.getSize();
        name = font.getName();
        bold = font.isBold();
        italic = font.isItalic();

        if (font.hasLayoutAttributes()) {
            var map = font.getAttributes();

            strikethrough = STRIKETHROUGH_ON.equals(map.get(STRIKETHROUGH));
            kerning = KERNING_ON.equals(map.get(KERNING));
            underline = UNDERLINE_ON.equals(map.get(UNDERLINE));
            ligatures = LIGATURES_ON.equals(map.get(LIGATURES));

            Float trackingValue = (Float) map.get(TRACKING);
            if (trackingValue != null) {
                tracking = (int) (100 * trackingValue);
            }
        }
    }

    public FontInfo(UserPreset preset) {
        size = preset.getInt("font_size");
        name = preset.get("font_type");
        bold = preset.getBoolean("bold");
        italic = preset.getBoolean("italic");

        strikethrough = preset.getBoolean("strikethrough");
        kerning = preset.getBoolean("kerning");
        underline = preset.getBoolean("underline");
        ligatures = preset.getBoolean("ligatures");
        tracking = preset.getInt("tracking", 0);
    }

    public void saveStateTo(UserPreset preset) {
        preset.putInt("font_size", size);
        preset.put("font_type", name);
        preset.putBoolean("bold", bold);
        preset.putBoolean("italic", italic);

        preset.putBoolean("strikethrough", strikethrough);
        preset.putBoolean("kerning", kerning);
        preset.putBoolean("underline", underline);
        preset.putBoolean("ligatures", ligatures);
        preset.putInt("tracking", tracking);
    }

    public void updateBasic(String name, int size, boolean bold, boolean italic) {
        this.name = name;
        this.size = size;
        this.bold = bold;
        this.italic = italic;
    }

    public void updateAdvanced(boolean strikethrough, boolean kerning, boolean ligatures,
                               boolean underline, int tracking) {
        this.strikethrough = strikethrough;
        this.kerning = kerning;
        this.ligatures = ligatures;
        this.underline = underline;
        this.tracking = tracking;
    }

    public Font createStyledFont() {
        Font font = createFont(name, size, bold, italic);

        Map<TextAttribute, Object> map = new HashMap<>();
        Boolean strikeThroughSetting = Boolean.FALSE;
        if (strikethrough) {
            strikeThroughSetting = STRIKETHROUGH_ON;
        }
        map.put(STRIKETHROUGH, strikeThroughSetting);

        Integer kerningSetting = 0;
        if (kerning) {
            kerningSetting = KERNING_ON;
        }
        map.put(KERNING, kerningSetting);

        Integer ligaturesSetting = 0;
        if (ligatures) {
            ligaturesSetting = LIGATURES_ON;
        }
        map.put(LIGATURES, ligaturesSetting);

        Integer underlineSetting = -1;
        if (underline) {
            underlineSetting = UNDERLINE_ON;
        }
        map.put(UNDERLINE, underlineSetting);

        map.put(TRACKING, tracking / 100.0f);

        return font.deriveFont(map);
    }

    private static Font createFont(String family, int size, boolean bold, boolean italic) {
        int style = Font.PLAIN;
        if (bold) {
            style |= Font.BOLD;
        }
        if (italic) {
            style |= Font.ITALIC;
        }
        return new Font(family, style, size);
    }

    public boolean hasStrikeThrough() {
        return strikethrough;
    }

    public boolean hasKerning() {
        return kerning;
    }

    public boolean hasUnderline() {
        return underline;
    }

    public boolean hasLigatures() {
        return ligatures;
    }

    public int getTracking() {
        return tracking;
    }
}
