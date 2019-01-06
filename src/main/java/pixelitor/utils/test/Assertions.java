/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.OpenComps;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Static, boolean-returning methods that
 * can be conveniently used after the assert keyword
 */
public class Assertions {
    private Assertions() {
    }

    public static boolean hasMask(boolean enabled, boolean linked) {
        Layer layer = OpenComps.getActiveLayerOrNull();
        if (layer == null) {
            throw new IllegalStateException();
        }
        if (!layer.hasMask()) {
            return false;
        }
        if (layer.isMaskEnabled() != enabled) {
            return false;
        }
        LayerMask mask = layer.getMask();
        return mask.isLinked() == linked;
    }

    public static boolean numLayersIs(int expected) {
        Composition comp = OpenComps.getActiveCompOrNull();
        if (comp == null) {
            throw new IllegalStateException();
        }
        return comp.getNumLayers() == expected;
    }

    public static boolean callingClassIs(String name) {
        // checks the caller of the caller
        String callingClassName = new Exception().getStackTrace()[2].getClassName();
        return callingClassName.contains(name);
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean checkRasterMinimum(BufferedImage newImage) {
        if (RandomGUITest.isRunning()) {
            WritableRaster raster = newImage.getRaster();
            if ((raster.getMinX() != 0) || (raster.getMinY() != 0)) {
                throw new
                        IllegalArgumentException("Raster " + raster +
                        " has minX or minY not equal to zero: "
                        + raster.getMinX() + ' ' + raster.getMinY());
            }
        }
        return true;
    }
}
