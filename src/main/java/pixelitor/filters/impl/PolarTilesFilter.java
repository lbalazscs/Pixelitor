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

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.filters.PolarTiles;

/**
 * The implementation of the {@link PolarTiles} filter.
 */
public class PolarTilesFilter extends CenteredTransformFilter {
    private float zoom;
    private double rotateResult;
    private float curvature;
    private double rotateEffect;
    private int numADivisions;
    private int numRDivisions;
    private float randomness;

    public static final int MODE_CONCENTRIC = 0;
    public static final int MODE_SPIRAL = 1;
    public static final int MODE_VORTEX = 2;
    private int mode = MODE_CONCENTRIC;

    public PolarTilesFilter() {
        super(PolarTiles.NAME);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        double angle = FastMath.atan2(dy, dx);

        float randomShift = 0;
        if (randomness > 0) {
            randomShift = randomness * Noise.noise2(dx / srcWidth, dy / srcHeight);
        }

        double r = Math.sqrt(dx * dx + dy * dy);
        double rr = r;
        if (mode != MODE_CONCENTRIC) {
            double spiralCorr = srcWidth * (Math.PI + angle + rotateEffect) / (4 * Math.PI);
            if (mode == MODE_SPIRAL) {
                spiralCorr /= numRDivisions;
            }
            rr += spiralCorr;
        }

        if (numADivisions > 0) {
            double tan = FastMath.tan(randomShift + rotateEffect + angle * numADivisions / 2);
            double angleShift = tan * curvature * (numADivisions / 4.0) / r;
            angle += angleShift;
        }

        if (numRDivisions > 0) {
            double tan = FastMath.tan(3 * randomShift + rr / srcWidth * 2 * Math.PI * numRDivisions);
            double rShift = tan * numRDivisions * curvature / 2;
            r += rShift;
        }

        angle += rotateResult;

        double zoomedR = r / zoom;
        float u = (float) (zoomedR * FastMath.cos(angle));
        float v = (float) (zoomedR * FastMath.sin(angle));

        out[0] = u + cx;
        out[1] = v + cy;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setRotateResult(double rotateResult) {
        this.rotateResult = rotateResult;
    }

    public void setRotateEffect(double rotateEffect) {
        this.rotateEffect = Math.PI * rotateEffect;
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

    public void setMode(int mode) {
        this.mode = mode;
    }
}
