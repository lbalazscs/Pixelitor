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

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.filters.PolarTiles;

/**
 *
 */
public class PolarTilesFilter extends CenteredTransformFilter {
    private float zoom;
    private double rotateResult;
    private float curvature;

    private double t;
    private int numADivisions;
    private int numRDivisions;
    private float randomness;

    public PolarTilesFilter() {
        super(PolarTiles.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        double r = Math.sqrt(dx * dx + dy * dy);
        double angle = FastMath.atan2(dy, dx);

        float randomShift = 0;
        if (randomness > 0) {
//            randomShift = randomness * Noise.noise2((float) angle, (float) (2.0f * r / srcWidth));
            randomShift = randomness * Noise.noise2(dx / srcWidth, dy / srcHeight);
        }

        if (numADivisions > 0) {
            double angleShift = FastMath.tan(randomShift + t + angle * numADivisions / 2) * curvature * (numADivisions / 4.0) / r;
            angle += angleShift;
        }

        if (numRDivisions > 0) {
            double rShift = FastMath.tan(3 * randomShift + r / srcWidth * 2 * Math.PI * numRDivisions) * numRDivisions * curvature / 2;
            r += rShift;
        }

        angle += rotateResult;

        double zoomedR = r / zoom;
        float u = (float) (zoomedR * FastMath.cos(angle));
        float v = (float) (zoomedR * FastMath.sin(angle));

        out[0] = (u + cx);
        out[1] = (v + cy);
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setRotateResult(double rotateResult) {
        this.rotateResult = rotateResult;
    }

    public void setT(double t) {
        this.t = Math.PI * t;
    }

    public void setNumADivisions(int numADivisions) {
        this.numADivisions = numADivisions;
    }

    public void setNumRDivisions(int numRDivisions) {
        this.numRDivisions = numRDivisions;
    }

    public void setCurvature(double curvature) {
        this.curvature = (float) (curvature * curvature / 10.0f);
    }

    public void setRandomness(float randomness) {
        this.randomness = (float) (randomness * Math.PI);
    }
}
