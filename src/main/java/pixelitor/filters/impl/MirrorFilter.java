/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

    private static float PI4 = (float) (FastMath.PI / 4);
    private static float PI2 = (float) (FastMath.PI / 2);

    // ax + by + c = 0
    private double a;
    private double b;
    private double c;
    // a² + b²
    private double aa_bb;
    // for a point present at an angle from cx, cy, this variable is the result if we put that point to the line above.
    private double base_put;

    public MirrorFilter() {
        super(Mirror.NAME);
    }

    public void setAngle(double angle) {

        if ((FastMath.abs(angle) % PI2) < PI4) {

            // y = mx + c    m-> slope
            double slope = FastMath.tan(angle);

            a = slope;
            b = 1;

        } else {

            // y = mx + c    1/m-> oneBySlope
            double oneBySlope = FastMath.tan(PI2 - angle);

            a = 1;
            b = oneBySlope;

        }

        // for a line `a*x + b*y + c = 0` which satisfies (cx, cy)
        // c = -(a*cx + b*cy)
        c = -(a * cx + b * cy);

        aa_bb = a * a + b * b;

        // Just a point (cx, cy-1) rotated about (cx, cy)
        double gx = -FastMath.sin(-angle) + cx;
        double gy = FastMath.cos(-angle) + cy;
        base_put = FastMath.signum(a * gx + b * gy + c);

    }


    @Override
    protected void transformInverse(int x, int y, float[] out) {

        // x₂-x₁     y₂-y₁     -2 * (a*x₁ + b*y₁ + c)
        // -----  =  -----  =  ----------------------
        //   a         b               a² + b²

        double put = a * x + b * y + c;

        if (FastMath.signum(put) == base_put) {
            out[0] = x;
            out[1] = y;
            return;
        }

        // -2 * (a*x₁ + b*y₁ + c)
        double rhs = -2 * (put) / aa_bb;

        // x₂ = x₁ + a * rhs
        out[0] = (float) (x + a * rhs);

        // y₂ = y₁ + b * rhs
        out[1] = (float) (y + b * rhs);

    }
}
