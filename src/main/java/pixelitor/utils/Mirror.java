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

package pixelitor.utils;

import pixelitor.tools.Symmetry;

/**
 * A mirroring effect achieved with negative scaling.
 * Not to be confused with {@link Symmetry}.
 */
public enum Mirror {
    NONE("None", 1.0, 1.0),
    VERTICAL("Vertical", -1.0, 1.0),
    HORIZONTAL("Horizontal", 1.0, -1.0);

    private final String guiName;
    private final double multX;
    private final double multY;

    Mirror(String guiName, double multX, double multY) {
        this.guiName = guiName;
        this.multX = multX;
        this.multY = multY;
    }

    public double getScaleX(double scaleAbs) {
        return scaleAbs * multX;
    }

    public double getScaleY(double scaleAbs) {
        return scaleAbs * multY;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
