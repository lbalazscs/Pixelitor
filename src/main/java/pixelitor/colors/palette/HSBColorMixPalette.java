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

package pixelitor.colors.palette;

import pixelitor.colors.Colors;

import java.awt.Color;

import static com.jhlabs.image.ImageMath.lerp;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * A palette that mixes the foreground color with the background color
 * using the HSB color space to interpolate between them
 */
public class HSBColorMixPalette extends Palette {
    // static palette-specific variables so that they
    // are remembered between dialog sessions
    private static int lastRows = 7;
    private static int lastCols = 10;

    private final boolean startWithFg;

    private float hueA, hueB;
    private final float satA, briA, satB, briB;
    private final float averageSat;
    private float extraSat;

    private static final float MAX_BRI_DEVIATION = 0.5f;

    public HSBColorMixPalette(boolean startWithFg) {
        super(lastRows, lastCols);
        this.startWithFg = startWithFg;

        Color colorA, colorB;
        if (startWithFg) {
            colorA = getFGColor();
            colorB = getBGColor();
        } else {
            colorA = getBGColor();
            colorB = getFGColor();
        }

        float[] hsbA = Colors.toHSB(colorA);
        float[] hsbB = Colors.toHSB(colorB);

        hueA = hsbA[0];
        satA = hsbA[1];
        briA = hsbA[2];
        hueB = hsbB[0];
        satB = hsbB[1];
        briB = hsbB[2];

        // if the saturation is 0, then the hue does not mean anything,
        // but can lead to unexpected hue variations in the mix
        if (satA == 0) {
            hueA = hueB;
        } else if (satB == 0) {
            hueB = hueA;
        }

        // set the average saturation as the slider default
        averageSat = (satA + satB) / 2;
        config = new HueSatPaletteConfig(0.0f, averageSat);
    }

    @Override
    public void configChanged() {
        float configSat = ((HueSatPaletteConfig) config).getSaturation();
        extraSat = configSat - averageSat;
    }

    @Override
    public void addButtons(PalettePanel panel) {
        for (int y = 0; y < numRows; y++) {
            float briStep = calcBriStep();
            for (int x = 0; x < numCols; x++) {
                Color c;
                if (numRows == 1) {
                    float mixFactor = calcMixFactor(x);
                    float h = calcHue(mixFactor);
                    float s = calcSat(mixFactor);
                    float b = lerp(mixFactor, briA, briB);
                    c = new Color(Color.HSBtoRGB(h, s, b));
                } else {
                    float mixFactor = calcMixFactor(x);
                    float h = calcHue(mixFactor);
                    float s = calcSat(mixFactor);
                    float b = lerp(mixFactor, briA, briB);

                    float startBri = b - MAX_BRI_DEVIATION;
                    b = startBri + y * briStep;
                    if (b > 1.0f) {
                        b = 1.0f;
                    } else if (b < 0.0f) {
                        b = 0.0f;
                    }

                    c = new Color(Color.HSBtoRGB(h, s, b));
                }
                panel.addButton(x, y, c);
            }
        }
    }

    private float calcBriStep() {
        // the total bri range (2 * MAX_BRI_DEVIATION) is
        // divided into numRows - 1 equal parts
        return 2 * MAX_BRI_DEVIATION / (numRows - 1);
    }

    private float calcMixFactor(int x) {
        return (x * (numCols + 1) / (float) numCols) / numCols;
    }

    private float calcSat(float mixFactor) {
        float s = lerp(mixFactor, satA, satB) + extraSat;

        if (s > 1.0f) {
            s = 1.0f;
        } else if (s < 0.0f) {
            s = 0.0f;
        }
        return s;
    }

    private float calcHue(float mixFactor) {
        float hueShift = ((HueSatPaletteConfig) config).getHueShift();
        float h = hueShift + Colors.lerpHue(mixFactor, hueA, hueB);
        if (h > 1.0f) {
            h = h - 1.0f;
        }
        return h;
    }

    @Override
    public void setSize(int numRows, int numCols) {
        super.setSize(numRows, numCols);
        lastCols = numCols;
        lastRows = numRows;
    }

    @Override
    public String getDialogTitle() {
        return startWithFg ?
            "HSB Mix with Background" :
            "HSB Mix with Foreground";
    }
}
