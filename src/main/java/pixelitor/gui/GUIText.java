/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import javax.swing.*;

import static pixelitor.utils.Texts.i18n;

/**
 * Common strings
 */
public class GUIText {
    public static final String OK = UIManager.getString("OptionPane.okButtonText");
    public static final String CANCEL = UIManager.getString("OptionPane.cancelButtonText");

    public static final String CLOSE_DIALOG = i18n("close_dialog");
    public static final String OPACITY = i18n("opacity") + " (%)";
    public static final String ZOOM = i18n("zoom");
    public static final String MERGE_DOWN = i18n("merge_down");
    public static final String MERGE_DOWN_TT = i18n("merge_down_tt");
    public static final String HUE = i18n("hue");
    public static final String SATURATION = i18n("saturation");
    public static final String BRIGHTNESS = i18n("brightness");
    public static final String COLOR = i18n("color");
    public static final String MIRROR = i18n("mirror");
    public static final String RADIUS = i18n("radius");
    public static final String BRUSH = i18n("brush");
    public static final String TYPE = i18n("type");
    public static final String FILL_WITH = i18n("fill_with");
    public static final String FG_COLOR = i18n("fg_color");
    public static final String BG_COLOR = i18n("bg_color");

    public static final String PRESETS = i18n("presets");
    public static final String BUILT_IN_PRESETS = i18n("builtin_presets");

    public static final String HELP = i18n("help");
    public static final String COPY_AS_JSON = "Copy as JSON";

    // LAB color labels
    public static final String RED_GREEN_A = "Green-Red (a)";
    public static final String BLUE_YELLOW_B = "Blue-Yellow (b)";
    public static final String LIGHTNESS = "Lightness";

    public static final String COLOR_SPACE = "Color Space";
    public static final String CHANNEL = "Channel";

    private GUIText() {
    }
}
