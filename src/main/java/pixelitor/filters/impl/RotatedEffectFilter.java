/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.TransformFilter;
import net.jafama.FastMath;

import java.awt.image.BufferedImage;

public abstract class RotatedEffectFilter extends TransformFilter {
    private float halfWidth;
    private float halfHeight;

    private double angle;
    private double sin = 0;
    private double cos = 1;


    protected RotatedEffectFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        halfWidth = src.getWidth() / 2.0f;
        halfHeight = src.getHeight() / 2.0f;

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double i = x - halfWidth;
        double j = y - halfHeight;

        double ii, jj;
        if (angle != 0) {
            // rotate the sampling coordinates around the center
            ii = i * cos - j * sin;
            jj = j * cos + i * sin;
        } else {
            ii = i;
            jj = j;
        }

        // not a field, because this is multithreaded
        double[] twoDoubles = new double[2];

        transformInverseUnRotated(ii, jj, twoDoubles);
        double sampleX = twoDoubles[0];
        double sampleY = twoDoubles[1];

        // So far we have rotated both the effect and the background.
        // Now rotate the background back.
        double sampleXX, sampleYY;
        if (angle != 0) {
            sampleXX = sampleX * cos + sampleY * sin;
            sampleYY = sampleY * cos - sampleX * sin;
        } else {
            sampleXX = sampleX;
            sampleYY = sampleY;
        }

        out[0] = (float) (halfWidth + sampleXX);
        out[1] = (float) (halfHeight + sampleYY);
    }

    protected abstract void transformInverseUnRotated(double x, double y, double[] out);

//    protected abstract double transformX(double ii, double jj);
//
//    protected abstract double transformY(double ii, double jj);

    public void setAngle(double angle) {
        this.angle = angle;
        if (angle != 0) {
            cos = FastMath.cos(angle);
            sin = FastMath.sin(angle);
        }
    }
}
