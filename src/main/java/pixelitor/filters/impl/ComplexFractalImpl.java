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

package pixelitor.filters.impl;

import com.jhlabs.image.PointFilter;

import java.awt.image.BufferedImage;

/**
 * A common superclass for the Mandelbrot and Julia fractal implementations.
 */
public abstract class ComplexFractalImpl extends PointFilter {
    // the bounds in the complex space
    private final double cxMin;
    private final double cxMax;
    private final double cyMin;
    private final double cyMax;
    private final double cxRange;
    private final double cyRange;

    // the actual start in the complex space,
    // taking the zooming into account
    protected double cxStart;
    protected double cyStart;

    // multipliers for translating image
    // coordinates into complex coordinates
    protected double xMultiplier;
    protected double yMultiplier;

    private double zoomCenterX = 0.5;
    private double zoomCenterY = 0.5;

    private int maxIterations = 570;
    private double zoom = 1.0;

    protected int[] colors;

    protected ComplexFractalImpl(String filterName,
                                 double cxMin, double cxMax,
                                 double cyMin, double cyMax) {
        super(filterName);

        this.cxMin = cxMin;
        this.cxMax = cxMax;
        this.cyMin = cyMin;
        this.cyMax = cyMax;

        this.cxRange = cxMax - cxMin;
        this.cyRange = cyMax - cyMin;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        // calculate the width and height of the view in the complex plane based on the zoom level
        double zoomedRangeX = cxRange / zoom;
        double zoomedRangeY = cyRange / zoom;

        // calculate multipliers for converting image coordinates to complex plane coordinates
        xMultiplier = zoomedRangeX / src.getWidth();
        yMultiplier = zoomedRangeY / src.getHeight();

        // find the zoom center point in the complex plane
        double zoomCenterComplexX = cxMin + zoomCenterX * cxRange;
        double zoomCenterComplexY = cyMin + zoomCenterY * cyRange;

        // calculate the boundaries of the zoomed view
        double zoomedMinX = zoomCenterComplexX - zoomedRangeX / 2.0;
        double zoomedMaxX = zoomCenterComplexX + zoomedRangeX / 2.0;
        double zoomedMinY = zoomCenterComplexY - zoomedRangeY / 2.0;
        double zoomedMaxY = zoomCenterComplexY + zoomedRangeY / 2.0;

        // adjust the view boundaries to ensure they stay within the original fractal limits
        cxStart = adjustStart(zoomedMinX, zoomedMaxX, cxMin, cxMax);
        cyStart = adjustStart(zoomedMinY, zoomedMaxY, cyMin, cyMax);

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

    /**
     * Calculates the color for a point based on its escape time.
     */
    protected int calcIteratedColor(double zx, double zy, double cx, double cy) {
        int it = iterate(zx, zy, cx, cy, maxIterations);
        return colors[it];
    }

    private static int iterate(double x, double y, double cx, double cy, int maxIt) {
        int it = maxIt;
        double x2 = 0;
        double y2 = 0;
        double xy;
        while (x2 + y2 <= 4 && it > 0) {
            it--;
            xy = x * y;
            x2 = x * x;
            y2 = y * y;
            x = x2 - y2 + cx;
            y = xy + xy + cy;
        }
        return it;
    }

    // the unoptimized version of the "iterate" method, where
    // the algorithm is clearer (not used, kept for reference)
    @SuppressWarnings("unused")
    private static int iterateReference(double x, double y, double cx, double cy, int maxIt) {
        int it = maxIt;
        while (x * x + y * y < 4 && it > 0) {
            it--;
            double xTmp = x * x - y * y + cx;
            double yTmp = 2.0 * x * y + cy;
            if (x == xTmp && y == yTmp) {
                it = 0;
                break;
            }
            y = yTmp;
            x = xTmp;
        }
        return it;
    }

    /**
     * Sets the zoom level for the fractal.
     */
    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    /**
     * Sets the color palette used for rendering.
     */
    public void setColors(int[] colors) {
        this.colors = colors;
    }

    /**
     * Sets the center point for zooming.
     */
    public void setZoomCenter(double zoomCenterX, double zoomCenterY) {
        this.zoomCenterX = zoomCenterX;
        this.zoomCenterY = zoomCenterY;
    }

    /**
     * Sets the maximum number of iterations for the escape time algorithm.
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
