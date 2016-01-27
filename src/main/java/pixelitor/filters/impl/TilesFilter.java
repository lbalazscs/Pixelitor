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

import com.jhlabs.image.TransformFilter;
import net.jafama.FastMath;

import java.awt.image.BufferedImage;

/**
 * Tile filter - inspired by the Paint.net tile effect
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
        float i = x - halfWidth;
        float j = y - halfHeight;

        float sampleX = (float) (i + (curvatureX * FastMath.tan(i * sizeX - shiftX/(double)sizeX)));
        float sampleY = (float) (j + (curvatureY * FastMath.tan(j * sizeY - shiftY/(double)sizeY)));

        out[0] = halfWidth + sampleX;
        out[1] = halfHeight + sampleY;
    }

    public void setShiftX(float shiftX) {
        this.shiftX = shiftX;
    }

    public void setShiftY(float shiftY) {
        this.shiftY = shiftY;
    }
}
