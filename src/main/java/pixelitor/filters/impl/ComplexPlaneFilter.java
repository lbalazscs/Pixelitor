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

import com.jhlabs.image.PointFilter;
import pixelitor.filters.util.ComplexBounds;

import java.awt.geom.Point2D;

/**
 * An abstract base filter that maps image pixel coordinates to a zoomable region of the complex plane.
 */
public abstract class ComplexPlaneFilter extends PointFilter {
    // points that never converge/escape are colored black
    public static final int IN_SET_COLOR = 0xFF_00_00_00;

    // the bounds in the complex plane
    private final ComplexBounds bounds;

    // the actual start in the complex plane, taking the zooming into account
    protected double xStart, yStart;

    // multipliers for translating image coordinates into complex coordinates
    protected double xMultiplier, yMultiplier;

    private final double zoomCenterX, zoomCenterY;
    private final double zoom;
    private final boolean stretched;

    /**
     * Constructs a new ComplexPlaneFilter.
     *
     * @param filterName  The name of the filter.
     * @param bounds      The rectangular bounds in the complex plane.
     * @param zoom        The zoom level.
     * @param zoomCenter  The relative coordinates of the center point for
     *                    zooming (in the unit square [0, 1] x [0, 1]).
     * @param stretched   True to stretch the complex plane independently on both axes
     *                    to fill the image; false to preserve aspect ratio (isotropic mapping).
     */
    protected ComplexPlaneFilter(String filterName,
                                 ComplexBounds bounds,
                                 double zoom,
                                 Point2D zoomCenter,
                                 boolean stretched) {
        super(filterName);

        this.bounds = bounds;
        this.zoom = zoom;
        this.zoomCenterX = zoomCenter.getX();
        this.zoomCenterY = zoomCenter.getY();
        this.stretched = stretched;
    }

    @Override
    public void setDimensions(int width, int height) {
        double rangeRe = bounds.width() / zoom;
        double rangeIm = bounds.height() / zoom;

        double extentX;
        double extentY;
        if (stretched) {
            xMultiplier = rangeRe / width;
            yMultiplier = rangeIm / height;
            extentX = rangeRe;
            extentY = rangeIm;
        } else {
            // isotropic mapping: uniform units per pixel
            // fitting the zoomed range to the shorter image side
            double unitsPerPixel = Math.max(rangeRe / width, rangeIm / height);
            xMultiplier = unitsPerPixel;
            yMultiplier = unitsPerPixel;
            extentX = width * unitsPerPixel;
            extentY = height * unitsPerPixel;
        }

        xStart = viewStart(bounds.minRe(), bounds.maxRe(), zoomCenterX, extentX);
        yStart = viewStart(bounds.minIm(), bounds.maxIm(), zoomCenterY, extentY);
    }

    /**
     * Computes the start coordinate on an axis. If the visible extent exceeds the bounds range,
     * the view is centered on the bounds. Otherwise, it is centered on the zoom center and clamped
     * within [min, max].
     */
    private static double viewStart(double min, double max, double relCenter, double extent) {
        double boundsRange = max - min;
        if (extent >= boundsRange) {
            return (min + max - extent) / 2.0;
        }
        double center = min + relCenter * boundsRange;
        return Math.clamp(center - extent / 2.0, min, max - extent);
    }
}
