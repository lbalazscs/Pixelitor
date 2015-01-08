/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.RangeWithColorsParam;
import pixelitor.filters.levels.RGBLookup;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ShortLookupTable;

/**
 *
 */
public class ColorBalance extends FilterWithParametrizedGUI {

    private static final int EVERYTHING = 0;
    private static final int SHADOWS = 1;
    private static final int MIDTONES = 2;
    private static final int HIGHLIGHTS = 4;

    private static final int LUT_TABLE_SIZE = 256;

    private final IntChoiceParam affectParam = new IntChoiceParam("Affect", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Everything", EVERYTHING),
            new IntChoiceParam.Value("Shadows", SHADOWS),
            new IntChoiceParam.Value("Midtones", MIDTONES),
            new IntChoiceParam.Value("Highlights", HIGHLIGHTS),
    });

    private final RangeParam cyanRed = new RangeWithColorsParam(Color.CYAN, Color.RED, "Cyan-Red", -100, 100, 0);
    private final RangeParam magentaGreen = new RangeWithColorsParam(Color.MAGENTA, Color.GREEN, "Magenta-Green", -100, 100,
            0);
    private final RangeParam yellowBlue = new RangeWithColorsParam(Color.YELLOW, Color.BLUE, "Yellow-Blue", -100, 100,
            0);

    public ColorBalance() {
        super("Color Balance", true, false);
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

        float[] affectFactor = new float[LUT_TABLE_SIZE];
        for (int i = 0; i < LUT_TABLE_SIZE; i++) {
            switch (affect) {
                case EVERYTHING:
                    affectFactor[i] = 1.0f;
                    break;
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
            }
        }

        short[] redMapping = new short[LUT_TABLE_SIZE];
        short[] greenMapping = new short[LUT_TABLE_SIZE];
        short[] blueMapping = new short[LUT_TABLE_SIZE];

        if (affect == EVERYTHING) {
            for (short i = 0; i < LUT_TABLE_SIZE; i++) {
                short r = (short) (i + cr - (mg / 2) - (yb / 2));
                r = ImageUtils.limitTo8Bits(r);
                redMapping[i] = r;

                short g = (short) (i + mg - (cr / 2) - (yb / 2));
                g = ImageUtils.limitTo8Bits(g);
                greenMapping[i] = g;

                short b = (short) (i + yb - (mg / 2) - (cr / 2));
                b = ImageUtils.limitTo8Bits(b);
                blueMapping[i] = b;
            }
        } else {
            for (short i = 0; i < LUT_TABLE_SIZE; i++) {
                short r = (short) (i + affectFactor[i] * (cr - (mg / 2) - (yb / 2)));
                r = ImageUtils.limitTo8Bits(r);
                redMapping[i] = r;

                short g = (short) (i + affectFactor[i] * (mg - (cr / 2) - (yb / 2)));
                g = ImageUtils.limitTo8Bits(g);
                greenMapping[i] = g;

                short b = (short) (i + affectFactor[i] * (yb - (mg / 2) - (cr / 2)));
                b = ImageUtils.limitTo8Bits(b);
                blueMapping[i] = b;
            }

        }

        RGBLookup rgbLookup = new RGBLookup(redMapping, greenMapping, blueMapping);
        BufferedImageOp filterOp = new FastLookupOp((ShortLookupTable) rgbLookup.getLookupOp());
        filterOp.filter(src, dest);

        return dest;
    }
}
