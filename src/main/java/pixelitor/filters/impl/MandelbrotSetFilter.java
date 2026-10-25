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

import java.awt.geom.Point2D;

/**
 * Renders a Mandelbrot-type set, where the constant c is varied across pixels and z starts at 0.
 */
public class MandelbrotSetFilter extends EscapeTimeFilter {
    /**
     * Constructs a new MandelbrotSetFilter.
     *
     * @param filterName    The name of the filter.
     * @param iterator      The iteration strategy for the fractal.
     * @param zoom          The zoom level for the fractal.
     * @param zoomCenter    The relative coordinates of the center point for
     *                      zooming (in the unit square [0, 1] x [0, 1]).
     * @param maxIterations The maximum number of iterations for the escape time algorithm.
     * @param palette       The color palette used for rendering.
     * @param insideOut     True to invert the complex constant c using f(c) = 1/c, false otherwise.
     */
    public MandelbrotSetFilter(String filterName,
                               IterationStrategy iterator, double zoom,
                               Point2D zoomCenter, int maxIterations,
                               int[] palette, boolean insideOut) {
        super(filterName, iterator.getBounds(),
            iterator, zoom, zoomCenter, maxIterations, palette, insideOut);
    }

    @Override
    protected int calcColor(double cx, double cy) {
        // for Mandelbrot-type sets, z starts at 0
        return iterator.checkShortcuts(cx, cy)
            ? IN_SET_COLOR
            : calcIteratedColor(0, 0, cx, cy);
    }
}
