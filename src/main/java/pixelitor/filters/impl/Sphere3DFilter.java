/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.Sphere3D;

/**
 * The implementation of the {@link Sphere3D} filter.
 *
 * Not finished.
 */
public class Sphere3DFilter extends CenteredTransformFilter {
    private double alpha;
    private double beta;
    private double gamma;

    public Sphere3DFilter() {
        super(Sphere3D.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double theta = FastMath.atan2(dy, dx) + Math.PI;
        double rd = 0.45 * Math.min(srcWidth, srcHeight);

        if (r > rd) {
            out[0] = -1;
            out[1] = -1;
            return;
        }

        double sa = FastMath.sin(alpha);
        double sb = FastMath.sin(beta);
        double ca = FastMath.cos(alpha);
        double cb = FastMath.cos(beta);

        double phi = FastMath.acos(r / rd);

        double x0 = FastMath.cos(theta) * FastMath.cos(phi);
        double y0 = FastMath.sin(theta) * FastMath.cos(phi);
        double z0 = FastMath.sin(phi);
        double x1 = ca * x0 + sa * y0;
        double z1 = -sa * -sb * x0 + ca * -sb * y0 + cb * z0;
        double y1 = cb * -sa * x0 + cb * ca * y0 + sb * z0;
        double theta1 = FastMath.atan(-x1 / y1);
        double phi1 = FastMath.asin(z1);

        int X = srcWidth / 2;
        int Y = srcHeight / 2;

        out[0] = (float) ((((((theta1 * 2) + gamma) % (2 * Math.PI)) - Math.PI) / Math.PI) * X);
        out[1] = (float) (-phi1 / (Math.PI / 2) * Y);
    }

    public void setAlpha(double alpha) {
        this.alpha = 2 * Math.PI * alpha;
    }

    public void setBeta(double beta) {
        this.beta = 2 * Math.PI * beta;
    }

    public void setGamma(double gamma) {
        this.gamma = 2 * Math.PI * gamma;
    }
}
