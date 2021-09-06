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
package pixelitor.tools.shapes;

import pixelitor.filters.gui.EnumParam;

import java.awt.BasicStroke;

import static java.awt.BasicStroke.*;

/**
 * An enum wrapper around the join argument of a {@link BasicStroke} constructor
 */
public enum StrokeJoin {
    ROUND("Round", JOIN_ROUND),
    BEVEL("Bevel", JOIN_BEVEL),
    MITER("Miter", JOIN_MITER);

    public static final String NAME = "Corner Join";
    private final int value;
    private final String guiName;

    StrokeJoin(String guiName, int value) {
        this.guiName = guiName;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static EnumParam<StrokeJoin> asParam() {
        var param = new EnumParam<>(NAME, StrokeJoin.class);
        param.setToolTip("The way lines connect at the corners");
        return param;
    }

    @Override
    public String toString() {
        return guiName;
    }

    public String toSVG() {
        return "stroke-linejoin=\"" + guiName.toLowerCase() + "\"";
    }
}
