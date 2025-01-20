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

import net.jafama.FastMath;
import pixelitor.filters.Mirror;

/**
 * The implementation of the {@link Mirror} filter.
 */
public class MirrorFilter extends CenteredTransformFilter {
    private static final float QUARTER_PI = (float) (Math.PI / 4);
    private static final float HALF_PI = (float) (Math.PI / 2);

    // coefficients for the line equation ax + by + c = 0
    private double a;
    private double b;
    private double c;

    // stores a² + b² as a precomputed value
    private double aa_bb;

    // determines whether a point lies on one side of the reflection line or the other.
    private double baseSideIndicator;

    public MirrorFilter() {
        super(Mirror.NAME);
    }

    public void setAngle(double angle) {
        if ((Math.abs(angle) % HALF_PI) < QUARTER_PI) {
            // for angles near horizontal, the slope is calculated as tan(−angle)
            double slope = FastMath.tan(-angle);
            a = slope;
            b = 1;
        } else {
            // for angles near vertical, the slope is calculated using the reciprocal of tan(π/2+angle)
            double reciprocalSlope = FastMath.tan(HALF_PI + angle);
            a = 1;
            b = reciprocalSlope;
        }

        // the line equation coefficients a, b, c are determined
        // such that the line passes through the center point
        c = -(a * cx + b * cy);

        aa_bb = a * a + b * b;

        // (cx, cy−1) is choosen because it simplifies the calculations.
        // (rx, ry) is (cx, cy−1) rotated by the angle around (cx, cy),
        // and it is consistent with the orientation of the reflection line.
        double rx = -FastMath.sin(angle) + cx;
        double ry = FastMath.cos(angle) + cy;
        baseSideIndicator = FastMath.signum(a * rx + b * ry + c);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // x₂-x₁     y₂-y₁     -2 * (a*x₁ + b*y₁ + c)
        // -----  =  -----  =  ----------------------
        //   a         b               a² + b²

        // the point's position relative to the line.
        double sideIndicator = a * x + b * y + c;

        if (FastMath.signum(sideIndicator) == baseSideIndicator) {
            // if the point is on the same side of the line
            // as the base point, it remains unchanged
            out[0] = x;
            out[1] = y;
            return;
        }
        // otherwise compute the reflection of the point (x,y) about the line

        // -2 * (a*x₁ + b*y₁ + c)
        double rhs = -2 * sideIndicator / aa_bb;

        // x₂ = x₁ + a * rhs
        out[0] = (float) (x + a * rhs);

        // y₂ = y₁ + b * rhs
        out[1] = (float) (y + b * rhs);
    }
}
