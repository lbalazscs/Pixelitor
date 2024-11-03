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

package pixelitor.filters.painters;

import pixelitor.filters.gui.UserPreset;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import static java.awt.font.TextAttribute.KERNING;
import static java.awt.font.TextAttribute.KERNING_ON;
import static java.awt.font.TextAttribute.LIGATURES;
import static java.awt.font.TextAttribute.LIGATURES_ON;
import static java.awt.font.TextAttribute.STRIKETHROUGH;
import static java.awt.font.TextAttribute.STRIKETHROUGH_ON;
import static java.awt.font.TextAttribute.TRACKING;
import static java.awt.font.TextAttribute.UNDERLINE;
import static java.awt.font.TextAttribute.UNDERLINE_ON;

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
    private int tracking = 0; // Stored as percentage (100 = 1.0)

    public FontInfo(Font font) {
        size = font.getSize();
        name = font.getName();
        bold = font.isBold();
        italic = font.isItalic();

        if (font.hasLayoutAttributes()) {
            extractLayoutAttributes(font);
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

    /**
     * Extracts advanced typography attributes from a Font's attribute map.
     */
    private void extractLayoutAttributes(Font font) {
        Map<TextAttribute, ?> map = font.getAttributes();

        strikethrough = STRIKETHROUGH_ON.equals(map.get(STRIKETHROUGH));
        kerning = KERNING_ON.equals(map.get(KERNING));
        underline = UNDERLINE_ON.equals(map.get(UNDERLINE));
        ligatures = LIGATURES_ON.equals(map.get(LIGATURES));

        Float trackingValue = (Float) map.get(TRACKING);
        if (trackingValue != null) {
            tracking = (int) (100 * trackingValue);
        }
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

    public Font createFont() {
        Font font = createBaseFont(name, size, bold, italic);
        return font.deriveFont(createAttributeMap());
    }

    private Map<TextAttribute, Object> createAttributeMap() {
        Map<TextAttribute, Object> map = new HashMap<>();

        map.put(STRIKETHROUGH, strikethrough ? STRIKETHROUGH_ON : Boolean.FALSE);
        map.put(KERNING, kerning ? KERNING_ON : 0);
        map.put(LIGATURES, ligatures ? LIGATURES_ON : 0);
        map.put(UNDERLINE, underline ? UNDERLINE_ON : -1);
        map.put(TRACKING, tracking / 100.0f);

        return map;
    }

    private static Font createBaseFont(String family, int size, boolean bold, boolean italic) {
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
