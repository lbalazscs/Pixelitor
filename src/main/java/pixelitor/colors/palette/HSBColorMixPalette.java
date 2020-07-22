/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

    private float hue, otherHue;
    private final float sat, bri, otherSat, otherBri;
    private final float averageSat;
    private float extraSat;

    private static final float MAX_BRI_DEVIATION = 0.5f;

    public HSBColorMixPalette(boolean startWithFg) {
        super(lastRows, lastCols);
        this.startWithFg = startWithFg;

        Color color, otherColor;
        if (startWithFg) {
            color = getFGColor();
            otherColor = getBGColor();
        } else {
            color = getBGColor();
            otherColor = getFGColor();
        }

        float[] hsb = Colors.toHSB(color);
        float[] hsb2 = Colors.toHSB(otherColor);

        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        otherHue = hsb2[0];
        otherSat = hsb2[1];
        otherBri = hsb2[2];

        // if the saturation is 0, then the hue does not mean anything,
        // but can lead to unexpected hue variations in the mix
        if (sat == 0) {
            hue = otherHue;
        } else if (otherSat == 0) {
            otherHue = hue;
        }

        // set the average saturation as the slider default
        averageSat = (sat + otherSat) / 2;
        config = new HueSatPaletteConfig(0.0f, averageSat);
    }

    @Override
    public void onConfigChange() {
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
                    float b = lerp(mixFactor, bri, otherBri);
                    c = new Color(Color.HSBtoRGB(h, s, b));
                } else {
                    float mixFactor = calcMixFactor(x);
                    float h = calcHue(mixFactor);
                    float s = calcSat(mixFactor);
                    float b = lerp(mixFactor, bri, otherBri);

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
        float s = lerp(mixFactor, sat, otherSat) + extraSat;

        if (s > 1.0f) {
            s = 1.0f;
        } else if (s < 0.0f) {
            s = 0.0f;
        }
        return s;
    }

    private float calcHue(float mixFactor) {
        float hueShift = ((HueSatPaletteConfig) config).getHueShift();
        float h = hueShift + Colors.lerpHue(mixFactor, hue, otherHue);
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
