/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
 *
 */
public class Sphere3DFilter extends CenteredTransformFilter {
    private float alpha;
    private float beta;
    private float gamma;

    public Sphere3DFilter() {
        super(Sphere3D.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double theta = FastMath.atan2(dy, dx) + Math.PI;
        double rd = 0.45 * Math.min(srcWidth, srcHeight);

//        System.out.println(String.format("Sphere3DFilter::transformInverse: r = %.2f, rd = %.2f", r, rd));

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
//        double theta1 = Math.atan(-x1 / y1) + (y1 > 0 ? Math.PI / 2 : 3 * Math.PI / 2);
        double theta1 = FastMath.atan(-x1 / y1);
        double phi1 = FastMath.asin(z1);

        int X = srcWidth / 2;
        int Y = srcHeight / 2;

//        out[0] = Math.abs((float) (((theta1 * 2 + gamma) % (2 * Math.PI) - Math.PI) / Math.PI * X));
//        out[1] = Math.abs((float) (-phi1 / (Math.PI / 2) * Y));

        out[0] = (float) ((((((theta1 * 2) + gamma) % (2 * Math.PI)) - Math.PI) / Math.PI) * X);
        out[1] = (float) (-phi1 / (Math.PI / 2) * Y);


//        System.out.println(String.format("Sphere3DFilter::transformInverse: out[0] = %.2f, out[1] = %.2f", out[0], out[1]));
    }

    public void setAlpha(float alpha) {
        this.alpha = (float) (2 * Math.PI * alpha);
    }

    public void setBeta(float beta) {
        this.beta = (float) (2 * Math.PI * beta);
    }

    public void setGamma(float gamma) {
        this.gamma = (float) (2 * Math.PI * gamma);
    }
}
