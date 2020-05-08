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
package pixelitor.tools.shapes;

import pixelitor.filters.gui.EnumParam;

import static java.awt.BasicStroke.*;

/**
 * An enum wrapper around the cap argument of a BasicStroke constructor
 */
public enum BasicStrokeCap {
    ROUND("Round", CAP_ROUND),
    BUTT("Butt", CAP_BUTT),
    SQUARE("Square", CAP_SQUARE);

    public static final String NAME = "Endpoint Cap";
    private final int value;
    private final String guiName;

    BasicStrokeCap(String guiName, int value) {
        this.guiName = guiName;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return guiName;
    }

    public static EnumParam<BasicStrokeCap> asParam() {
        return new EnumParam<>(NAME, BasicStrokeCap.class);
    }

    public static EnumParam<BasicStrokeCap> asParam(BasicStrokeCap defaultValue) {
        EnumParam<BasicStrokeCap> param = asParam();
        param.selectAndSetAsDefault(defaultValue);
        return param;
    }
}
