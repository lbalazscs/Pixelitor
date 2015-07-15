/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
package pixelitor.tools.shapestool;

import pixelitor.filters.gui.EnumParam;

import static java.awt.BasicStroke.JOIN_BEVEL;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.BasicStroke.JOIN_ROUND;

/**
 * An enum wrapper around the join argument of a BasicStroke constructor
 */
public enum BasicStrokeJoin {
    ROUND("Round", JOIN_ROUND),
    BEVEL("Bevel", JOIN_BEVEL),
    MITER("Miter", JOIN_MITER);

    private final int value;
    private final String guiName;

    BasicStrokeJoin(String guiName, int value) {
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

    public static EnumParam<BasicStrokeJoin> asParam() {
        return new EnumParam<>("Cap", BasicStrokeJoin.class);
    }
}
