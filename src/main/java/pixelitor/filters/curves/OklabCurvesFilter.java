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

package pixelitor.filters.curves;

import com.jhlabs.image.Curve;
import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.utils.ColorSpaces;

/**
 * A PointFilter that applies curves in the Oklab color space.
 */
class OklabCurvesFilter extends PointFilter {
    /**
     * Domain for the 'a' and 'b' channels of Oklab that comfortably covers the sRGB gamut.
     */
    private static final float AB_CHANNEL_MIN = -0.5f;
    private static final float AB_CHANNEL_MAX = 0.5f;
    private static final float AB_CHANNEL_RANGE = AB_CHANNEL_MAX - AB_CHANNEL_MIN;

    private final int[] lTable;
    private final int[] aTable;
    private final int[] bTable;

    public OklabCurvesFilter(Curve lCurve, Curve aCurve, Curve bCurve) {
        super("Oklab Curves");
        this.lTable = lCurve.makeTable();
        this.aTable = aCurve.makeTable();
        this.bTable = bCurve.makeTable();
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // 1. convert sRGB (int) to Oklab (float[])
        float[] oklab = ColorSpaces.srgbToOklab(rgb);
        float l = oklab[0];
        float a = oklab[1];
        float b = oklab[2];

        // 2. apply curves via lookup tables
        // L channel is normalized to [0, 1]
        int lIndex = ImageMath.clamp((int) (l * 255.0f + 0.5f), 0, 255);
        float newL = lTable[lIndex] / 255.0f;

        // a and b channels are normalized from their specific range
        float newA = applyAbCurve(a, aTable);
        float newB = applyAbCurve(b, bTable);

        // 3. convert back to sRGB
        oklab[0] = newL;
        oklab[1] = newA;
        oklab[2] = newB;
        return ColorSpaces.oklabToSrgb(oklab);
    }

    /**
     * Applies a curve to an Oklab 'a' or 'b' channel value.
     */
    private static float applyAbCurve(float channelValue, int[] table) {
        // normalize the value from its domain to [0, 1] for LUT lookup
        float normalized = (channelValue - AB_CHANNEL_MIN) / AB_CHANNEL_RANGE;
        int index = ImageMath.clamp((int) (normalized * 255.0f + 0.5f), 0, 255);

        // get the transformed value from the LUT and scale it back to [0, 1]
        float transformedNormalized = table[index] / 255.0f;

        // denormalize the value back to the original domain
        return transformedNormalized * AB_CHANNEL_RANGE + AB_CHANNEL_MIN;
    }
}
