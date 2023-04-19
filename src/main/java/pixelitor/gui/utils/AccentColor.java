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

package pixelitor.gui.utils;

import pixelitor.colors.Colors;

import java.awt.*;

/**
 * The accent color used in flat LAFs.
 */
public enum AccentColor {
    BLUE("Blue", "#0A84FF"),
    PURPLE("Purple", "#BF5AF2"),
    RED("Red", "#FF453A"),
    ORANGE("Orange", "#FF9F0A"),
    GREEN("Green", "#32D74B");

    AccentColor(String name, String hexCode) {
        this.name = name;
        this.hexCode = hexCode;
    }

    private final String name;
    private final String hexCode;
    private Color color;

    public String asHexCode() {
        return hexCode;
    }

    public Color asColor() {
        if (color == null) {
            color = Colors.fromHTMLHex(hexCode.substring(1));
        }
        return color;
    }

    @Override
    public String toString() {
        return name;
    }
}
