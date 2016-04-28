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

package pixelitor.colors;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorSwatch;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.VariationsPanel;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.menus.MenuAction;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private static float calculateHueAverage(float hue1, float hue2) {
        float diff = hue1 - hue2;
        if (diff < 0.5f && diff > -0.5f) {
            return (hue1 + hue2) / 2.0f;
        } else if (diff >= 0.5f) { // hue1 is bigger
            float retVal = hue1 + (1.0f - hue1 + hue2) / 2.0f;
            return retVal;
        } else if (diff <= 0.5f) { // hue2 is bigger
            float retVal = hue2 + (1.0f - hue2 + hue1) / 2.0f;
            return retVal;
        } else {
            throw new IllegalStateException("should not get here");
        }
    }

    public static float lerpHue(float mixFactor, float hue1, float hue2) {
        float diff = hue1 - hue2;
        if (diff < 0.5f && diff > -0.5f) {
            return ImageMath.lerp(mixFactor, hue1, hue2);
        } else if (diff >= 0.5f) { // hue1 is big, hue2 is small
            hue2 += 1.0f;
            float mix = ImageMath.lerp(mixFactor, hue1, hue2);
            if (mix > 1.0f) {
                mix -= 1.0f;
            }
            return mix;
        } else if (diff <= 0.5f) { // hue2 is big, hue1 is small
            hue1 += 1.0f;
            float mix = ImageMath.lerp(mixFactor, hue1, hue2);
            if (mix > 1.0f) {
                mix -= 1.0f;
            }
            return mix;
        } else {
            throw new IllegalStateException("should not get here");
        }
    }

    public static float lerpHueLong(float mixFactor, float hue1, float hue2) {
        float diff = hue1 - hue2;
        if (diff > 0.5f || diff < -0.5f) {
            return ImageMath.lerp(mixFactor, hue1, hue2);
        } else if (hue2 > hue1) { // hue2 is slightly bigger
            hue1 += 1.0f;
            float mix = ImageMath.lerp(mixFactor, hue1, hue2);
            if (mix > 1.0f) {
                mix -= 1.0f;
            }
            return mix;
        } else if (hue1 >= hue2) { // hue1 is slightly bigger
            hue2 += 1.0f;
            float mix = ImageMath.lerp(mixFactor, hue1, hue2);
            if (mix > 1.0f) {
                mix -= 1.0f;
            }
            return mix;
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
        int cMax = (r > g) ? r : g;
        if (b > cMax) {
            cMax = b;
        }
        int cMin = (r < g) ? r : g;
        if (b < cMin) {
            cMin = b;
        }

        if (cMax != 0) {
            sat = ((float) (cMax - cMin)) / ((float) cMax);
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

    public static Color colorToGray(Color c) {
        int rgb = c.getRGB();
//        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        int gray = (r + r + g + g + g + b) / 6;

        return new Color(0xFF_00_00_00 | (gray << 16) | (gray << 8) | gray);
    }

    public static float[] colorToHSB(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    public static void copyColorToClipboard(Color c) {
        String htmlHexString = String.format("%06X", (0xFFFFFF & c.getRGB()));

        Utils.copyStringToClipboard(htmlHexString);
    }

    public static Color getColorFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String text;
        try {
            text = (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return null;
        }

        text = text.trim();

        text = text.startsWith("#") ? text.substring(1) : text;

        // try HTML hex format
        if (text.length() == 6) {
            return new Color(
                    Integer.parseInt(text.substring(0, 2), 16),
                    Integer.parseInt(text.substring(2, 4), 16),
                    Integer.parseInt(text.substring(4, 6), 16));
        }

        // try rgb(163, 69, 151) format
        if (text.startsWith("rgb(") && text.endsWith(")")) {
            text = text.substring(4, text.length() - 1);
            String[] strings = text.split("\\s*,\\s*");
            if (strings.length == 3) {
                return new Color(
                        Integer.parseInt(strings[0]),
                        Integer.parseInt(strings[1]),
                        Integer.parseInt(strings[2]));
            }
        }

        return null;
    }

    public static void setupFilterColorsPopupMenu(JComponent parent, ColorSwatch swatch,
                                                  Supplier<Color> colorSupplier,
                                                  Consumer<Color> colorSetter) {
        JPopupMenu popup = new JPopupMenu();

        ColorSwatchClickHandler clickHandler = (newColor, e) -> colorSetter.accept(newColor);

        popup.add(new MenuAction("Color Variations...") {
            @Override
            public void onClick() {
                Window window = SwingUtilities.windowForComponent(parent);
                VariationsPanel.showFilterVariationsDialog(window, colorSupplier.get(), clickHandler);
            }
        });

        popup.add(new MenuAction("Filter Color History...") {
            @Override
            public void onClick() {
                Window window = SwingUtilities.windowForComponent(parent);
                ColorHistory.FILTER.showDialog(window, clickHandler);
            }
        });

        popup.addSeparator();

        setupCopyColorPopupMenu(popup, colorSupplier);

        Window window = SwingUtilities.windowForComponent(parent);
        setupPasteColorPopupMenu(popup, window, colorSetter);

        swatch.setComponentPopupMenu(popup);
    }

    public static void setupCopyColorPopupMenu(JPopupMenu popup, Supplier<Color> colorSupplier) {
        popup.add(new MenuAction("Copy Color") {
            @Override
            public void onClick() {
                ColorUtils.copyColorToClipboard(colorSupplier.get());
            }
        });
    }

    public static void setupPasteColorPopupMenu(JPopupMenu popup, Window window, Consumer<Color> colorSetter) {
        popup.add(new MenuAction("Paste Color") {
            @Override
            public void onClick() {
                Color color = ColorUtils.getColorFromClipboard();
                if (color == null) {
                    Dialogs.showNotAColorOnClipboardDialog(window);
                } else {
                    colorSetter.accept(color);
                }
            }
        });
    }
}
