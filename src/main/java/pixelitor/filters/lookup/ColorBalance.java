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

package pixelitor.filters.lookup;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PixelUtils;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeWithColorsParam;
import pixelitor.filters.util.ColorSpace;
import pixelitor.utils.ColorSpaces;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GREEN;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import static pixelitor.utils.Texts.i18n;

/**
 * Color balance filter
 */
public class ColorBalance extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 8496579363272349016L;

    public static final String NAME = i18n("color_balance");

    private static final int EVERYTHING = 0;
    private static final int SHADOWS = 1;
    private static final int MIDTONES = 2;
    private static final int HIGHLIGHTS = 4;

    private static final int LUT_TABLE_SIZE = 256;
    private static final String CYAN_RED_TEXT = i18n("cyan") + "-" + i18n("red");
    private static final String MAGENTA_GREEN_TEXT = i18n("magenta") + "-" + i18n("green");
    private static final String YELLOW_BLUE_TEXT = i18n("yellow") + "-" + i18n("blue");

    private final EnumParam<ColorSpace> colorSpace = ColorSpace.asParam();

    private final IntChoiceParam affect = new IntChoiceParam("Affect", new Item[]{
        new Item("Everything", EVERYTHING),
        new Item("Shadows", SHADOWS),
        new Item("Midtones", MIDTONES),
        new Item("Highlights", HIGHLIGHTS),
    });

    private final RangeWithColorsParam range1 = new RangeWithColorsParam(CYAN, RED,
        CYAN_RED_TEXT, -100, 0, 100);
    private final RangeWithColorsParam range2 = new RangeWithColorsParam(MAGENTA, GREEN,
        MAGENTA_GREEN_TEXT, -100, 0, 100);
    private final RangeWithColorsParam range3 = new RangeWithColorsParam(YELLOW, BLUE,
        YELLOW_BLUE_TEXT, -100, 0, 100);

    public ColorBalance() {
        super(true);

        range1.setPresetKey("Cyan-Red");
        range2.setPresetKey("Magenta-Green");
        range3.setPresetKey("Yellow-Blue");

        initParams(
            colorSpace,
            affect,
            range1,
            range2,
            range3
        );

        colorSpace.addOnChangeTask(this::updateSliders);
    }

    private void updateSliders() {
        switch (colorSpace.getSelected()) {
            case SRGB -> updateSlidersForSRGB();
            case OKLAB -> updateSlidersForOKLAB();
        }

        range1.updateGUIAppearance(true);
        range2.updateGUIAppearance(true);
        range3.updateGUIAppearance(true);
    }

    private void updateSlidersForSRGB() {
        range1.setName(CYAN_RED_TEXT);
        range1.setLeftColor(CYAN);
        range1.setRightColor(RED);

        range2.setName(MAGENTA_GREEN_TEXT);
        range2.setLeftColor(MAGENTA);
        range2.setRightColor(GREEN);

        range3.setName(YELLOW_BLUE_TEXT);
        range3.setLeftColor(YELLOW);
        range3.setRightColor(BLUE);
    }

    private void updateSlidersForOKLAB() {
        range1.setName("Green-Red (a)");
        range1.setLeftColor(GREEN);
        range1.setRightColor(RED);

        range2.setName("Blue-Yellow (b)");
        range2.setLeftColor(BLUE);
        range2.setRightColor(YELLOW);

        range3.setName("Dark-Light (L)");
        range3.setLeftColor(BLACK);
        range3.setRightColor(WHITE);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        assert dest == null;

        float p1 = range1.getValueAsFloat();
        float p2 = range2.getValueAsFloat();
        float p3 = range3.getValueAsFloat();

        if (p1 == 0 && p2 == 0 && p3 == 0) { // no change
            return src;
        }

        return switch (colorSpace.getSelected()) {
            case SRGB -> {
                var lookup = createLookup(p1, p2, p3, affect.getValue());
                var filterOp = lookup.asFastLookupOp();
                yield filterOp.filter(src, null);
            }
            case OKLAB -> {
                var filter = new OklabPointFilter(p1, p2, p3, affect.getValue());
                yield filter.filter(src, null);
            }
        };
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    private static RGBLookup createLookup(float cyanRed, float magentaGreen, float yellowBlue, int affect) {
        short[] redMapping = new short[LUT_TABLE_SIZE];
        short[] greenMapping = new short[LUT_TABLE_SIZE];
        short[] blueMapping = new short[LUT_TABLE_SIZE];

        setupMappings(redMapping, greenMapping, blueMapping,
            cyanRed, magentaGreen, yellowBlue, affect);

        return new RGBLookup(redMapping, greenMapping, blueMapping);
    }

    private static void setupMappings(short[] redMap, short[] greenMap, short[] blueMap,
                                      float cyanRed, float magentaGreen, float yellowBlue, int affect) {
        // pre-calculate the color adjustments for each channel
        float rAdj = cyanRed - magentaGreen / 2.0f - yellowBlue / 2.0f;
        float gAdj = magentaGreen - cyanRed / 2.0f - yellowBlue / 2.0f;
        float bAdj = yellowBlue - cyanRed / 2.0f - magentaGreen / 2.0f;

        float[] affectFactor = createAffectLut(affect);

        for (int i = 0; i < LUT_TABLE_SIZE; i++) {
            float factor = (affectFactor == null) ? 1.0f : affectFactor[i];

            // calculate the new color value, clamp it to the 0-255 range, and store in the map
            redMap[i] = (short) PixelUtils.clamp((int) (i + rAdj * factor));
            greenMap[i] = (short) PixelUtils.clamp((int) (i + gAdj * factor));
            blueMap[i] = (short) PixelUtils.clamp((int) (i + bAdj * factor));
        }
    }

    private static float[] createAffectLut(int affect) {
        return switch (affect) {
            case EVERYTHING -> null;
            case SHADOWS -> calcAffectLutForShadows();
            case MIDTONES -> calcAffectLutForMidtones();
            case HIGHLIGHTS -> calcAffectLutForHighlights();
            default -> throw new IllegalArgumentException("affect = " + affect);
        };
    }

    // creates a linear ramp from 1.0 to 0.0, affecting shadows more
    private static float[] calcAffectLutForShadows() {
        float[] affectFactor = new float[LUT_TABLE_SIZE];
        for (int i = 0; i < LUT_TABLE_SIZE; i++) {
            affectFactor[i] = 1.0f - ((float) i / (LUT_TABLE_SIZE - 1));
        }
        return affectFactor;
    }

    // creates a triangular ramp that peaks at mid-gray
    private static float[] calcAffectLutForMidtones() {
        float[] affectFactor = new float[LUT_TABLE_SIZE];
        for (int i = 0; i < LUT_TABLE_SIZE; i++) {
            float highlightFactor = (float) i / (LUT_TABLE_SIZE - 1);
            if (i <= LUT_TABLE_SIZE / 2) {
                affectFactor[i] = 2.0f * highlightFactor;
            } else {
                affectFactor[i] = 2.0f * (1.0f - highlightFactor);
            }
        }
        return affectFactor;
    }

    // creates a linear ramp from 0.0 to 1.0, affecting highlights more
    private static float[] calcAffectLutForHighlights() {
        float[] affectFactor = new float[LUT_TABLE_SIZE];
        for (int i = 0; i < LUT_TABLE_SIZE; i++) {
            affectFactor[i] = (float) i / (LUT_TABLE_SIZE - 1);
        }
        return affectFactor;
    }

    private static float getAffectFactor(float l, int affect) {
        return switch (affect) {
            case EVERYTHING -> 1.0f;
            case SHADOWS -> 1.0f - l;
            case MIDTONES -> 1.0f - 2.0f * Math.abs(l - 0.5f);
            case HIGHLIGHTS -> l;
            default -> throw new IllegalArgumentException("affect = " + affect);
        };
    }

    private static class OklabPointFilter extends PointFilter {
        private final float aAdj;
        private final float bAdj;
        private final float lAdj;
        private final int affect;

        public OklabPointFilter(float greenRed, float blueYellow, float darkLight, int affect) {
            super(NAME);
            // scale [-100, 100] to a reasonable adjustment range for Oklab channels
            this.aAdj = greenRed / 1000.0f;   // maps to [-0.1, 0.1]
            this.bAdj = blueYellow / 1000.0f; // maps to [-0.1, 0.1]
            this.lAdj = darkLight / 500.0f;   // maps to [-0.2, 0.2]
            this.affect = affect;
        }

        @Override
        public int processPixel(int x, int y, int rgb) {
            float[] oklab = ColorSpaces.srgbToOklab(rgb);

            float l = oklab[0];
            float affectFactor = getAffectFactor(l, affect);

            // apply adjustments to L, a, b channels
            oklab[0] += lAdj * affectFactor;
            oklab[1] += aAdj * affectFactor;
            oklab[2] += bAdj * affectFactor;

            // clamp lightness to its valid [0, 1] range
            oklab[0] = ImageMath.clamp(oklab[0], 0.0f, 1.0f);

            return ColorSpaces.oklabToSrgb(oklab);
        }
    }
}
