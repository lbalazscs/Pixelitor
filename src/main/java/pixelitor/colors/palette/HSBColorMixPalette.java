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

package pixelitor.colors.palette;

import pixelitor.colors.Colors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.jhlabs.image.ImageMath.lerp;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * A palette that mixes foreground and background colors using HSB interpolation.
 */
public class HSBColorMixPalette extends DynamicPalette {
    // static palette-specific fields are remembered between dialog sessions
    private static int lastRowCount = 7;
    private static int lastColumnCount = 10;

    private final boolean startWithFg;

    private float hueA, hueB;
    private final float satA, briA, satB, briB;
    private final float averageSat;
    private float satAdjustment = 0.0f;

    private static final float MAX_BRI_DEVIATION = 0.5f;

    public HSBColorMixPalette(boolean startWithFg) {
        super(lastRowCount, lastColumnCount);
        this.startWithFg = startWithFg;

        Color colorA = startWithFg ? getFGColor() : getBGColor();
        Color colorB = startWithFg ? getBGColor() : getFGColor();

        float[] hsbA = Colors.toHSB(colorA);
        float[] hsbB = Colors.toHSB(colorB);

        hueA = hsbA[0];
        satA = hsbA[1];
        briA = hsbA[2];
        hueB = hsbB[0];
        satB = hsbB[1];
        briB = hsbB[2];

        // if saturation is zero, hue is meaningless and can cause
        // unexpected hue variations in the mix
        if (satA == 0) {
            hueA = hueB;
        } else if (satB == 0) {
            hueB = hueA;
        }

        // set the average saturation as the slider default
        averageSat = (satA + satB) / 2.0f;
        config = new HueSatPaletteConfig(0.0f, averageSat);
    }

    @Override
    public void onConfigChanged() {
        float configSat = ((HueSatPaletteConfig) config).getSaturation();
        satAdjustment = configSat - averageSat;
    }

    @Override
    public List<Color> getColors() {
        List<Color> colors = new ArrayList<>(rowCount * columnCount);
        float briStep = (rowCount > 1) ? calcBriStep() : 0;

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                float mixFactor = calcMixFactor(col);
                float h = calcHue(mixFactor);
                float s = calcSat(mixFactor);
                float b = lerp(mixFactor, briA, briB);

                if (rowCount > 1) {
                    float startBri = b - MAX_BRI_DEVIATION;
                    b = startBri + row * briStep;
                    b = Math.max(0.0f, Math.min(1.0f, b));
                }

                int rgb = Color.HSBtoRGB(h, s, b);
                colors.add(new Color(rgb));
            }
        }
        return colors;
    }

    private float calcBriStep() {
        // the total brightness range (2 * MAX_BRI_DEVIATION) is
        // divided into (rowCount - 1) parts
        return 2 * MAX_BRI_DEVIATION / (rowCount - 1);
    }

    private float calcMixFactor(int col) {
        return (col * (columnCount + 1) / (float) columnCount) / columnCount;
    }

    private float calcSat(float mixFactor) {
        float s = lerp(mixFactor, satA, satB) + satAdjustment;
        return Math.max(0.0f, Math.min(1.0f, s));
    }

    private float calcHue(float mixFactor) {
        float hueOffset = ((HueSatPaletteConfig) config).getHueOffset();
        float h = hueOffset + Colors.lerpHue(mixFactor, hueA, hueB);
        return h % 1.0f;
    }

    @Override
    public void setGridSize(int rows, int columns) {
        super.setGridSize(rows, columns);
        lastColumnCount = columns;
        lastRowCount = rows;
    }

    @Override
    public String getDialogTitle() {
        return startWithFg ? "HSB Mix with Background" : "HSB Mix with Foreground";
    }
}
