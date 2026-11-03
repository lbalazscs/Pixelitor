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

package pixelitor.filters.painters;

import pixelitor.filters.gui.UserPreset;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import static java.awt.font.TextAttribute.*;

/**
 * Wraps a {@link Font} to provide a more usable API.
 */
public record FontInfo(
    // basic properties, edited in the text settings dialog
    String name,
    int size,
    boolean bold,
    boolean italic,

    // advanced properties, edited in the advanced text settings dialog
    boolean strikethrough,
    boolean kerning,
    boolean underline,
    boolean ligatures,
    int tracking // stored as a percentage (e.g., 100 = 1.0)
) {
    public FontInfo(Font font) {
        this(font, font.hasLayoutAttributes() ? font.getAttributes() : Map.of());
    }

    private FontInfo(Font font, Map<TextAttribute, ?> attributes) {
        this(
            font.getName(),
            font.getSize(),
            font.isBold(),
            font.isItalic(),
            STRIKETHROUGH_ON.equals(attributes.get(STRIKETHROUGH)),
            KERNING_ON.equals(attributes.get(KERNING)),
            UNDERLINE_ON.equals(attributes.get(UNDERLINE)),
            LIGATURES_ON.equals(attributes.get(LIGATURES)),
            extractTracking(attributes)
        );
    }

    public FontInfo(UserPreset preset) {
        this(
            preset.get("font_type"),
            preset.getInt("font_size"),
            preset.getBoolean("bold"),
            preset.getBoolean("italic"),
            preset.getBoolean("strikethrough"),
            preset.getBoolean("kerning"),
            preset.getBoolean("underline"),
            preset.getBoolean("ligatures"),
            preset.getInt("tracking", 0)
        );
    }

    private static int extractTracking(Map<TextAttribute, ?> map) {
        return map.get(TRACKING) instanceof Number n
            ? Math.round(100 * n.floatValue())
            : 0;
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

    public FontInfo withBasic(String name, int size, boolean bold, boolean italic) {
        return new FontInfo(name, size, bold, italic,
            strikethrough, kerning, underline, ligatures, tracking);
    }

    public FontInfo withAdvanced(boolean strikethrough, boolean kerning, boolean ligatures,
                                 boolean underline, int tracking) {
        return new FontInfo(name, size, bold, italic,
            strikethrough, kerning, underline, ligatures, tracking);
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
}
