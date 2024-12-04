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
package pixelitor.tools.shapes;

import pixelitor.filters.gui.EnumParam;

import java.awt.BasicStroke;
import java.util.Locale;

/**
 * The join style for a stroke, corresponding to the 'join'
 * argument of the {@link BasicStroke} constructor.
 */
public enum StrokeJoin {
    /**
     * Joins path segments by rounding off the corner with a circular arc.
     */
    ROUND("Round", BasicStroke.JOIN_ROUND),

    /**
     * Joins path segments by connecting the outer corners of their strokes with a straight line segment.
     */
    BEVEL("Bevel", BasicStroke.JOIN_BEVEL),

    /**
     * Joins path segments by extending their outside edges until they meet.
     */
    MITER("Miter", BasicStroke.JOIN_MITER);

    public static final String NAME = "Corner Join";

    // the corresponding constant defined in BasicStroke
    private final int awtConstant;

    private final String displayName;

    StrokeJoin(String displayName, int awtConstant) {
        this.displayName = displayName;
        this.awtConstant = awtConstant;
    }

    public int getValue() {
        return awtConstant;
    }

    public static EnumParam<StrokeJoin> asParam() {
        var param = new EnumParam<>(NAME, StrokeJoin.class);
        param.setToolTip("The way lines connect at the corners");
        return param;
    }

    public String toSVGAttribute() {
        return "stroke-linejoin=\"" + displayName.toLowerCase(Locale.ENGLISH) + "\"";
    }

    @Override
    public String toString() {
        return displayName;
    }
}
