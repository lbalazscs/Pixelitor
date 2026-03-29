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

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.filters.PolarTiles;

import java.awt.geom.Point2D;

/**
 * The implementation of the {@link PolarTiles} filter.
 */
public class PolarTilesFilter extends CenteredTransformFilter {
    public static final int MODE_CONCENTRIC = 0;
    public static final int MODE_SPIRAL = 1;
    public static final int MODE_VORTEX = 2;

    private final int mode;
    private final int numADivisions;
    private final int numRDivisions;
    private final float curvature;
    private final double effectRotation;
    private final float randomness;
    private final double zoom;
    private final double imageRotation;

    /**
     * Constructs a new PolarTilesFilter.
     *
     * @param filterName     the name of the filter.
     * @param edgeAction     the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation  the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param center         the effect's center (in pixels).
     * @param mode           the tile mode (CONCENTRIC, SPIRAL, or VORTEX).
     * @param numADivisions  the number of angular divisions.
     * @param numRDivisions  the number of radial divisions.
     * @param curvature      the curvature factor of the glass tiles.
     * @param effectRotation the rotation applied to the tile effect (0.0 to 1.0).
     * @param randomness     the amount of random noise displacement (0.0 to 1.0).
     * @param zoom           the image zoom percentage.
     * @param imageRotation  the rotation of the underlying image in radians.
     */
    public PolarTilesFilter(String filterName, int edgeAction, int interpolation, Point2D center,
                            int mode, int numADivisions, int numRDivisions, double curvature,
                            double effectRotation, double randomness, double zoom, double imageRotation) {
        super(filterName, edgeAction, interpolation, center);

        this.mode = mode;
        this.numADivisions = numADivisions;
        this.numRDivisions = numRDivisions;
        this.curvature = (float) (curvature * curvature / 10.0f);
        this.effectRotation = Math.PI * effectRotation;
        this.randomness = (float) (randomness * Math.PI);
        this.zoom = zoom;
        this.imageRotation = imageRotation;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double dx = x - cx;
        double dy = y - cy;
        double angle = FastMath.atan2(dy, dx);

        float randomShift = 0;
        if (randomness > 0) {
            randomShift = randomness * Noise.noise2((float) (dx / width), (float) (dy / height));
        }

        double r = Math.sqrt(dx * dx + dy * dy);
        double rr = r;
        if (mode != MODE_CONCENTRIC) {
            double spiralCorr = width * (Math.PI + angle + effectRotation) / (4 * Math.PI);
            if (mode == MODE_SPIRAL) {
                spiralCorr /= numRDivisions;
            }
            rr += spiralCorr;
        }

        if (numADivisions > 0) {
            double tan = FastMath.tan(randomShift + effectRotation + angle * numADivisions / 2);
            double angleShift = tan * curvature * (numADivisions / 4.0) / r;
            angle += angleShift;
        }

        if (numRDivisions > 0) {
            double tan = FastMath.tan(3 * randomShift + rr / width * 2 * Math.PI * numRDivisions);
            double rShift = tan * numRDivisions * curvature / 2;
            r += rShift;
        }

        angle += imageRotation;

        double zoomedR = r / zoom;
        double u = zoomedR * FastMath.cos(angle);
        double v = zoomedR * FastMath.sin(angle);

        out[0] = (float) (u + cx);
        out[1] = (float) (v + cy);
    }
}
