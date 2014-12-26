/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.shapestool;

import java.awt.BasicStroke;

/**
 * An enum wrapper around the cap argument of a BasicStroke constructor
 */
enum BasicStrokeCap {
    ROUND("Round", BasicStroke.CAP_ROUND),
    BUTT("Butt", BasicStroke.CAP_BUTT),
    SQUARE("Square", BasicStroke.CAP_SQUARE);

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
}
