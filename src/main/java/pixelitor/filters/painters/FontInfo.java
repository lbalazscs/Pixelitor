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

package pixelitor.filters.painters;

import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.GUIUtils;

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
    private boolean strikeThrough = false;
    private boolean kerning = false;
    private boolean underLine = false;
    private boolean ligatures = false;
    private int tracking = 0;

    public FontInfo(Font font) {
        size = font.getSize();
        name = font.getName();
        bold = font.isBold();
        italic = font.isItalic();

        if (font.hasLayoutAttributes()) {
            var map = font.getAttributes();

            strikeThrough = STRIKETHROUGH_ON.equals(map.get(STRIKETHROUGH));
            kerning = KERNING_ON.equals(map.get(KERNING));
            underLine = UNDERLINE_ON.equals(map.get(UNDERLINE));
            ligatures = LIGATURES_ON.equals(map.get(LIGATURES));

            Float trackingSetting = (Float) map.get(TRACKING);
            if (trackingSetting != null) {
                tracking = (int) (100 * trackingSetting);
            }
        }
    }

    public FontInfo(UserPreset preset) {
        size = preset.getInt("font_size");
        name = preset.get("font_type");
        bold = preset.getBoolean("bold");
        italic = preset.getBoolean("italic");

        strikeThrough = preset.getBoolean("strikethrough");
        kerning = preset.getBoolean("kerning");
        underLine = preset.getBoolean("underline");
        ligatures = preset.getBoolean("ligatures");
        tracking = preset.getInt("tracking");
    }

    public void saveStateTo(UserPreset preset) {
        preset.putInt("font_size", size);
        preset.put("font_type", name);
        preset.putBoolean("bold", bold);
        preset.putBoolean("italic", italic);

        preset.putBoolean("strikethrough", strikeThrough);
        preset.putBoolean("kerning", kerning);
        preset.putBoolean("underline", underLine);
        preset.putBoolean("ligatures", ligatures);
        preset.putInt("tracking", tracking);
    }

    public void updateBasic(String name, int size, boolean bold, boolean italic) {
        this.name = name;
        this.size = size;
        this.bold = bold;
        this.italic = italic;
    }

    public void updateAdvanced(boolean strikeThrough, boolean kerning, boolean ligatures,
                               boolean underline, int tracking) {
        this.strikeThrough = strikeThrough;
        this.kerning = kerning;
        this.ligatures = ligatures;
        this.underLine = underline;
        this.tracking = tracking;
    }

    public Font createFont() {
        Font font = GUIUtils.createFont(name, size, bold, italic);

        Map<TextAttribute, Object> map = new HashMap<>();
        Boolean strikeThroughSetting = Boolean.FALSE;
        if (strikeThrough) {
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
        if (underLine) {
            underlineSetting = UNDERLINE_ON;
        }
        map.put(UNDERLINE, underlineSetting);

        map.put(TRACKING, tracking / 100.0f);

        return font.deriveFont(map);
    }

    public boolean hasStrikeThrough() {
        return strikeThrough;
    }

    public boolean hasKerning() {
        return kerning;
    }

    public boolean hasUnderLine() {
        return underLine;
    }

    public boolean hasLigatures() {
        return ligatures;
    }

    public int getTracking() {
        return tracking;
    }
}
