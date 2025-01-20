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

import com.jhlabs.image.TransformFilter;
import net.jafama.FastMath;

import java.awt.image.BufferedImage;

/**
 * A {@link TransformFilter} that can rotate a distortion effect,
 * in a way that leaves the undistorted pixels unaffected.
 * Not to be confused with {@link CenteredTransformFilter}.
 */
public abstract class RotatedEffectFilter extends TransformFilter {
    private double centerX;
    private double centerY;

    private double angle;
    private double sin = 0;
    private double cos = 1;

    protected RotatedEffectFilter(String filterName) {
        super(filterName);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        centerX = src.getWidth() / 2.0;
        centerY = src.getHeight() / 2.0;

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        double relX = x - centerX;
        double relY = y - centerY;

        double rotatedX, rotatedY;
        if (angle != 0) {
            // rotate the sampling coordinates around the center
            rotatedX = relX * cos - relY * sin;
            rotatedY = relY * cos + relX * sin;
        } else {
            rotatedX = relX;
            rotatedY = relY;
        }

        // not a field, because this is multithreaded
        double[] distorted = new double[2];

        coreTransformInverse(rotatedX, rotatedY, distorted);
        double distortedX = distorted[0];
        double distortedY = distorted[1];

        // rotate back to ensure that only the distortion effect is
        // rotated while keeping the underlying image properly oriented
        double unrotatedX, unrotatedY;
        if (angle != 0) {
            unrotatedX = distortedX * cos + distortedY * sin;
            unrotatedY = distortedY * cos - distortedX * sin;
        } else {
            unrotatedX = distortedX;
            unrotatedY = distortedY;
        }

        out[0] = (float) (centerX + unrotatedX);
        out[1] = (float) (centerY + unrotatedY);
    }

    /**
     * Subclasses implement this to define the inverse transformation.
     */
    protected abstract void coreTransformInverse(double x, double y, double[] out);

    public void setAngle(double angle) {
        this.angle = angle;
        cos = FastMath.cos(angle);
        sin = FastMath.sin(angle);
    }
}
