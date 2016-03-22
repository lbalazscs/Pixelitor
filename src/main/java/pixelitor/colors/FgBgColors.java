/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import java.awt.Color;

public class FgBgColors {
    private static FgBgColorSelector gui;

    private FgBgColors() {
    }

    public static void setGUI(FgBgColorSelector gui) {
        FgBgColors.gui = gui;
    }

    public static FgBgColorSelector getGUI() {
        return gui;
    }

    public static Color getFG() {
        return gui.getFgColor();
    }

    public static Color getBG() {
        return gui.getBgColor();
    }

    public static void setFG(Color c) {
        gui.setFgColor(c);
    }

    public static void setBG(Color c) {
        gui.setBgColor(c);
    }

    public static void randomizeColors() {
        gui.randomizeColorsAction.actionPerformed(null);
    }

    public static void setLayerMaskEditing(boolean b) {
        gui.setLayerMaskEditing(b);
    }
}
