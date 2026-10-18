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

import pixelitor.filters.JuliaSet;
import pixelitor.filters.util.Complex;
import pixelitor.filters.util.ComplexBounds;

import java.awt.geom.Point2D;

/**
 * Renders a Julia-type set, where z is varied across pixels and the constant c is fixed.
 */
public class JuliaSetFilter extends EscapeTimeFilter {
    private final double cx;
    private final double cy;
    private final boolean insideOut;

    /**
     * Constructs a new JuliaSetFilter.
     *
     * @param iterator      The iteration strategy for the fractal.
     * @param zoom          The zoom level for the fractal.
     * @param zoomCenter    The relative coordinates of the center point for
     *                      zooming (in the unit square [0, 1] x [0, 1]).
     * @param maxIterations The maximum number of iterations for the escape time algorithm.
     * @param colors        The color palette used for rendering.
     * @param c             The fixed complex constant c.
     * @param insideOut     True to invert the initial z value using f(z) = 1/z, false otherwise.
     */
    public JuliaSetFilter(IterationStrategy iterator,
                          double zoom, Point2D zoomCenter,
                          int maxIterations, int[] colors,
                          Complex c, boolean insideOut) {
        // the Julia view doesn't depend on the chosen iteration strategy
        super(JuliaSet.NAME, new ComplexBounds(-2.0, 2.0, -1.2, 1.2),
            iterator, zoom, zoomCenter, maxIterations, colors);
        this.cx = c.re();
        this.cy = c.im();
        this.insideOut = insideOut;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // for Julia-type sets, the initial z is mapped from the pixel's image coordinates
        double zx = xStart + x * xMultiplier;
        double zy = yStart + y * yMultiplier;

        if (insideOut) {
            // invert the initial z value using f(z) = 1/z
            double d = zx * zx + zy * zy;
            if (d == 0) {
                // z0 is at the origin, so 1/z0 is at infinity => escape immediately
                return colors[colors.length - 1];
            }
            // use the inverted z0' as z0
            zx = zx / d;
            zy = -zy / d;
        }

        // the complex constant c is fixed for the entire image
        return calcIteratedColor(zx, zy, cx, cy);
    }
}
