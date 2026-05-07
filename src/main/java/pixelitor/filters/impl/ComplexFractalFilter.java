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
import net.jafama.FastMath;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * A common superclass for the Mandelbrot and Julia fractal implementations.
 */
public abstract class ComplexFractalFilter extends PointFilter {
    // the bounds in the complex plane
    private final double cxMin;
    private final double cxMax;
    private final double cyMin;
    private final double cyMax;
    private final double cxRange;
    private final double cyRange;

    // the actual start in the complex plane,
    // taking the zooming into account
    protected double cxStart;
    protected double cyStart;

    // multipliers for translating image
    // coordinates into complex coordinates
    protected double xMultiplier;
    protected double yMultiplier;

    private final double zoomCenterX;
    private final double zoomCenterY;

    private final int maxIterations;
    private final double zoom;

    protected final int[] colors;

    protected final IterationStrategy iterator;

    /**
     * Constructs a new ComplexFractalFilter.
     *
     * @param filterName    The name of the filter.
     * @param cxMin         The minimum x boundary in the complex plane.
     * @param cxMax         The maximum x boundary in the complex plane.
     * @param cyMin         The minimum y boundary in the complex plane.
     * @param cyMax         The maximum y boundary in the complex plane.
     * @param iterator      The iteration strategy for the fractal.
     * @param zoom          The zoom level for the fractal.
     * @param zoomCenterX   The x-coordinate of the center point for zooming.
     * @param zoomCenterY   The y-coordinate of the center point for zooming.
     * @param maxIterations The maximum number of iterations for the escape time algorithm.
     * @param colors        The color palette used for rendering.
     */
    protected ComplexFractalFilter(String filterName,
                                   double cxMin, double cxMax,
                                   double cyMin, double cyMax,
                                   IterationStrategy iterator,
                                   double zoom,
                                   double zoomCenterX,
                                   double zoomCenterY,
                                   int maxIterations,
                                   int[] colors) {
        super(filterName);

        this.cxMin = cxMin;
        this.cxMax = cxMax;
        this.cyMin = cyMin;
        this.cyMax = cyMax;

        this.cxRange = cxMax - cxMin;
        this.cyRange = cyMax - cyMin;

        this.iterator = iterator;
        this.zoom = zoom;
        this.zoomCenterX = zoomCenterX;
        this.zoomCenterY = zoomCenterY;
        this.maxIterations = maxIterations;
        this.colors = colors;
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
     * A strategy interface for different fractal iteration algorithms.
     */
    public interface IterationStrategy {
        double ESCAPE_RADIUS_SQ = 4.0; // the squared escape radius

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
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double x2 = 0;
            double y2 = 0;
            double xy;
            while (x2 + y2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                xy = zx * zy;
                x2 = zx * zx;
                y2 = zy * zy;
                zx = x2 - y2 + cx;
                zy = xy + xy + cy;
            }
            return it;
        }

        // an unoptimized version of the iterate method, kept for reference to show the algorithm more clearly
        @SuppressWarnings("unused")
        public int iterateReference(double x, double y, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (x * x + y * y < ESCAPE_RADIUS_SQ && it > 0) {
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
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // this implements z_n+1 = (|Re(z_n)| + i*|Im(z_n)|)^2 + c
                zx = Math.abs(zx);
                zy = Math.abs(zy);

                double xTmp = zx * zx - zy * zy + cx;
                zy = 2.0 * zx * zy + cy;
                zx = xTmp;
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
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // use the conjugate of z, which means negating the
                // imaginary part before calculating the next step
                double xTmp = zx * zx - zy * zy + cx;
                zy = -2.0 * zx * zy + cy; // the only change is the minus sign here
                zx = xTmp;
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
            safeRadiusSq = calcSafeMultibrotRadiusSq(3);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // calculate z^3 + c
                // z^3 = (zx + i*zy)^3 = (zx^3 - 3*zx*zy^2) + i*(3*zx^2*zy - zy^3)
                double zx2 = zx * zx;
                double zy2 = zy * zy;

                double nextZx = zx * (zx2 - 3 * zy2) + cx;
                double nextZy = zy * (3 * zx2 - zy2) + cy;

                zx = nextZx;
                zy = nextZy;
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
            safeRadiusSq = calcSafeMultibrotRadiusSq(4);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // calculate z^4 + c by computing z^4 = (z^2)^2
                // first, z^2
                double z2Re = zx * zx - zy * zy;
                double z2Im = 2 * zx * zy;

                // then, (z^2)^2
                double nextZx = z2Re * z2Re - z2Im * z2Im + cx;
                double nextZy = 2 * z2Re * z2Im + cy;

                zx = nextZx;
                zy = nextZy;
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
            safeRadiusSq = calcSafeMultibrotRadiusSq(5);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            while (zx * zx + zy * zy <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // calculate z^5 + c by computing z^5 = z^4 * z = (z^2)^2 * z
                // first, z^2
                double z2Re = zx * zx - zy * zy;
                double z2Im = 2 * zx * zy;

                // then, z^4 = (z^2)^2
                double z4Re = z2Re * z2Re - z2Im * z2Im;
                double z4Im = 2 * z2Re * z2Im;

                // finally, z^5 = z^4 * z
                double nextZx = z4Re * zx - z4Im * zy + cx;
                double nextZy = z4Re * zy + z4Im * zx + cy;

                zx = nextZx;
                zy = nextZy;
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
    private static double calcSafeMultibrotRadiusSq(int d) {
        double r = (1.0 - 1.0 / d) * FastMath.pow(1.0 / d, 1.0 / (d - 1.0));
        return r * r;
    }
}
