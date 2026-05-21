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
 * A filter which draws a four-color gradient using a radial-angular hybrid effect.
 * It calculates an "edge color" using angular interpolation, and then interpolates
 * between a computed center color (at distance = 0) and this edge color
 * (at the maximum distance to the image corners) depending on the pixel's
 * distance from the center.
 */
public class FourColorPolarFilter extends FourColorFilter {
    private static final float EPSILON = 1.0e-6f;

    private int centerA;
    private float centerC1, centerC2, centerC3;

    public FourColorPolarFilter(String filterName,
                                int colorNW, int colorNE, int colorSW, int colorSE,
                                InterpolationType interpolation,
                                ColorSpaceType colorSpace,
                                double relCx, double relCy,
                                int width, int height) {
        super(filterName, colorNW, colorNE, colorSW, colorSE, interpolation, colorSpace, relCx, relCy, width, height);

        setupAngularData(true);
        setupCenterAndRadius();
    }

    private void setupCenterAndRadius() {
        centerA = (int) (calcWeightedCenterAlpha() + 0.5f);
        float[] cCenter = calcWeightedCenterColor();
        centerC1 = cCenter[0];
        centerC2 = cCenter[1];
        centerC3 = cCenter[2];
    }

    private int interpolate(Corner start, Corner end, double angle, double angleStart, double invRange, double distance) {
        float ratioAngle = invRange == 0.0 ? 0.5f : (float) ((angle - angleStart) * invRange);
        float weightAngle = interpolation.calcInterpolatedWeight(ratioAngle);

        float edgeA = ImageMath.lerp(weightAngle, (float) start.a(), (float) end.a());
        float edgeC1 = ImageMath.lerp(weightAngle, start.c1(), end.c1());
        float edgeC2 = ImageMath.lerp(weightAngle, start.c2(), end.c2());
        float edgeC3 = ImageMath.lerp(weightAngle, start.c3(), end.c3());

        float edgeRadius = ImageMath.lerp(weightAngle, (float) start.dist(), (float) end.dist());

        // guard against div by 0 for scenarios where edge radius crashes
        float ratioRadius = edgeRadius <= EPSILON ? 1.0f : Math.min((float) (distance / edgeRadius), 1.0f);
        float weightRadius = interpolation.calcInterpolatedWeight(ratioRadius);

        int finalA = (int) (ImageMath.lerp(weightRadius, (float) centerA, edgeA) + 0.5f);
        float finalC1 = ImageMath.lerp(weightRadius, centerC1, edgeC1);
        float finalC2 = ImageMath.lerp(weightRadius, centerC2, edgeC2);
        float finalC3 = ImageMath.lerp(weightRadius, centerC3, edgeC3);

        return colorSpace.toSrgb(finalA, finalC1, finalC2, finalC3);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double dx = x - cx;
        double dy = y - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // explicit center coordinates (radius is 0)
        if (distance == 0) {
            return colorSpace.toSrgb(centerA, centerC1, centerC2, centerC3);
        }

        double angle = ImageMath.mod(FastMath.atan2(dy, dx), Math.PI * 2.0);

        return switch (calcAngularSegment(angle)) {
            case 0 -> interpolate(corners[0], corners[1], angle, corners[0].angle(), invRange01, distance);
            case 1 -> interpolate(corners[1], corners[2], angle, corners[1].angle(), invRange12, distance);
            case 2 -> interpolate(corners[2], corners[3], angle, corners[2].angle(), invRange23, distance);
            default -> {
                double adjustedAngle = angle < corners[0].angle() ? angle + Math.PI * 2.0 : angle;
                yield interpolate(corners[3], corners[0], adjustedAngle, corners[3].angle(), invRange30, distance);
            }
        };
    }
}
