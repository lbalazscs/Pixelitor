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

package pixelitor.colors;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorSwatch;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.PalettePanel;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.utils.Utils;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

/**
 * Color-related static utility methods.
 */
public class Colors {
    public static final Color TRANSPARENT_COLOR = new Color(0, true);

    private Colors() {
    }

    // linear interpolation in the RGB space
    public static Color rgbInterpolate(Color startColor, Color endColor, float progress) {
        int interpolatedRGB = ImageMath.mixColors(progress,
            startColor.getRGB(), endColor.getRGB());
        return new Color(interpolatedRGB, true);
    }

    /**
     * Calculates the average of two colors in the RGB space.
     * Full opacity is assumed.
     */
    public static Color rgbAverage(Color c1, Color c2) {
        assert c1 != null && c2 != null;

        int rgb1 = c1.getRGB();
        int rgb2 = c2.getRGB();

        int r1 = (rgb1 >>> 16) & 0xFF;
        int g1 = (rgb1 >>> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >>> 16) & 0xFF;
        int g2 = (rgb2 >>> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        return new Color(r, g, b);
    }

    /**
     * Calculates the average of two colors in the HSB space.
     * Full opacity is assumed.
     */
    public static Color hsbAverage(Color c1, Color c2) {
        assert c1 != null && c2 != null;

        int rgb1 = c1.getRGB();
        int rgb2 = c2.getRGB();

        int r1 = (rgb1 >>> 16) & 0xFF;
        int g1 = (rgb1 >>> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >>> 16) & 0xFF;
        int g2 = (rgb2 >>> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        float[] hsb1 = Color.RGBtoHSB(r1, g1, b1, null);
        float[] hsb2 = Color.RGBtoHSB(r2, g2, b2, null);

        float hue = hueAverage(hsb1[0], hsb2[0]);
        float sat = (hsb1[1] + hsb2[1]) / 2.0f;
        float bri = (hsb1[2] + hsb2[2]) / 2.0f;

        return Color.getHSBColor(hue, sat, bri);
    }

    private static float hueAverage(float hue1, float hue2) {
        float diff = hue1 - hue2;
        if (diff < 0.5f && diff > -0.5f) {
            return (hue1 + hue2) / 2.0f;
        } else if (diff >= 0.5f) { // hue1 is bigger
            return hue1 + (1.0f - hue1 + hue2) / 2.0f;
        } else if (diff <= 0.5f) { // hue2 is bigger
            return hue2 + (1.0f - hue2 + hue1) / 2.0f;
        } else {
            throw new IllegalStateException(
                format("hue1 = %.2f, hue2 = %.2f", hue1, hue2));
        }
    }

    /**
     * A linear interpolation for hue values,
     * taking their circular nature into account
     */
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
            throw new IllegalStateException(
                format("hue1 = %.2f, hue2 = %.2f, mixFactor = %.2f", hue1, hue2, mixFactor));
        }
    }

    /**
     * Just like the above lerpHue method, but taking the average
     * that is on the opposite side of the circle. Not very useful.
     */
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
            throw new IllegalStateException(
                format("hue1 = %.2f, hue2 = %.2f, mixFactor = %.2f", hue1, hue2, mixFactor));
        }
    }

    public static String packedIntToString(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        return format("(%d, %d, %d, %d)", a, r, g, b);
    }

    public static float calcSaturation(int r, int g, int b) {
        int cMax = Math.max(r, g);
        if (b > cMax) {
            cMax = b;
        }
        int cMin = Math.min(r, g);
        if (b < cMin) {
            cMin = b;
        }

        float sat;
        if (cMax != 0) {
            sat = (cMax - cMin) / (float) cMax;
        } else {
            sat = 0;
        }
        return sat;
    }

    public static int toPackedARGB(int a, int r, int g, int b) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static Color toGray(Color c) {
        int rgb = c.getRGB();
//        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;

        int gray = (r + r + g + g + g + b) / 6;

        return new Color(0xFF_00_00_00 | gray << 16 | gray << 8 | gray);
    }

    public static float[] toHSB(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    public static int HSBAtoARGB(float[] hsb_col, int alpha) {
        int col = Color.HSBtoRGB(hsb_col[0], hsb_col[1], hsb_col[2]);
        col &= 0x00FFFFFF; // Remove the FF alpha which was added by HSBtoRGB.
        col |= (alpha << 24); // Add the alpha specified by user.
        return col;
    }

    public static String toHTMLHex(Color c, boolean includeAlpha) {
        if (includeAlpha) {
            String argb = format("%08X", c.getRGB());
            String rgba = argb.substring(2) + argb.substring(0, 2);
            return rgba;
        } else {
            return format("%06X", 0xFFFFFF & c.getRGB());
        }
    }

    public static Color fromHTMLHex(String text) {
        int length = text.length();
        if (length == 6) {
            return new Color(
                parseInt(text.substring(0, 2), 16),
                parseInt(text.substring(2, 4), 16),
                parseInt(text.substring(4, 6), 16));
        } else if (length == 8) {
            return new Color(
                parseInt(text.substring(0, 2), 16),
                parseInt(text.substring(2, 4), 16),
                parseInt(text.substring(4, 6), 16),
                parseInt(text.substring(6, 8), 16));
        } else {
            throw new IllegalArgumentException("text = " + text);
        }
    }

    public static void selectColorWithDialog(JComponent component, String title,
                                             Color selectedColor, boolean allowTransparency,
                                             Consumer<Color> colorChangeListener) {
        Window owner = SwingUtilities.getWindowAncestor(component);
        selectColorWithDialog(owner, title, selectedColor, allowTransparency, colorChangeListener);
    }

    // returns true if the dialog was accepted
    public static boolean selectColorWithDialog(Window owner, String title,
                                                Color selectedColor, boolean allowTransparency,
                                                Consumer<Color> colorChangeListener) {
        if (RandomGUITest.isRunning()) {
            return false;
        }

        Color prevColor = selectedColor;
        GlobalEvents.dialogOpened(title);
        Color color = ColorPicker.showDialog(owner, title, selectedColor,
            allowTransparency, colorChangeListener);
        GlobalEvents.dialogClosed(title);

        if (color == null) {  // Cancel was pressed, reset the old color
            colorChangeListener.accept(prevColor);
            return false;
        } else {
            return true;
        }
    }

    public static void copyColorToClipboard(Color c) {
        Utils.copyStringToClipboard(toHTMLHex(c, false));
    }

    public static Color getColorFromClipboard() {
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
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
            return fromHTMLHex(text);
        }

        // try rgb(163, 69, 151) format
        if (text.startsWith("rgb(") && text.endsWith(")")) {
            text = text.substring(4, text.length() - 1);
            String[] strings = text.split("\\s*,\\s*");
            if (strings.length == 3) {
                return new Color(
                    parseInt(strings[0]),
                    parseInt(strings[1]),
                    parseInt(strings[2]));
            }
        }

        return null;
    }

    public static void setupFilterColorsPopupMenu(JComponent parent, ColorSwatch swatch,
                                                  Supplier<Color> colorSupplier,
                                                  Consumer<Color> colorSetter) {
        var popup = new JPopupMenu();

        ColorSwatchClickHandler clickHandler = (newColor, e) -> colorSetter.accept(newColor);

        popup.add(new PAction("Color Variations...") {
            @Override
            public void onClick() {
                Window window = SwingUtilities.windowForComponent(parent);
                PalettePanel.showFilterVariationsDialog(window, colorSupplier.get(), clickHandler);
            }
        });

        popup.add(new PAction("Filter Color History...") {
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

        // swatch.setComponentPopupMenu doesn't consider the disabled state
        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                if (swatch.isEnabled()) {
                    popup.show(swatch, e.getX(), e.getY());
                }
            }
        });
    }

    public static void setupCopyColorPopupMenu(JPopupMenu popup, Supplier<Color> colorSupplier) {
        popup.add(new PAction("Copy Color") {
            @Override
            public void onClick() {
                copyColorToClipboard(colorSupplier.get());
            }
        });
    }

    public static void setupPasteColorPopupMenu(JPopupMenu popup, Window window, Consumer<Color> colorSetter) {
        popup.add(new PAction("Paste Color") {
            @Override
            public void onClick() {
                Color color = getColorFromClipboard();
                if (color == null) {
                    Dialogs.showNotAColorOnClipboardDialog(window);
                } else {
                    colorSetter.accept(color);
                }
            }
        });
    }

    public static void fillWith(Color color, Graphics2D g2, int width, int height) {
        g2.setColor(color);
        g2.fillRect(0, 0, width, height);
    }
}
