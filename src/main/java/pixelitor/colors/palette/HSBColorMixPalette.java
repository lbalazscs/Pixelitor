/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import static pixelitor.colors.FgBgColors.getBgColor;
import static pixelitor.colors.FgBgColors.getFgColor;

/**
 * A palette that mixes foreground and background colors using HSB interpolation.
 * The rows vary brightness around the interpolated value,
 * from darker at the top to lighter at the bottom.
 * The sliders rotate the hue and shift the saturation
 * relative to the average of the two colors.
 */
public class HSBColorMixPalette extends DynamicPalette {
    private static final float MAX_BRI_DEVIATION = 0.5f;

    private final HueSatPaletteConfig hueSatConfig;
    private final float hueA, hueB;
    private final float satA, briA, satB, briB;
    private final float averageSat;

    public HSBColorMixPalette(boolean startWithFg) {
        Color colorA = startWithFg ? getFgColor() : getBgColor();
        Color colorB = startWithFg ? getBgColor() : getFgColor();

        float[] hsbA = Colors.toHsb(colorA);
        float[] hsbB = Colors.toHsb(colorB);

        float hA = hsbA[0];
        float sA = hsbA[1];
        float bA = hsbA[2];
        float hB = hsbB[0];
        float sB = hsbB[1];
        float bB = hsbB[2];

        // if saturation is zero, hue is meaningless and can cause
        // unexpected hue variations in the mix
        if (sA == 0) {
            hA = hB;
        } else if (sB == 0) {
            hB = hA;
        }

        // set the average saturation as the slider default
        float averageSat = (sA + sB) / 2.0f;
        var hueSatConfig = new HueSatPaletteConfig(0.0f, averageSat);

        String title = startWithFg
            ? "HSB Mix: Foreground to Background"
            : "HSB Mix: Background to Foreground";

        // default grid: 7 brightness rows × 10 color mix columns
        super(title, hueSatConfig, 7, 10);

        this.hueSatConfig = hueSatConfig;
        this.hueA = hA;
        this.satA = sA;
        this.briA = bA;
        this.hueB = hB;
        this.satB = sB;
        this.briB = bB;
        this.averageSat = averageSat;
    }

    @Override
    protected Color createColor(int row, int col) {
        float mixFactor = calcColumnMixFactor(col);
        return Color.getHSBColor(
            calcHue(mixFactor),
            calcSat(mixFactor),
            calcBri(row, mixFactor));
    }

    private float calcSat(float mixFactor) {
        // the difference between the slider's saturation and the
        // default average, added to the interpolated saturation
        float satOffset = hueSatConfig.getSaturation() - averageSat;
        float s = lerp(mixFactor, satA, satB) + satOffset;
        return Math.clamp(s, 0.0f, 1.0f);
    }

    private float calcHue(float mixFactor) {
        float hueOffset = hueSatConfig.getHueOffset();
        return hueOffset + Colors.lerpHue(mixFactor, hueA, hueB);
    }

    private float calcBri(int row, float mixFactor) {
        float bri = lerp(mixFactor, briA, briB) + MAX_BRI_DEVIATION * calcRowOffset(row);
        return Math.clamp(bri, 0.0f, 1.0f);
    }
}
