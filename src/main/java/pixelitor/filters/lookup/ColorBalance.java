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

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.RangeWithColorsParam;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GREEN;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.RED;
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
        super(true);

        cyanRed.setPresetKey("Cyan-Red");
        magentaGreen.setPresetKey("Magenta-Green");
        yellowBlue.setPresetKey("Yellow-Blue");

        initParams(
            affect,
            cyanRed,
            magentaGreen,
            yellowBlue
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        assert dest == null;

        float cr = cyanRed.getValueAsFloat();
        float mg = magentaGreen.getValueAsFloat();
        float yb = yellowBlue.getValueAsFloat();

        if (cr == 0 && mg == 0 && yb == 0) { // no change
            return src;
        }

        var lookup = createLookup(cr, mg, yb, affect.getValue());
        var filterOp = lookup.asFastLookupOp();

        return filterOp.filter(src, null);
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
}
