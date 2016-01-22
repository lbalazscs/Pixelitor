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

package pixelitor.filters.lookup;

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.RangeWithColorsParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.filters.levels.RGBLookup;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;

import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GREEN;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;

/**
 * Color balance filter
 */
public class ColorBalance extends FilterWithParametrizedGUI {
    private static final int EVERYTHING = 0;
    private static final int SHADOWS = 1;
    private static final int MIDTONES = 2;
    private static final int HIGHLIGHTS = 4;

    private final IntChoiceParam affectParam = new IntChoiceParam("Affect", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Everything", EVERYTHING),
            new IntChoiceParam.Value("Shadows", SHADOWS),
            new IntChoiceParam.Value("Midtones", MIDTONES),
            new IntChoiceParam.Value("Highlights", HIGHLIGHTS),
    });

    private final RangeParam cyanRed = new RangeWithColorsParam(CYAN, RED, "Cyan-Red", -100, 0, 100);
    private final RangeParam magentaGreen = new RangeWithColorsParam(MAGENTA, GREEN, "Magenta-Green", -100, 0, 100);
    private final RangeParam yellowBlue = new RangeWithColorsParam(YELLOW, BLUE, "Yellow-Blue", -100, 0, 100);

    public ColorBalance() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                affectParam,
                cyanRed,
                magentaGreen,
                yellowBlue
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float cr = cyanRed.getValueAsFloat();
        float mg = magentaGreen.getValueAsFloat();
        float yb = yellowBlue.getValueAsFloat();

        if ((cr == 0) && (mg == 0) && (yb == 0)) {
            return src;
        }

        int affect = affectParam.getValue();

        RGBLookup rgbLookup = new LookupCalculator(cr, mg, yb, affect).getLookup();

        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);

        return dest;
    }

    private static class LookupCalculator {
        private final float cyanRed;
        private final float magentaGreen;
        private final float yellowBlue;
        private final int affect;

        private static final int LUT_TABLE_SIZE = 256;

        final short[] redMapping = new short[LUT_TABLE_SIZE];
        final short[] greenMapping = new short[LUT_TABLE_SIZE];
        final short[] blueMapping = new short[LUT_TABLE_SIZE];

        public LookupCalculator(float cyanRed, float magentaGreen, float yellowBlue, int affect) {
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
                short r = (short) (i + cyanRed - (magentaGreen / 2) - (yellowBlue / 2));
                r = PixelUtils.clamp(r);
                redMapping[i] = r;

                short g = (short) (i + magentaGreen - (cyanRed / 2) - (yellowBlue / 2));
                g = PixelUtils.clamp(g);
                greenMapping[i] = g;

                short b = (short) (i + yellowBlue - (magentaGreen / 2) - (cyanRed / 2));
                b = PixelUtils.clamp(b);
                blueMapping[i] = b;
            }
        }

        private void setupMappingsForPartiallyAffected() {
            float[] affectFactor = calculateAffectFactor(affect);
            for (short i = 0; i < LUT_TABLE_SIZE; i++) {
                short r = (short) (i + affectFactor[i] * (cyanRed - (magentaGreen / 2) - (yellowBlue / 2)));
                r = PixelUtils.clamp(r);
                redMapping[i] = r;

                short g = (short) (i + affectFactor[i] * (magentaGreen - (cyanRed / 2) - (yellowBlue / 2)));
                g = PixelUtils.clamp(g);
                greenMapping[i] = g;

                short b = (short) (i + affectFactor[i] * (yellowBlue - (magentaGreen / 2) - (cyanRed / 2)));
                b = PixelUtils.clamp(b);
                blueMapping[i] = b;
            }
        }

        private static float[] calculateAffectFactor(int affect) {
            float[] affectFactor = new float[LUT_TABLE_SIZE];
            for (int i = 0; i < LUT_TABLE_SIZE; i++) {
                switch (affect) {
                    case SHADOWS:
                        affectFactor[i] = 1.0f - (1.0f * i) / LUT_TABLE_SIZE;
                        break;
                    case HIGHLIGHTS:
                        affectFactor[i] = (1.0f * i) / LUT_TABLE_SIZE;
                        break;
                    case MIDTONES:
                        int halfSize = LUT_TABLE_SIZE / 2;
                        if (i <= halfSize) {
                            affectFactor[i] = (2.0f * i) / LUT_TABLE_SIZE;
                        } else {
                            affectFactor[i] = 2 * (1.0f - (1.0f * i) / LUT_TABLE_SIZE);
                        }
                        break;
                    case EVERYTHING:
                        // should not get here
                        affectFactor[i] = 1.0f;
                        break;
                }
            }
            return affectFactor;
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
