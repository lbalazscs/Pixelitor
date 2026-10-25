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

import net.jafama.FastMath;
import pixelitor.filters.util.ComplexBounds;

import java.awt.geom.Point2D;

/**
 * A common superclass for the Mandelbrot and Julia fractal implementations.
 */
public abstract class EscapeTimeFilter extends ComplexPlaneFilter {
    private final int maxIterations;
    private final boolean insideOut;

    protected final int[] palette;

    protected final IterationStrategy iterator;

    /**
     * Constructs a new EscapeTimeFilter.
     *
     * @param filterName    The name of the filter.
     * @param bounds        The rectangular bounds in the complex plane.
     * @param iterator      The iteration strategy for the fractal.
     * @param zoom          The zoom level for the fractal.
     * @param zoomCenter    The relative coordinates of the center point for
     *                      zooming (in the unit square [0, 1] x [0, 1]).
     * @param maxIterations The maximum number of iterations for the escape time algorithm.
     * @param palette       The color palette used for rendering.
     * @param insideOut     True to invert the coordinates using f(w) = 1/w, false otherwise.
     */
    protected EscapeTimeFilter(String filterName,
                               ComplexBounds bounds,
                               IterationStrategy iterator,
                               double zoom,
                               Point2D zoomCenter,
                               int maxIterations,
                               int[] palette,
                               boolean insideOut) {
        super(filterName, bounds, zoom, zoomCenter, true);

        this.iterator = iterator;
        this.maxIterations = maxIterations;
        this.palette = palette;
        this.insideOut = insideOut;
    }

    @Override
    public final int processPixel(int x, int y, int rgb) {
        double re = xStart + x * xMultiplier;
        double im = yStart + y * yMultiplier;

        if (insideOut) {
            double d = re * re + im * im;
            if (d == 0) {
                // origin inverts to infinity => escapes immediately
                return palette[palette.length - 1];
            }
            re /= d;
            im = -im / d;
        }

        return calcColor(re, im);
    }

    /**
     * Calculates the color for a point in the complex plane.
     *
     * @param re the real coordinate (already inverted if insideOut is true)
     * @param im the imaginary coordinate (already inverted if insideOut is true)
     * @return the ARGB color for the pixel
     */
    protected abstract int calcColor(double re, double im);

    /**
     * Calculates the color for a point based on its escape time.
     */
    protected int calcIteratedColor(double zx, double zy, double cx, double cy) {
        int it = iterator.iterate(zx, zy, cx, cy, maxIterations);
        return palette[it];
    }

    /**
     * A strategy interface for different fractal iteration algorithms.
     */
    public interface IterationStrategy {
        double ESCAPE_RADIUS_SQ = 4.0; // the squared escape radius

        // magnet fractals have two attractors: the fixed point z = 1 and infinity
        double MAGNET_BAILOUT_SQ = 100.0 * 100.0; // |z| > 100 => heading to infinity
        double MAGNET_CONVERGENCE_SQ = 1.0e-12;   // |z - 1| < 1e-6 => converged to 1

        /**
         * Iterates a fractal formula for a given point until it escapes or the
         * iteration budget runs out.
         *
         * @return the number of iterations *remaining* when the point escapes
         *         (so a point that escapes almost immediately returns a value
         *         close to maxIterations), or 0 if the point never escapes
         *         (i.e. it's considered part of the set)
         */
        int iterate(double zx, double zy, double cx, double cy, int maxIterations);

        /**
         * Checks for regions that are known to be part of the set, allowing an early exit.
         */
        default boolean checkShortcuts(double cx, double cy) {
            return false; // no shortcut by default
        }

        /**
         * Returns the bounds in the complex plane for this fractal.
         */
        ComplexBounds getBounds();
    }

    /**
     * Implements the classic Mandelbrot iteration, z = z² + c.
     */
    public static class MandelbrotStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;

            double zx2 = zx * zx;
            double zy2 = zy * zy;
            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                double xy = zx * zy;
                zx = zx2 - zy2 + cx;
                zy = xy + xy + cy;
                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            // check if the point is inside the main cardioid
            if (cx > -0.75 && cx < 0.37 && cy < 0.65 && cy > -0.65) { // bounding-box check
                double cm = cx - 0.25;
                double cy2 = cy * cy;
                double q = cm * cm + cy2;
                if (q * (q + cm) < cy2 * 0.25) { // exact check
                    return true; // point is in the set
                }
            }

            // check if the point is in the period-2 bulb
            if (cx < -0.75 && cx > -1.25 && cy < 0.28 && cy > -0.28) { // bounding-box check
                if ((cx + 1) * (cx + 1) + cy * cy < 0.0625) { // exact check
                    return true; // point is in the set
                }
            }

            return false; // point is not in a known region
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 0.7, -1.2, 1.2);
        }
    }

    /**
     * Implements the cubic Multibrot iteration, z = z³ + c.
     */
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
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // calculate z^3 + c
                // z^3 = (zx + i*zy)^3 = (zx^3 - 3*zx*zy^2) + i*(3*zx^2*zy - zy^3)
                double nextZx = zx * (zx2 - 3 * zy2) + cx;
                double nextZy = zy * (3 * zx2 - zy2) + cy;

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.5, 1.5, -1.5, 1.5);
        }
    }

    /**
     * Implements the quartic Multibrot iteration, z = z⁴ + c.
     */
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
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // calculate z^4 + c by computing z^4 = (z^2)^2
                // first, z^2
                double z2Re = zx2 - zy2;
                double z2Im = 2 * zx * zy;

                // then, (z^2)^2
                double nextZx = z2Re * z2Re - z2Im * z2Im + cx;
                double nextZy = 2 * z2Re * z2Im + cy;

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.5, 1.0, -1.5, 1.5);
        }
    }

    /**
     * Implements the quintic Multibrot iteration, z = z⁵ + c.
     */
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
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // calculate z^5 + c by computing z^5 = z^4 * z = (z^2)^2 * z
                // first, z^2
                double z2Re = zx2 - zy2;
                double z2Im = 2 * zx * zy;

                // then, z^4 = (z^2)^2
                double z4Re = z2Re * z2Re - z2Im * z2Im;
                double z4Im = 2 * z2Re * z2Im;

                // finally, z^5 = z^4 * z
                double nextZx = z4Re * zx - z4Im * zy + cx;
                double nextZy = z4Re * zy + z4Im * zx + cy;

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.0, 1.0, -1.0, 1.0);
        }
    }

    /**
     * Implements the Tricorn iteration, z = conj(z)² + c.
     */
    public static class TricornStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // use the conjugate of z
                double xTmp = zx2 - zy2 + cx;
                zy = -2.0 * zx * zy + cy;
                zx = xTmp;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.5, 2.5, -2.5, 2.5);
        }
    }

    /**
     * Implements the cubic Multicorn iteration, z = conj(z)³ + c.
     * It is the cubic generalization of the Tricorn (4-fold symmetry).
     */
    public static class MulticornStrategy3 implements IterationStrategy {
        private final double safeRadiusSq;

        public MulticornStrategy3() {
            safeRadiusSq = calcSafeMultibrotRadiusSq(3);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            // conjugation preserves |z|, so |f(z)| = |z|³ + |c| bounds the
            // orbit exactly as for the Multibrot, and the same safe disk applies
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // conj(z)^3 = (zx - i*zy)^3 = (zx^3 - 3*zx*zy^2) - i*(3*zx^2*zy - zy^3)
                double nextZx = zx * (zx2 - 3 * zy2) + cx;
                double nextZy = -zy * (3 * zx2 - zy2) + cy;

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.3, 1.3, -1.3, 1.3);
        }
    }

    /**
     * Implements the quartic Multicorn iteration, z = conj(z)⁴ + c.
     * It is the quartic generalization of the Tricorn (5-fold symmetry).
     */
    public static class MulticornStrategy4 implements IterationStrategy {
        private final double safeRadiusSq;

        public MulticornStrategy4() {
            safeRadiusSq = calcSafeMultibrotRadiusSq(4);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // conj(z)^4 = conj(z^4), so compute z^4 = (z^2)^2 and negate the imaginary part
                double z2Re = zx2 - zy2;
                double z2Im = 2 * zx * zy;

                double nextZx = z2Re * z2Re - z2Im * z2Im + cx;
                double nextZy = -2 * z2Re * z2Im + cy; // the minus sign is the conjugation

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.5, 1.2, -1.4, 1.4);
        }
    }

    /**
     * Implements the Burning Ship iteration, z = (|Re(z)| + i|Im(z)|)² + c.
     */
    public static class BurningShipStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                zx = Math.abs(zx);
                zy = Math.abs(zy);

                double xTmp = zx2 - zy2 + cx;
                zy = 2.0 * zx * zy + cy;
                zx = xTmp;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 1.2, -2.0, 1.0);
        }
    }

    /**
     * Implements the cubic Burning Ship iteration, z = (|Re(z)| + i|Im(z)|)³ + c.
     */
    public static class BurningShipStrategy3 implements IterationStrategy {
        private final double safeRadiusSq;

        public BurningShipStrategy3() {
            safeRadiusSq = calcSafeMultibrotRadiusSq(3);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            // taking absolute values of the components preserves |z|
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // with a = |zx| and b = |zy|: (a + i*b)^3 = (a^3 - 3*a*b^2) + i*(3*a^2*b - b^3)
                // and since a^2 = zx2 and b^2 = zy2, only the odd factors need Math.abs
                double nextZx = Math.abs(zx) * (zx2 - 3 * zy2) + cx;
                double nextZy = Math.abs(zy) * (3 * zx2 - zy2) + cy;

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.6, 1.2, -1.3, 1.6);
        }
    }

    /**
     * Implements the quartic Burning Ship iteration, z = (|Re(z)| + i|Im(z)|)⁴ + c.
     */
    public static class BurningShipStrategy4 implements IterationStrategy {
        private final double safeRadiusSq;

        public BurningShipStrategy4() {
            safeRadiusSq = calcSafeMultibrotRadiusSq(4);
        }

        @Override
        public boolean checkShortcuts(double cx, double cy) {
            return cx * cx + cy * cy < safeRadiusSq;
        }

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // (|zx| + i|zy|)^4 = ((|zx| + i|zy|)^2)^2
                // first, the square: its real part is the usual zx^2 - zy^2,
                // while its imaginary part is 2|zx||zy| = 2|zx*zy|
                double z2Re = zx2 - zy2;
                double z2Im = 2 * Math.abs(zx * zy);

                // then, square it again
                double nextZx = z2Re * z2Re - z2Im * z2Im + cx;
                double nextZy = 2 * z2Re * z2Im + cy;

                zx = nextZx;
                zy = nextZy;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.5, 1.2, -1.1, 1.5);
        }
    }

    /**
     * Implements the Celtic iteration, z = |Re(z²)| + i*Im(z²) + c.
     */
    public static class CelticStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                double xy = zx * zy;
                zx = Math.abs(zx2 - zy2) + cx;
                zy = xy + xy + cy;
                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 0.7, -2.0, 2.0);
        }
    }

    /**
     * Implements the Celtic Mandelbar iteration, z = |Re(conj(z)²)| + i*Im(conj(z)²) + c.
     * It combines the Celtic fold with the conjugation of the Tricorn.
     */
    public static class CelticMandelbarStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // conj(z)^2 = (zx^2 - zy^2) - 2i*zx*zy, then fold the real part
                double xTmp = Math.abs(zx2 - zy2) + cx;
                zy = -2.0 * zx * zy + cy;
                zx = xTmp;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 0.7, -1.1, 1.1);
        }
    }

    /**
     * Implements the Buffalo fractal iteration, z = |Re(z²)| + i*2|Re(z)*Im(z)| + c.
     */
    public static class BuffaloStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                double xy = zx * zy;
                zx = Math.abs(zx2 - zy2) + cx;
                zy = 2.0 * Math.abs(xy) + cy;
                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 0.8, -2.0, 0.8);
        }
    }

    /**
     * Implements the Heart Mandelbrot iteration, z = (|Re(z)| + i*Im(z))² + c.
     * Only the real part is folded before squaring, which breaks the
     * symmetry of the Mandelbrot set across the real axis.
     */
    public static class HeartMandelbrotStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                // (|zx| + i*zy)^2 = (zx^2 - zy^2) + 2i*|zx|*zy
                double xTmp = zx2 - zy2 + cx;
                zy = 2.0 * Math.abs(zx) * zy + cy;
                zx = xTmp;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 0.7, -0.7, 0.7);
        }
    }

    /**
     * Implements the Perpendicular Mandelbrot iteration, z = (|Re(z)| - i*Im(z))² + c.
     */
    public static class PerpendicularMandelbrotStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                double xTmp = zx2 - zy2 + cx;
                zy = -2.0 * Math.abs(zx) * zy + cy;
                zx = xTmp;
                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 1.2, -1.4, 1.4);
        }
    }

    /**
     * Implements the Perpendicular Burning Ship iteration, z = (Re(z) - i*|Im(z)|)² + c.
     */
    public static class PerpendicularBurningShipStrategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            while (zx2 + zy2 <= ESCAPE_RADIUS_SQ && it > 0) {
                it--;
                double xTmp = zx2 - zy2 + cx;
                zy = -2.0 * zx * Math.abs(zy) + cy;
                zx = xTmp;
                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-2.2, 1.2, -1.5, 1.5);
        }
    }

    /**
     * Implements the Magnet Type I iteration, z = ((z² + c - 1) / (2z + c - 2))².
     * <p>
     * The map has two attractors: the superattracting fixed point z = 1 and
     * infinity. The iteration stops when the orbit reaches either of them, so
     * the colors show how quickly that happens, and the points that reach
     * neither (the Magnet set itself) are drawn as part of the set.
     */
    public static class Magnet1Strategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            // the real parts of the constants c - 1 and c - 2
            // (their imaginary part is just cy)
            double cm1x = cx - 1.0;
            double cm2x = cx - 2.0;

            // a NaN (from a pole) fails the comparison, so it ends the loop like an escape
            while (zx2 + zy2 <= MAGNET_BAILOUT_SQ && it > 0) {
                double dx = zx - 1.0;
                if (dx * dx + zy2 < MAGNET_CONVERGENCE_SQ) {
                    break; // converged to the fixed point z = 1
                }
                it--;

                // numerator: z^2 + (c - 1)
                double numRe = zx2 - zy2 + cm1x;
                double numIm = 2.0 * zx * zy + cy;
                // denominator: 2z + (c - 2)
                double denRe = 2.0 * zx + cm2x;
                double denIm = 2.0 * zy + cy;

                // quotient = num * conj(den) / |den|^2
                double denSq = denRe * denRe + denIm * denIm;
                double qRe = (numRe * denRe + numIm * denIm) / denSq;
                double qIm = (numIm * denRe - numRe * denIm) / denSq;

                // the result is the square of the quotient
                zx = qRe * qRe - qIm * qIm;
                zy = 2.0 * qRe * qIm;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.5, 4.0, -2.7, 2.7);
        }
    }

    /**
     * Implements the Magnet Type II iteration,
     * z = ((z³ + 3(c-1)z + (c-1)(c-2)) / (3z² + 3(c-2)z + (c-1)(c-2) + 1))².
     * <p>
     * Like Type I, it has the attractors z = 1 and infinity, and the
     * iteration stops at whichever one the orbit reaches first.
     */
    public static class Magnet2Strategy implements IterationStrategy {
        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;
            double zx2 = zx * zx;
            double zy2 = zy * zy;

            // the real parts of c - 1 and c - 2 (their imaginary part is cy)
            double cm1x = cx - 1.0;
            double cm2x = cx - 2.0;
            // (c - 1)(c - 2) = (cm1x + i*cy)(cm2x + i*cy)
            double abRe = cm1x * cm2x - cy * cy;
            double abIm = cy * (cm1x + cm2x);

            while (zx2 + zy2 <= MAGNET_BAILOUT_SQ && it > 0) {
                double dx = zx - 1.0;
                if (dx * dx + zy2 < MAGNET_CONVERGENCE_SQ) {
                    break; // converged to the fixed point z = 1
                }
                it--;

                // z^2 and z^3
                double z2Re = zx2 - zy2;
                double z2Im = 2.0 * zx * zy;
                double z3Re = z2Re * zx - z2Im * zy;
                double z3Im = z2Re * zy + z2Im * zx;

                // numerator: z^3 + 3(c-1)z + (c-1)(c-2)
                double numRe = z3Re + 3.0 * (cm1x * zx - cy * zy) + abRe;
                double numIm = z3Im + 3.0 * (cm1x * zy + cy * zx) + abIm;
                // denominator: 3z^2 + 3(c-2)z + (c-1)(c-2) + 1
                double denRe = 3.0 * z2Re + 3.0 * (cm2x * zx - cy * zy) + abRe + 1.0;
                double denIm = 3.0 * z2Im + 3.0 * (cm2x * zy + cy * zx) + abIm;

                // quotient = num * conj(den) / |den|^2
                double denSq = denRe * denRe + denIm * denIm;
                double qRe = (numRe * denRe + numIm * denIm) / denSq;
                double qIm = (numIm * denRe - numRe * denIm) / denSq;

                // the result is the square of the quotient
                zx = qRe * qRe - qIm * qIm;
                zy = 2.0 * qRe * qIm;

                zx2 = zx * zx;
                zy2 = zy * zy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-1.0, 3.0, -2.0, 2.0);
        }
    }

    /**
     * Implements the sine iteration, z = sin(z) + c.
     * <p>
     * The additive form is used because the starting point z = 0 (Mandelbrot
     * mode) is a fixed point of c * sin(z), which would make that variant
     * trivial. The Mandelbrot view is periodic in Re(c) with period 2π, so
     * the bounds cover exactly one period.
     * <p>
     * |sin(z)| grows like e^|Im(z)|, so orbits escape vertically. Large real
     * parts are harmless because of the periodicity, so the bailout only
     * looks at |Im(z)|. As with the exponential map, the threshold is a heuristic.
     */
    public static class SineStrategy implements IterationStrategy {
        private static final double BAILOUT_IM = 50.0;

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;

            while (Math.abs(zy) <= BAILOUT_IM && it > 0) {
                it--;
                // sin(z) = sin(zx)*cosh(zy) + i*cos(zx)*sinh(zy)
                // where cosh and sinh are built from a single exp,
                // which can't overflow because |zy| <= BAILOUT_IM
                double e = FastMath.exp(zy);
                double inv = 1.0 / e;
                double cosh = 0.5 * (e + inv);
                double sinh = 0.5 * (e - inv);

                double nextZx = FastMath.sin(zx) * cosh + cx;
                double nextZy = FastMath.cos(zx) * sinh + cy;

                zx = nextZx;
                zy = nextZy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-Math.PI, Math.PI, -2.5, 2.5);
        }
    }

    /**
     * Implements the exponential iteration, z = c * e^z.
     * <p>
     * 0 is the asymptotic value of this map, so starting at z = 0 gives the
     * true parameter plane in Mandelbrot mode.
     * <p>
     * Orbits escape by running off to the right (Re(z) -> +infinity). Large
     * negative real parts are harmless, because e^z then collapses to 0 and
     * the orbit falls back, so a radial bailout would be wrong here. The
     * threshold is a heuristic: an orbit that gets past it could in theory
     * still jump back, but that is extremely rare in practice.
     */
    public static class ExponentialStrategy implements IterationStrategy {
        private static final double BAILOUT_RE = 50.0;

        @Override
        public int iterate(double zx, double zy, double cx, double cy, int maxIt) {
            int it = maxIt;

            // a NaN fails the comparison, so it ends the loop like an escape
            while (zx <= BAILOUT_RE && it > 0) {
                it--;
                // c * e^z = e^zx * (cx + i*cy) * (cos(zy) + i*sin(zy))
                double e = FastMath.exp(zx);
                double cos = FastMath.cos(zy);
                double sin = FastMath.sin(zy);

                double nextZx = e * (cx * cos - cy * sin);
                double nextZy = e * (cx * sin + cy * cos);

                zx = nextZx;
                zy = nextZy;
            }
            return it;
        }

        @Override
        public ComplexBounds getBounds() {
            return new ComplexBounds(-3.0, 5.0, -4.0, 4.0);
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
