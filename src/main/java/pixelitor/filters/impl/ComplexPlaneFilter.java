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

import java.awt.image.BufferedImage;

/**
 * An abstract base filter that maps image pixel coordinates to a zoomable region of the complex plane.
 */
public abstract class ComplexPlaneFilter extends PointFilter {
    // points belonging to the set are colored black
    public static final int IN_SET_COLOR = 0xFF_00_00_00;

    // the bounds in the complex plane
    private final double xMin, xMax, yMin, yMax, xRange, yRange;

    // the actual start in the complex plane, taking the zooming into account
    protected double xStart, yStart;

    // multipliers for translating image coordinates into complex coordinates
    protected double xMultiplier, yMultiplier;

    private final double zoomCenterX, zoomCenterY;
    private final double zoom;

    /**
     * Constructs a new ComplexPlaneFilter.
     *
     * @param filterName  The name of the filter.
     * @param xMin        The minimum x boundary in the complex plane.
     * @param xMax        The maximum x boundary in the complex plane.
     * @param yMin        The minimum y boundary in the complex plane.
     * @param yMax        The maximum y boundary in the complex plane.
     * @param zoom        The zoom level.
     * @param zoomCenterX The x-coordinate of the center point for zooming (in [0, 1]).
     * @param zoomCenterY The y-coordinate of the center point for zooming (in [0, 1]).
     */
    protected ComplexPlaneFilter(String filterName,
                                 double xMin, double xMax,
                                 double yMin, double yMax,
                                 double zoom,
                                 double zoomCenterX,
                                 double zoomCenterY) {
        super(filterName);

        assert xMax > xMin && yMax > yMin : "invalid complex-plane bounds";

        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        this.xRange = xMax - xMin;
        this.yRange = yMax - yMin;

        this.zoom = zoom;
        this.zoomCenterX = zoomCenterX;
        this.zoomCenterY = zoomCenterY;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        // calculate the width and height of the view in the complex plane based on the zoom level
        double zoomedRangeX = xRange / zoom;
        double zoomedRangeY = yRange / zoom;

        // calculate multipliers for converting image coordinates to complex plane coordinates
        xMultiplier = zoomedRangeX / src.getWidth();
        yMultiplier = zoomedRangeY / src.getHeight();

        // find the zoom center point in the complex plane
        double zoomCenterComplexX = xMin + zoomCenterX * xRange;
        double zoomCenterComplexY = yMin + zoomCenterY * yRange;

        // calculate the boundaries of the zoomed view
        double zoomedMinX = zoomCenterComplexX - zoomedRangeX / 2.0;
        double zoomedMaxX = zoomCenterComplexX + zoomedRangeX / 2.0;
        double zoomedMinY = zoomCenterComplexY - zoomedRangeY / 2.0;
        double zoomedMaxY = zoomCenterComplexY + zoomedRangeY / 2.0;

        // adjust the view boundaries to ensure they stay within the original fractal limits
        xStart = adjustStart(zoomedMinX, zoomedMaxX, xMin, xMax);
        yStart = adjustStart(zoomedMinY, zoomedMaxY, yMin, yMax);

        return super.filter(src, dst);
    }

    /**
     * Adjusts the starting coordinate to keep the zoomed view within the original boundaries.
     */
    private static double adjustStart(double zoomedMin, double zoomedMax, double min, double max) {
        if (zoomedMax > max) {
            // if the zoomed view exceeds the maximum boundary, shift it back
            return zoomedMin - (zoomedMax - max);
        }
        if (zoomedMin < min) {
            // if the zoomed view is below the minimum boundary, clamp it to the minimum
            return min;
        }
        // otherwise, the view is within bounds
        return zoomedMin;
    }
}
