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

package pixelitor.filters.curves;

import java.awt.Color;

/**
 * Curve type enum
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public enum ToneCurveType {
    RGB("RGB", Color.BLACK),
    RED("Red", Color.RED),
    GREEN("Green", Color.GREEN),
    BLUE("Blue", Color.BLUE);

    private final String name;
    private final Color color;
    private final Color inactiveColor;

    ToneCurveType(String name, Color color) {
        this.name = name;
        this.color = color;
        inactiveColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }

    public Color getColor() {
        return color;
    }

    public Color getInactiveColor() {
        return inactiveColor;
    }

    @Override
    public String toString() {
        return name;
    }
}