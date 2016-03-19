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

package pixelitor.utils;

import com.bric.swing.ColorPicker;
import com.jhlabs.image.ImageMath;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;

import java.awt.Color;
import java.util.Random;

/**
 * Color-related static utility methods.
 */
public class ColorUtils {
    public static final Color TRANSPARENT_COLOR = new Color(0, true);

    private ColorUtils() {
    }

    public static Color interpolateColor(Color startColor, Color endColor, float progress) {
        int initialRGB = startColor.getRGB();
        int finalRGB = endColor.getRGB();

        // linear interpolation in the RGB space
        // possibly interpolating in HSB space would be better
        int interpolatedRGB = ImageMath.mixColors(progress, initialRGB, finalRGB);
        return new Color(interpolatedRGB);
    }

    public static Color getRandomColor(boolean randomAlpha) {
        Random rnd = new Random();
        return getRandomColor(rnd, randomAlpha);
    }

    public static Color getRandomColor(Random rnd, boolean randomAlpha) {
        int r = rnd.nextInt(256);
        int g = rnd.nextInt(256);
        int b = rnd.nextInt(256);

        if (randomAlpha) {
            int a = rnd.nextInt(256);
            return new Color(r, g, b, a);
        }

        return new Color(r, g, b);
    }

    /**
     * Calculates the average of two colors in the HSB space. Full opacity is assumed.
     */
    public static Color getHSBAverageColor(Color c1, Color c2) {
        assert c1 != null && c2 != null;

        int rgb1 = c1.getRGB();
        int rgb2 = c2.getRGB();

        int r1 = (rgb1 >>> 16) & 0xFF;
        int g1 = (rgb1 >>> 8) & 0xFF;
        int b1 = (rgb1) & 0xFF;

        int r2 = (rgb2 >>> 16) & 0xFF;
        int g2 = (rgb2 >>> 8) & 0xFF;
        int b2 = (rgb2) & 0xFF;

        float[] hsb1 = Color.RGBtoHSB(r1, g1, b1, null);
        float[] hsb2 = Color.RGBtoHSB(r2, g2, b2, null);

        float hue1 = hsb1[0];
        float hue2 = hsb2[0];
        float hue = calculateHueAverage(hue1, hue2);

        float sat = (hsb1[1] + hsb2[1]) / 2.0f;
        float bri = (hsb1[2] + hsb2[2]) / 2.0f;
        return Color.getHSBColor(hue, sat, bri);
    }

    private static float calculateHueAverage(float f1, float f2) {
        float delta = f1 - f2;
        if (delta < 0.5f && delta > -0.5f) {
            return (f1 + f2) / 2.0f;
        } else if (delta >= 0.5f) { // f1 is bigger
            float retVal = f1 + (1.0f - f1 + f2) / 2.0f;
            return retVal;
        } else if (delta <= 0.5f) { // f2 is bigger
            float retVal = f2 + (1.0f - f2 + f1) / 2.0f;
            return retVal;
        } else {
            throw new IllegalStateException("should not get here");
        }
    }

    public static String intColorToString(int color) {
        Color c = new Color(color);
        return "[r=" + c.getRed() + ", g=" + c.getGreen() + ", b=" + c.getBlue() + ']';
    }

    /**
     * Calculates the average of two colors in the RGB space. Full opacity is assumed.
     */
    public static Color getRGBAverageColor(Color c1, Color c2) {
        assert c1 != null && c2 != null;

        int rgb1 = c1.getRGB();
        int rgb2 = c2.getRGB();

        int r1 = (rgb1 >>> 16) & 0xFF;
        int g1 = (rgb1 >>> 8) & 0xFF;
        int b1 = (rgb1) & 0xFF;

        int r2 = (rgb2 >>> 16) & 0xFF;
        int g2 = (rgb2 >>> 8) & 0xFF;
        int b2 = (rgb2) & 0xFF;

        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        return new Color(r, g, b);
    }

    public static float calcSaturation(int r, int g, int b) {
        float sat;
        int cmax = (r > g) ? r : g;
        if (b > cmax) {
            cmax = b;
        }
        int cmin = (r < g) ? r : g;
        if (b < cmin) {
            cmin = b;
        }

        if (cmax != 0) {
            sat = ((float) (cmax - cmin)) / ((float) cmax);
        } else {
            sat = 0;
        }
        return sat;
    }

    public static String colorIntToString(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        return String.format("(%d, %d, %d, %d)", a, r, g, b);
    }

    public static int toPackedInt(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // should not be called from dialogs because it sets dialogActive to false at the end
    public static Color showColorPickerDialog(PixelitorWindow pw, String title, Color selectedColor, boolean allowOpacity) {
        GlobalKeyboardWatch.setDialogActive(true);
        Color color = ColorPicker.showDialog(pw, title, selectedColor, allowOpacity);
        GlobalKeyboardWatch.setDialogActive(false);
        return color;
    }
}
