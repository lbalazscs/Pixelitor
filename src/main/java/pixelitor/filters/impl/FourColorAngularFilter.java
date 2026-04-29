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

import com.jhlabs.image.FourColorFilter;
import com.jhlabs.image.ImageMath;
import net.jafama.FastMath;

/**
 * A filter which draws a four color gradient using angular interpolation.
 * The midpoint defines the center of the gradient (in polar coordinates).
 * The colors are constant along any given radius from the center, smoothly
 * interpolating between the corners depending on the angle.
 */
public class FourColorAngularFilter extends FourColorFilter {
    public FourColorAngularFilter(String filterName,
                                  int colorNW, int colorNE, int colorSW, int colorSE,
                                  InterpolationType interpolation,
                                  ColorSpaceType colorSpace,
                                  double relCx, double relCy) {
        super(filterName, colorNW, colorNE, colorSW, colorSE, interpolation, colorSpace, relCx, relCy);
    }

    @Override
    public void setDimensions(int width, int height) {
        super.setDimensions(width, height);
        setupAngularData(false);
    }

    private int interpolate(Corner start, Corner end, double angle, double angleStart, double invRange) {
        float ratio = invRange == 0.0 ? 0.5f : (float) ((angle - angleStart) * invRange);
        float weight = interpolation.calcInterpolatedWeight(ratio);

        int aInterp = (int) (ImageMath.lerp(weight, (float) start.a(), (float) end.a()) + 0.5f);
        float c1 = ImageMath.lerp(weight, start.c1(), end.c1());
        float c2 = ImageMath.lerp(weight, start.c2(), end.c2());
        float c3 = ImageMath.lerp(weight, start.c3(), end.c3());

        return colorSpace.toSrgb(aInterp, c1, c2, c3);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double dx = x - cx;
        double dy = y - cy;

        // special case: provide an average out color directly at the explicit center coordinates
        if (dx == 0 && dy == 0) {
            int a = (aNW + aNE + aSW + aSE) / 4;
            float c1 = (cNW[0] + cNE[0] + cSW[0] + cSE[0]) / 4.0f;
            float c2 = (cNW[1] + cNE[1] + cSW[1] + cSE[1]) / 4.0f;
            float c3 = (cNW[2] + cNE[2] + cSW[2] + cSE[2]) / 4.0f;
            return colorSpace.toSrgb(a, c1, c2, c3);
        }

        double angle = ImageMath.mod(FastMath.atan2(dy, dx), Math.PI * 2.0);

        return switch (calcAngularSegment(angle)) {
            case 0 -> interpolate(corners[0], corners[1], angle, corners[0].angle(), invRange01);
            case 1 -> interpolate(corners[1], corners[2], angle, corners[1].angle(), invRange12);
            case 2 -> interpolate(corners[2], corners[3], angle, corners[2].angle(), invRange23);
            default -> {
                double angleAdjusted = angle < corners[0].angle() ? angle + Math.PI * 2.0 : angle;
                yield interpolate(corners[3], corners[0], angleAdjusted, corners[3].angle(), invRange30);
            }
        };
    }
}
