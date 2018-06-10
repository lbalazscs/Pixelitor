/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.GlassTiles;

import java.awt.image.BufferedImage;

/**
 * The implementation of the {@link GlassTiles} filter.
 *
 * Inspired by the Paint.net tile effect
 */
public class TilesFilter extends TransformFilter {
    private float sizeX;
    private float sizeY;
    private float halfWidth;
    private float halfHeight;
    private float curvatureX;
    private float curvatureY;
    private float shiftX;
    private float shiftY;
    private double angle;

    public TilesFilter(String filterName) {
        super(filterName);
    }

    public void setSizeX(int size) {
        this.sizeX = (float) (Math.PI / size);
    }

    public void setSizeY(int size) {
        this.sizeY = (float) (Math.PI / size);
    }

    public void setCurvatureX(float curvature) {
        this.curvatureX = curvature * curvature / 10.0f;
    }

    public void setCurvatureY(float curvature) {
        this.curvatureY = curvature * curvature / 10.0f;
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
        double cos = 1.0;
        double sin = 0.0;
        if (angle != 0) {
            cos = FastMath.cos(angle);
            sin = FastMath.sin(angle);
            ii = i * cos - j * sin;
            jj = j * cos + i * sin;
        } else {
            ii = i;
            jj = j;
        }

        double sampleX = ii + (curvatureX * FastMath.tan(ii * sizeX - shiftX / (double) sizeX));
        double sampleY = jj + (curvatureY * FastMath.tan(jj * sizeY - shiftY / (double) sizeY));

        // So far we have rotated both the tiles
        // distortion and the background.
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

    public void setShiftX(float shiftX) {
        this.shiftX = shiftX;
    }

    public void setShiftY(float shiftY) {
        this.shiftY = shiftY;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }
}
