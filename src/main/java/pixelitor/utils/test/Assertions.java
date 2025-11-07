/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils.test;

import java.awt.image.BufferedImage;

/**
 * Static, boolean-returning methods that
 * can be conveniently used after the assert keyword.
 */
public class Assertions {
    private Assertions() {
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean rasterStartsAtOrigin(BufferedImage image) {
        var raster = image.getRaster();
        if (raster.getMinX() != 0 || raster.getMinY() != 0) {
            throw new IllegalStateException("Raster " + raster +
                " has minX or minY not equal to zero: "
                + raster.getMinX() + ' ' + raster.getMinY());
        }
        return true;
    }
}
