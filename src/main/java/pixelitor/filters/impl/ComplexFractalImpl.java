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
import net.jafama.FastMath;

import java.awt.geom.Rectangle2D;
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

    protected IterationStrategy iterator;

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
        int it = iterator.iterate(zx, zy, cx, cy, maxIterations);
        return colors[it];
    }

    /**
     * Sets the iteration strategy for the fractal.
     */
    public void setIterator(IterationStrategy iterator) {
        this.iterator = iterator;
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

    /**
     * A strategy interface for different fractal iteration algorithms.
     */
    public interface IterationStrategy {
        double ESCAPE_RADIUS_2 = 4.0; // the squared escape radius

        /**
         * Iterates a fractal formula for a given point and returns the
         * number of iterations before escaping, or 0 if it doesn't escape.
         */
        int iterate(double zx, double zy, double cx, double cy, int maxIterations);

        /**
         * Checks for regions that are known to be part of the set, allowing for a fast exit.
         */
        default boolean checkShortcuts(double cx, double cy) {
            return false; // no shortcut by default
        }

        /**
         * Returns the starting view in the complex plane for this fractal.
         */
        Rectangle2D getComplexView();
    }

    public static class MandelbrotStrategy implements IterationStrategy {
        @Override
        public int iterate(double x, double y, double cx, double cy, int maxIt) {
            int it = maxIt;
            double x2 = 0;
            double y2 = 0;
            double xy;
            while (x2 + y2 <= ESCAPE_RADIUS_2 && it > 0) {
                it--;
                xy = x * y;
                x2 = x * x;
                y2 = y * y;
                x = x2 - y2 + cx;
                y = xy + xy + cy;
            }
            return it;
        }

        // an unoptimized version of the iterate method, kept for reference to show the algorithm more clearly
        @SuppressWarnings("unused")
        public int iterateReference(double x, double y, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (x * x + y * y < ESCAPE_RADIUS_2 && it > 0) {
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

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            // check if the point is inside the main cardioid
            if (cx > -0.75 && cx < 0.37 && cy < 0.65 && cy > -0.65) { // approximate check
                double cm = cx - 1 / 4.0;
                double cy2 = cy * cy;
                double q = cm * cm + cy2;
                if (q * (q + cm) < cy2 / 4.0) { // exact check
                    return true; // point is in the set
                }
            }

            // check if the point is in the period-2 bulb
            if (cx < -0.75 && cx > -1.25 && cy < 0.28 && cy > -0.28) { // approximate check
                if ((cx + 1) * (cx + 1) + cy * cy < 1 / 16.0) { // exact check
                    return true; // point is in the set
                }
            }

            return false; // point is not in a known region
        }

        @Override
        public Rectangle2D getComplexView() {
            // corresponds to cxMin=-2.2, cxMax=0.7, cyMin=-1.2, cyMax=1.2
            return new Rectangle2D.Double(-2.2, -1.2, 2.9, 2.4);
        }
    }

    public static class BurningShipStrategy implements IterationStrategy {
        @Override
        public int iterate(double x, double y, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (x * x + y * y <= ESCAPE_RADIUS_2 && it > 0) {
                it--;
                // this implements z_n+1 = (|Re(z_n)| + i*|Im(z_n)|)^2 + c
                x = Math.abs(x);
                y = Math.abs(y);

                double xTmp = x * x - y * y + cx;
                y = 2.0 * x * y + cy;
                x = xTmp;
            }
            return it;
        }

        @Override
        public Rectangle2D getComplexView() {
            return new Rectangle2D.Double(-2.2, -2.0, 3.4, 3.0);
        }
    }

    public static class TricornStrategy implements IterationStrategy {
        @Override
        public int iterate(double x, double y, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (x * x + y * y <= ESCAPE_RADIUS_2 && it > 0) {
                it--;
                // use the conjugate of z, which means negating the
                // imaginary part before calculating the next step
                double xTmp = x * x - y * y + cx;
                y = -2.0 * x * y + cy; // the only change is the minus sign here
                x = xTmp;
            }
            return it;
        }

        @Override
        public Rectangle2D getComplexView() {
            return new Rectangle2D.Double(-2.5, -2.5, 5.0, 5.0);
        }
    }

    public static class MultibrotStrategy3 implements IterationStrategy {
        private final double safeRadiusSq;

        public MultibrotStrategy3() {
            safeRadiusSq = calcSafeMultiBrotRadiusSq(3);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_2 && it > 0) {
                it--;
                // calculate z^3 + c
                // z^3 = (zx + i*zy)^3 = (zx^3 - 3*zx*zy^2) + i*(3*zx^2*zy - zy^3)
                double zx2 = zx * zx;
                double zy2 = zy * zy;

                double next_zx = zx * (zx2 - 3 * zy2) + cx;
                double next_zy = zy * (3 * zx2 - zy2) + cy;

                zx = next_zx;
                zy = next_zy;
            }
            return it;
        }

        @Override
        public Rectangle2D getComplexView() {
            return new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0);
        }
    }

    public static class MultibrotStrategy4 implements IterationStrategy {
        private final double safeRadiusSq;

        public MultibrotStrategy4() {
            safeRadiusSq = calcSafeMultiBrotRadiusSq(4);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_2 && it > 0) {
                it--;
                // calculate z^4 + c by computing z^4 = (z^2)^2
                // first, z^2
                double z2_re = zx * zx - zy * zy;
                double z2_im = 2 * zx * zy;

                // then, (z^2)^2
                double next_zx = z2_re * z2_re - z2_im * z2_im + cx;
                double next_zy = 2 * z2_re * z2_im + cy;

                zx = next_zx;
                zy = next_zy;
            }
            return it;
        }

        @Override
        public Rectangle2D getComplexView() {
            return new Rectangle2D.Double(-1.5, -1.5, 2.5, 3.0);
        }
    }

    public static class MultibrotStrategy5 implements IterationStrategy {
        private final double safeRadiusSq;

        public MultibrotStrategy5() {
            safeRadiusSq = calcSafeMultiBrotRadiusSq(5);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_2 && it > 0) {
                it--;
                // calculate z^5 + c by computing z^5 = z^4 * z = (z^2)^2 * z
                // first, z^2
                double z2_re = zx * zx - zy * zy;
                double z2_im = 2 * zx * zy;

                // then, z^4 = (z^2)^2
                double z4_re = z2_re * z2_re - z2_im * z2_im;
                double z4_im = 2 * z2_re * z2_im;

                // finally, z^5 = z^4 * z
                double next_zx = z4_re * zx - z4_im * zy + cx;
                double next_zy = z4_re * zy + z4_im * zx + cy;

                zx = next_zx;
                zy = next_zy;
            }
            return it;
        }

        @Override
        public Rectangle2D getComplexView() {
            return new Rectangle2D.Double(-1.0, -1.0, 2.0, 2.0);
        }
    }

    /**
     * Calculates the squared radius of a circle that is guaranteed to be in a multibrot set.
     */
    private static double calcSafeMultiBrotRadiusSq(int d) {
        double r = (1.0 - 1.0 / d) * FastMath.pow(1.0 / d, 1.0 / (d - 1.0));
        return r * r;
    }
}
