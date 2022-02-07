/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.colors;

import pixelitor.filters.gui.UserPreset;

import java.awt.Color;

/**
 * This class is used for global access to foreground
 * and background colors, and also for separating the
 * GUI code from the testing code.
 */
public class FgBgColors {
    private static FgBgColorSelector selector;

    private FgBgColors() {
    }

    // during testing this is set to a mocked value
    public static void setUI(FgBgColorSelector selector) {
        FgBgColors.selector = selector;
    }

    public static FgBgColorSelector getGUI() {
        return selector;
    }

    public static Color getFGColor() {
        return selector.getFgColor();
    }

    public static Color getBGColor() {
        return selector.getBgColor();
    }

    public static Color getRealFgColor() {
        return selector.getRealFgColor();
    }

    public static Color getRealBgColor() {
        return selector.getRealBgColor();
    }

    public static void setFGColor(Color c) {
        selector.setFgColor(c, true);
    }

    public static void setFGColor(Color c, boolean notifyListeners) {
        selector.setFgColor(c, notifyListeners);
    }

    public static void setBGColor(Color c) {
        selector.setBgColor(c, true);
    }

    public static void setBGColor(Color c, boolean notifyListeners) {
        selector.setBgColor(c, notifyListeners);
    }

    public static void randomizeColors() {
        selector.randomizeColors();
    }

    public static void setDefaultColors() {
        selector.setDefaultColors();
    }

    public static void setLayerMaskEditing(boolean b) {
        selector.setLayerMaskEditing(b);
    }

    public static void saveStateTo(UserPreset preset) {
        preset.putColor("FG", getFGColor());
        preset.putColor("BG", getBGColor());
    }

    public static void loadStateFrom(UserPreset preset) {
        setFGColor(preset.getColor("FG"));
        setBGColor(preset.getColor("BG"));
    }
}
