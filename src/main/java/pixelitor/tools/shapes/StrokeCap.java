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
 * The line cap style for a stroke, corresponding to the 'cap'
 * argument of the {@link BasicStroke} constructor.
 */
public enum StrokeCap {
    /**
     * A rounded end cap.
     */
    ROUND("Round", BasicStroke.CAP_ROUND),

    /**
     * A butt (square, no extension) end cap.
     */
    BUTT("Butt", BasicStroke.CAP_BUTT),

    /**
     * A square end cap (extends the line).
     */
    SQUARE("Square", BasicStroke.CAP_SQUARE);

    public static final String NAME = "Endpoint Cap";

    // the corresponding constant defined in BasicStroke
    private final int awtConstant;

    private final String displayName;

    StrokeCap(String displayName, int awtConstant) {
        this.displayName = displayName;
        this.awtConstant = awtConstant;
    }

    public int getValue() {
        return awtConstant;
    }

    public static EnumParam<StrokeCap> asParam() {
        var param = new EnumParam<>(NAME, StrokeCap.class);
        param.setToolTip("The shape of the line endpoints");
        return param;
    }

    public static EnumParam<StrokeCap> asParam(StrokeCap defaultCap) {
        return asParam().withDefault(defaultCap);
    }

    public String toSVGAttribute() {
        return "stroke-linecap=\"" + displayName.toLowerCase(Locale.ENGLISH) + "\"";
    }

    @Override
    public String toString() {
        return displayName;
    }
}
