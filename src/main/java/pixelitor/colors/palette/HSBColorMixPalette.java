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

package pixelitor.colors.palette;

import com.jhlabs.image.ImageMath;
import pixelitor.colors.ColorUtils;
import pixelitor.colors.FgBgColors;

import java.awt.Color;

public class HSBColorMixPalette extends Palette {
    private static int lastRows = 7;
    private static int lastCols = 10;

    private final boolean fg;
    private float hue, otherHue;
    private final float sat, bri, otherSat, otherBri;
    private final float averageSat;
    private float extraSat;

    private static final float MAX_BRI_DEVIATION = 0.5f;

    public HSBColorMixPalette(boolean fg) {
        super(lastRows, lastCols);
        this.fg = fg;

        Color color, otherColor;
        if (fg) {
            color = FgBgColors.getFG();
            otherColor = FgBgColors.getBG();
        } else {
            color = FgBgColors.getBG();
            otherColor = FgBgColors.getFG();
        }

        float[] hsb = ColorUtils.colorToHSB(color);
        float[] hsb2 = ColorUtils.colorToHSB(otherColor);

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
        HueSatPaletteConfig hs = (HueSatPaletteConfig) config;
        extraSat = hs.getSaturation() - averageSat;
    }

    @Override
    public void addButtons(VariationsPanel panel) {
        for (int y = 0; y < numRows; y++) {
            float briStep = calcBriStep();
            for (int x = 0; x < numCols; x++) {
                Color c;
                if (numRows == 1) {
                    float mixFactor = calcMixFactor(x);
                    float h = calcHue(mixFactor);
                    float s = calcSat(mixFactor);
                    float b = ImageMath.lerp(mixFactor, bri, otherBri);
                    c = new Color(Color.HSBtoRGB(h, s, b));
                } else {
                    float mixFactor = calcMixFactor(x);
                    float h = calcHue(mixFactor);
                    float s = calcSat(mixFactor);
                    float b = ImageMath.lerp(mixFactor, bri, otherBri);

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
        return (x * (numCols + 1) / (float) numCols) / (float) numCols;
    }

    private float calcSat(float mixFactor) {
        float s = ImageMath.lerp(mixFactor, sat, otherSat) + extraSat;

        if (s > 1.0f) {
            s = 1.0f;
        } else if (s < 0.0f) {
            s = 0.0f;
        }
        return s;
    }

    private float calcHue(float mixFactor) {
        HueSatPaletteConfig hs = (HueSatPaletteConfig) config;
        float hueShift = hs.getHueShift();

        float h = hueShift + ColorUtils.lerpHue(mixFactor, hue, otherHue);
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
        return fg ? "HSB Mix with Background" : "HSB Mix with Foreground";
    }
}
