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

package pixelitor.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.utils.ColorUtils;

import java.awt.Color;

public enum ColorVariationsType {
    BRIGHTNESS("Brightness") {
        @Override
        public Color[] generate(float hue, float sat, float bri, int num, Color otherColor) {
            Color[] retVal = new Color[num];
            for (int i = 0; i < num; i++) {
                float generatedBri = (i * (num + 1) / (float) num) / (float) num;
                retVal[i] = Color.getHSBColor(hue, sat, generatedBri);
            }
            return retVal;
        }
    }, SATURATION("Saturation") {
        @Override
        public Color[] generate(float hue, float sat, float bri, int num, Color otherColor) {
            Color[] retVal = new Color[num];
            for (int i = 0; i < num; i++) {
                float generatedSat = (i * (num + 1) / (float) num) / (float) num;
                retVal[i] = Color.getHSBColor(hue, generatedSat, bri);
            }
            return retVal;
        }
    }, BG_MIX("Mix with Background") {
        @Override
        public Color[] generate(float hue, float sat, float bri, int num, Color otherColor) {
            return mixWith(otherColor, hue, sat, bri, num);
        }
    }, FG_MIX("Mix with Foreground") {
        @Override
        public Color[] generate(float hue, float sat, float bri, int num, Color otherColor) {
            return mixWith(otherColor, hue, sat, bri, num);
        }
    };

    private final String name;

    ColorVariationsType(String name) {
        this.name = name;
    }

    public abstract Color[] generate(float hue, float sat, float bri, int num, Color otherColor);

    protected static Color[] mixWith(Color other, float hue, float sat, float bri, int num) {
        float[] hsb = Color.RGBtoHSB(other.getRed(), other.getGreen(), other.getBlue(), null);
        float otherHue = hsb[0];
        float otherSat = hsb[1];
        float otherBri = hsb[2];

        Color[] retVal = new Color[num];
        for (int i = 0; i < num; i++) {
            float mixFactor = (i * (num + 1) / (float) num) / (float) num;
            float h = ColorUtils.lerpHue(mixFactor, hue, otherHue);
            float s = ImageMath.lerp(mixFactor, sat, otherSat);
            float b = ImageMath.lerp(mixFactor, bri, otherBri);
            retVal[i] = new Color(Color.HSBtoRGB(h, s, b));
        }
        return retVal;
    }

    public String getName() {
        return name;
    }
}
