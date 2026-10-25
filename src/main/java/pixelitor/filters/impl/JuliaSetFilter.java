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

import pixelitor.filters.util.Complex;
import pixelitor.filters.util.ComplexBounds;

import java.awt.geom.Point2D;

/**
 * Renders a Julia-type set, where z is varied across pixels and the constant c is fixed.
 */
public class JuliaSetFilter extends EscapeTimeFilter {
    private final double cx;
    private final double cy;

    /**
     * Constructs a new JuliaSetFilter.
     *
     * @param filterName    The name of the filter.
     * @param iterator      The iteration strategy for the fractal.
     * @param zoom          The zoom level for the fractal.
     * @param zoomCenter    The relative coordinates of the center point for
     *                      zooming (in the unit square [0, 1] x [0, 1]).
     * @param maxIterations The maximum number of iterations for the escape time algorithm.
     * @param palette       The color palette used for rendering.
     * @param c             The fixed complex constant c.
     * @param insideOut     True to invert the initial z value using f(z) = 1/z, false otherwise.
     */
    public JuliaSetFilter(String filterName, IterationStrategy iterator,
                          double zoom, Point2D zoomCenter,
                          int maxIterations, int[] palette,
                          Complex c, boolean insideOut) {
        // the Julia view doesn't depend on the chosen iteration strategy
        super(filterName, new ComplexBounds(-2.0, 2.0, -1.2, 1.2),
            iterator, zoom, zoomCenter, maxIterations, palette, insideOut);
        this.cx = c.re();
        this.cy = c.im();
    }

    @Override
    protected int calcColor(double zx, double zy) {
        // the complex constant c is fixed for the entire image
        return calcIteratedColor(zx, zy, cx, cy);
    }
}
