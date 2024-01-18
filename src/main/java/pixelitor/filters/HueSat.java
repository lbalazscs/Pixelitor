/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.RangeParam;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.gui.GUIText.BRIGHTNESS;
import static pixelitor.gui.GUIText.HUE;
import static pixelitor.gui.GUIText.SATURATION;

/**
 * Hue-Saturation filter
 */
public class HueSat extends ParametrizedFilter {
    public static final String NAME = HUE + "/" + SATURATION;

    @Serial
    private static final long serialVersionUID = -5215830710090103691L;

    private static final int MIN_HUE = -180;
    private static final int MAX_HUE = 180;
    private static final int DEFAULT_HUE = 0;

    private static final int MIN_SAT = -100;
    private static final int MAX_SAT = 100;
    private static final int DEFAULT_SAT = 0;

    private static final int MIN_BRI = -100;
    private static final int MAX_BRI = 100;
    private static final int DEFAULT_BRI = 0;

    private final RangeParam hue = new RangeParam(HUE, MIN_HUE, DEFAULT_HUE, MAX_HUE);
    private final RangeParam saturation = new RangeParam(SATURATION, MIN_SAT, DEFAULT_SAT, MAX_SAT);
    private final RangeParam brightness = new RangeParam(BRIGHTNESS, MIN_BRI, DEFAULT_BRI, MAX_BRI);

    public HueSat() {
        super(true);

        hue.setPresetKey("Hue");
        saturation.setPresetKey("Saturation");
        brightness.setPresetKey("Brightness");

        setParams(
            hue,
            saturation,
            brightness
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int hueP = hue.getValue();
        int satP = saturation.getValue();
        int briP = brightness.getValue();

        if (hueP == 0 && satP == 0 && briP == 0) {
            return src;
        }

        float satShift = (float) saturation.getPercentage();
        float briShift = (float) brightness.getPercentage();
        float hueShift = hue.getValueAsFloat() / 360.0f;

        dest = new Impl(hueShift, satShift, briShift).filter(src, dest);

        return dest;
    }

    private static class Impl extends PointFilter {
        private final float hueShift;
        private final float satShift;
        private final float briShift;

        protected Impl(float hueShift, float satShift, float briShift) {
            super(NAME);
            this.hueShift = hueShift;
            this.satShift = satShift;
            this.briShift = briShift;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int a = rgb & 0xFF_00_00_00;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            // for the multithreaded performance it's better to
            // create this array here instead of reusing it as a class field
            float[] tmpHSBArray = {0.0f, 0.0f, 0.0f};

            tmpHSBArray = Color.RGBtoHSB(r, g, b, tmpHSBArray);

            float shiftedHue = tmpHSBArray[0] + hueShift;
            float shiftedSat = tmpHSBArray[1] + satShift;
            float shiftedBri = tmpHSBArray[2] + briShift;

            if (shiftedSat < 0.0f) {
                shiftedSat = 0.0f;
            }
            if (shiftedSat > 1.0f) {
                shiftedSat = 1.0f;
            }

            if (shiftedBri < 0.0f) {
                shiftedBri = 0.0f;
            }
            if (shiftedBri > 1.0f) {
                shiftedBri = 1.0f;
            }

            if (shiftedHue < 0 && shiftedHue > -0.00000003) {
                // workaround for a bug in Color.HSBtoRGB, see issue #87
                shiftedHue = 0;
            }

            int newRGB = Color.HSBtoRGB(shiftedHue, shiftedSat, shiftedBri);  // alpha is 255 here
            newRGB &= 0x00_FF_FF_FF;  // set alpha to 0
            return a | newRGB; // add the real alpha
        }
    }
}