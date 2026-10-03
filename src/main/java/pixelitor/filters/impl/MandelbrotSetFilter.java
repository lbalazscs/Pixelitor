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

import pixelitor.filters.MandelbrotSet;

import java.awt.geom.Rectangle2D;

/**
 * Renders a Mandelbrot-type set, where the constant c is varied across pixels and z starts at 0.
 */
public class MandelbrotSetFilter extends EscapeTimeFilter {
    private final boolean insideOut;

    /**
     * Constructs a new MandelbrotSetFilter.
     *
     * @param view          The starting view in the complex plane for this fractal.
     * @param iterator      The iteration strategy for the fractal.
     * @param zoom          The zoom level for the fractal.
     * @param zoomCenterX   The x-coordinate of the center point for zooming.
     * @param zoomCenterY   The y-coordinate of the center point for zooming.
     * @param maxIterations The maximum number of iterations for the escape time algorithm.
     * @param colors        The color palette used for rendering.
     * @param insideOut     True to invert the complex constant c using f(c) = 1/c, false otherwise.
     */
    public MandelbrotSetFilter(Rectangle2D view, IterationStrategy iterator, double zoom,
                               double zoomCenterX, double zoomCenterY, int maxIterations,
                               int[] colors, boolean insideOut) {
        super(MandelbrotSet.NAME,
            view.getX(), view.getX() + view.getWidth(),
            view.getY(), view.getY() + view.getHeight(),
            iterator, zoom, zoomCenterX, zoomCenterY, maxIterations, colors);
        this.insideOut = insideOut;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // for Mandelbrot-type sets, z starts at 0
        double zx = 0;
        double zy = 0;

        // the complex constant c is mapped from the pixel's image coordinates
        double cx = xStart + x * xMultiplier;
        double cy = yStart + y * yMultiplier;

        if (insideOut) {
            // invert c using f(c) = 1/c
            double d = cx * cx + cy * cy;
            if (d == 0) {
                // c is at the origin, so 1/c is at infinity => escape immediately
                return colors[colors.length - 1];
            }
            // use the inverted c' as c
            cx = cx / d;
            cy = -cy / d;
        }

        // check for known regions of the set for a quick exit
        if (iterator.checkShortcuts(cx, cy)) {
            return IN_SET_COLOR; // the point is in the set
        }
        return calcIteratedColor(zx, zy, cx, cy);
    }
}
