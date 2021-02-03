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

package pixelitor.filters;

import pixelitor.filters.util.FilterAction;

/**
 * Used when colors of all pixels have to be changed
 * uniformly and independently from each other
 */
public interface RGBPixelOp {
    /**
     * Computes a new color from the given channel values
     * and returns the changed color in ARGB int format.
     */
    int changeRGB(int a, int r, int g, int b);

    default FilterAction toFilterAction(String name) {
        return new FilterAction(name, () -> new ExtractChannelFilter(this)).noGUI();
    }
}
