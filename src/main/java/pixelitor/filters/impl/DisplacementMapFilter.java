/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.impl;

import com.jhlabs.image.TransformFilter;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

public class DisplacementMapFilter extends TransformFilter {
    private final int[] mapPixels;
    private final int mapWidth;
    private final int mapHeight;

    private final double amount;
    private final boolean tile;
    private final double sin;
    private final double cos;

    /**
     * Constructs a DisplacementMapFilter.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param map           the displacement map image whose luminosity drives per-pixel displacement;
     *                      lighter areas shift pixels in the angle arrow's direction,
     *                      darker areas shift them in the opposite direction.
     * @param tile          if {@code true}, the displacement map is tiled across the source image;
     *                      if {@code false}, pixels outside the map bounds are left unchanged.
     * @param amount        the maximum displacement distance in pixels; the actual per-pixel
     *                      displacement is scaled by the map's normalized luminosity, centered
     *                      so that mid-grey produces no displacement.
     * @param angle         the angle in radians defining the direction of displacement for
     *                      bright map pixels.
     */
    public DisplacementMapFilter(String filterName, int edgeAction, int interpolation,
                                 BufferedImage map, boolean tile, double amount, double angle) {
        super(filterName, edgeAction, interpolation);

        mapPixels = ImageUtils.getPixels(map);
        mapWidth = map.getWidth();
        mapHeight = map.getHeight();

        this.tile = tile;
        this.amount = amount;
        this.sin = Math.sin(angle + Math.PI);
        this.cos = Math.cos(angle + Math.PI);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        int mapX = x;
        int mapY = y;
        if (tile) {
            mapX = x % mapWidth;
            mapY = y % mapHeight;
        } else {
            if (x >= mapWidth || y >= mapHeight) {
                out[0] = x;
                out[1] = y;
                return;
            }
        }

        int mapRGB = mapPixels[mapY * mapWidth + mapX];
        int r = (mapRGB >>> 16) & 0xFF;
        int g = (mapRGB >>> 8) & 0xFF;
        int b = mapRGB & 0xFF;

        int lum = r + g + b;

        // Max luminosity = 255*3 = 765.
        // Light values move the pixels in the angle arrow's direction,
        // dark values move them in the opposite direction.
        double dist = amount * lum / 765.0 - amount / 2.0;
        double distX = dist * cos;
        double distY = dist * sin;

        out[0] = (float) (x + distX);
        out[1] = (float) (y + distY);
    }
}
