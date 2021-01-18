/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.RangeWithColorsParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.levels.RGBLookup;

import java.awt.image.BufferedImage;
import java.awt.image.ShortLookupTable;

import static java.awt.Color.*;
import static pixelitor.utils.Texts.i18n;

/**
 * Color balance filter
 */
public class ColorBalance extends ParametrizedFilter {
    public static final String NAME = i18n("color_balance");

    private static final int EVERYTHING = 0;
    private static final int SHADOWS = 1;
    private static final int MIDTONES = 2;
    private static final int HIGHLIGHTS = 4;

    private final IntChoiceParam affect = new IntChoiceParam("Affect", new Item[]{
        new Item("Everything", EVERYTHING),
        new Item("Shadows", SHADOWS),
        new Item("Midtones", MIDTONES),
        new Item("Highlights", HIGHLIGHTS),
    });

    private final RangeParam cyanRed = new RangeWithColorsParam(CYAN, RED,
        i18n("cyan") + "-" + i18n("red"), -100, 0, 100);
    private final RangeParam magentaGreen = new RangeWithColorsParam(MAGENTA, GREEN,
        i18n("magenta") + "-" + i18n("green"), -100, 0, 100);
    private final RangeParam yellowBlue = new RangeWithColorsParam(YELLOW, BLUE,
        i18n("yellow") + "-" + i18n("blue"), -100, 0, 100);

    public ColorBalance() {
        super(ShowOriginal.YES);

        setParams(
            affect,
            cyanRed,
            magentaGreen,
            yellowBlue
        );
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        assert dest == null;

        float cr = cyanRed.getValueAsFloat();
        float mg = magentaGreen.getValueAsFloat();
        float yb = yellowBlue.getValueAsFloat();

        if (cr == 0 && mg == 0 && yb == 0) {
            return src;
        }

        var rgbLookup = new LookupHelper(cr, mg, yb, affect.getValue())
            .getLookup();

        var filterOp = new FastLookupOp(
            (ShortLookupTable) rgbLookup.getLookupOp());

        dest = filterOp.filter(src, null);

        return dest;
    }

    private static class LookupHelper {
        private final float cyanRed;
        private final float magentaGreen;
        private final float yellowBlue;
        private final int affect;

        private static final int LUT_TABLE_SIZE = 256;

        final short[] redMapping = new short[LUT_TABLE_SIZE];
        final short[] greenMapping = new short[LUT_TABLE_SIZE];
        final short[] blueMapping = new short[LUT_TABLE_SIZE];

        public LookupHelper(float cyanRed, float magentaGreen, float yellowBlue, int affect) {
            this.cyanRed = cyanRed;
            this.magentaGreen = magentaGreen;
            this.yellowBlue = yellowBlue;
            this.affect = affect;
        }

        public RGBLookup getLookup() {
            if (affect == EVERYTHING) {
                setupMappingsForTotallyAffected();
            } else {
                setupMappingsForPartiallyAffected();
            }

            return new RGBLookup(redMapping, greenMapping, blueMapping);
        }

        private void setupMappingsForTotallyAffected() {
            for (short i = 0; i < LUT_TABLE_SIZE; i++) {
                short r = (short) (i + cyanRed - magentaGreen / 2 - yellowBlue / 2);
                r = PixelUtils.clamp(r);
                redMapping[i] = r;

                short g = (short) (i + magentaGreen - cyanRed / 2 - yellowBlue / 2);
                g = PixelUtils.clamp(g);
                greenMapping[i] = g;

                short b = (short) (i + yellowBlue - magentaGreen / 2 - cyanRed / 2);
                b = PixelUtils.clamp(b);
                blueMapping[i] = b;
            }
        }

        private void setupMappingsForPartiallyAffected() {
            float[] affectFactor = calcAffectFactor(affect);
            for (short i = 0; i < LUT_TABLE_SIZE; i++) {
                short r = (short) (i + affectFactor[i] * (cyanRed - magentaGreen / 2 - yellowBlue / 2));
                r = PixelUtils.clamp(r);
                redMapping[i] = r;

                short g = (short) (i + affectFactor[i] * (magentaGreen - cyanRed / 2 - yellowBlue / 2));
                g = PixelUtils.clamp(g);
                greenMapping[i] = g;

                short b = (short) (i + affectFactor[i] * (yellowBlue - magentaGreen / 2 - cyanRed / 2));
                b = PixelUtils.clamp(b);
                blueMapping[i] = b;
            }
        }

        private static float[] calcAffectFactor(int affect) {
            float[] affectFactor = new float[LUT_TABLE_SIZE];
            for (int i = 0; i < LUT_TABLE_SIZE; i++) {
                affectFactor[i] = switch (affect) {
                    case SHADOWS -> calcAffectForShadows(i);
                    case HIGHLIGHTS -> calcAffectForHighlights(i);
                    case MIDTONES -> calcAffectForMidtones(i);
                    default -> 1.0f; // should not get here
                };
            }
            return affectFactor;
        }

        private static float calcAffectForMidtones(int i) {
            if (i <= LUT_TABLE_SIZE / 2) {
                return 2.0f * (calcAffectForHighlights(i));
            } else {
                return 2.0f * (calcAffectForShadows(i));
            }
        }

        private static float calcAffectForHighlights(int i) {
            return (1.0f * i) / LUT_TABLE_SIZE;
        }

        private static float calcAffectForShadows(int i) {
            return 1.0f - calcAffectForHighlights(i);
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
