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
import pixelitor.filters.Mirror;

import java.awt.geom.Point2D;

/**
 * The implementation of the {@link Mirror} filter.
 */
public class MirrorFilter extends CenteredTransformFilter {
    private static final float QUARTER_PI = (float) (Math.PI / 4);
    private static final float HALF_PI = (float) (Math.PI / 2);

    // coefficients for the line equation ax + by + c = 0
    private final double a;
    private final double b;
    private final double c;

    // stores a² + b² as a precomputed value
    private final double aa_bb;

    // determines whether a point lies on one side of the reflection line or the other.
    private final double baseSideIndicator;

    /**
     * Constructs a new MirrorFilter.
     *
     * @param filterName    the name of the filter
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT)
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC)
     * @param center        the center of the reflection line (in pixels)
     * @param angle         the angle of the reflection line in radians.
     */
    public MirrorFilter(String filterName, int edgeAction, int interpolation,
                        Point2D center, double angle) {
        super(filterName, edgeAction, interpolation, center);

        if ((Math.abs(angle) % HALF_PI) < QUARTER_PI) {
            // for angles near horizontal, the slope is calculated as tan(−angle)
            this.a = FastMath.tan(-angle);
            this.b = 1;
        } else {
            // for angles near vertical, the slope is calculated using the reciprocal of tan(π/2+angle)
            this.a = 1;
            this.b = FastMath.tan(HALF_PI + angle);
        }

        // the line equation coefficients a, b, c are determined
        // such that the line passes through the center point (cx, cy)
        this.c = -(a * cx + b * cy);

        this.aa_bb = a * a + b * b;

        // (cx, cy−1) is chosen because it simplifies the calculations.
        // (rx, ry) is (cx, cy−1) rotated by the angle around (cx, cy),
        // and it is consistent with the orientation of the reflection line.
        double rx = -FastMath.sin(angle) + cx;
        double ry = FastMath.cos(angle) + cy;
        this.baseSideIndicator = FastMath.signum(a * rx + b * ry + c);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // x₂-x₁     y₂-y₁     -2 * (a*x₁ + b*y₁ + c)
        // -----  =  -----  =  ----------------------
        //   a         b               a² + b²

        // the point's position relative to the line.
        double sideIndicator = a * x + b * y + c;

        if (Math.signum(sideIndicator) == baseSideIndicator) {
            // if the point is on the same side of the line
            // as the base point, it remains unchanged
            out[0] = x;
            out[1] = y;
            return;
        }

        // otherwise compute the reflection of the point (x,y) about the line
        // -2 * (a*x₁ + b*y₁ + c) / (a² + b²)
        double rhs = -2 * sideIndicator / aa_bb;

        // x₂ = x₁ + a * rhs
        out[0] = (float) (x + a * rhs);

        // y₂ = y₁ + b * rhs
        out[1] = (float) (y + b * rhs);
    }
}
