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

package pixelitor.colors;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorSwatch;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.palette.ColorSwatchClickHandler;
import pixelitor.colors.palette.PalettePanel;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.TaskAction;
import pixelitor.utils.Lazy;
import pixelitor.utils.Utils;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

/**
 * Color-related static utility methods.
 */
public class Colors {
    public static final Color TRANSPARENT_BLACK = new Color(0, true);

    // "color wheel" colors
    public static final Color CW_RED = new Color(233, 28, 35);
    public static final Color CW_GREEN = new Color(58, 180, 67);
    public static final Color CW_BLUE = new Color(43, 115, 191);
    public static final Color CW_ORANGE = new Color(248, 175, 62);
    public static final Color CW_TEAL = new Color(40, 167, 158);
    public static final Color CW_VIOLET = new Color(145, 37, 144);
    public static final Color CW_YELLOW = new Color(251, 240, 2);


    private Colors() {
    }

    /**
     * Linearly interpolates between two colors in the RGB color space.
     */
    public static Color interpolateRGB(Color startColor, Color endColor, double progress) {
        int interpolatedRGB = ImageMath.mixColors((float) progress,
            startColor.getRGB(), endColor.getRGB());
        return new Color(interpolatedRGB, true);
    }

    /**
     * Calculates the average of two colors in the RGB space.
     * Full opacity is assumed.
     */
    public static Color averageRGB(Color c1, Color c2) {
        int r = (c1.getRed() + c2.getRed()) / 2;
        int g = (c1.getGreen() + c2.getGreen()) / 2;
        int b = (c1.getBlue() + c2.getBlue()) / 2;

        return new Color(r, g, b);
    }

    /**
     * Linearly interpolates between two hue values, taking their circular nature into account.
     */
    public static float lerpHue(float mixFactor, float hue1, float hue2) {
        float diff = hue1 - hue2;

        // take the shortest path around the color wheel
        if (diff > 0.5f) {
            // hue1 is big, hue2 is small, so wrap hue2 up
            hue2 += 1.0f;
        } else if (diff < -0.5f) {
            // hue2 is big, hue1 is small, so wrap hue1 up
            hue1 += 1.0f;
        }

        float mix = ImageMath.lerp(mixFactor, hue1, hue2);

        // wrap around if the result is >= 1.0
        if (mix >= 1.0f) {
            mix -= 1.0f;
        }
        return mix;
    }

    public static String argbToString(int rgb) {
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        return format("(%d, %d, %d, %d)", a, r, g, b);
    }

    public static int toPackedARGB(int a, int r, int g, int b) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static Color toGray(Color c) {
        int gray = (2 * c.getRed() + 3 * c.getGreen() + c.getBlue()) / 6;

        return new Color(0xFF_00_00_00 | gray << 16 | gray << 8 | gray);
    }

    public static float[] toHSB(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    public static int hsbToARGB(float[] hsb, int alpha) {
        int col = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return setAlpha(col, alpha);
    }

    public static String toHTMLHex(Color c, boolean includeAlpha) {
        if (includeAlpha) {
            // RRGGBBAA format
            return format(Locale.ENGLISH, "%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        } else {
            // RRGGBB format
            return format(Locale.ENGLISH, "%06X", 0x00_FF_FF_FF & c.getRGB());
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
        GlobalEvents.modalDialogOpened();
        Color color = ColorPicker.showDialog(owner, title, selectedColor,
            allowTransparency, colorChangeListener);
        GlobalEvents.modalDialogClosed();

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
        String text;
        try {
            var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            text = (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return null;
        }

        text = text.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }

        // try HTML hex format (RRGGBB or RRGGBBAA)
        if (text.length() == 6 || text.length() == 8) {
            return fromHTMLHex(text);
        }

        // try rgb(163, 69, 151) format
        if (text.toLowerCase(Locale.ENGLISH).startsWith("rgb(") && text.endsWith(")")) {
            try {
                String[] components = text.substring(4, text.length() - 1).split("\\s*,\\s*");
                if (components.length == 3) {
                    return new Color(
                        parseInt(components[0].trim()),
                        parseInt(components[1].trim()),
                        parseInt(components[2].trim()));
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Attaches a right-click popup menu to a color swatch.
     *
     * @param parent      the parent component, used to determine the owner window for dialogs
     * @param swatch      the color swatch to attach the menu to
     * @param colorSource a supplier for the current color (used for 'Copy Color')
     * @param colorSetter a consumer for the newly selected color
     */
    public static void setupFilterColorsPopupMenu(JComponent parent, ColorSwatch swatch,
                                                  Supplier<Color> colorSource,
                                                  Consumer<Color> colorSetter) {
        Lazy<JPopupMenu> popupHolder = Lazy.of(() ->
            createFilterColorPopup(parent, colorSource, colorSetter));

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
                    popupHolder.get().show(swatch, e.getX(), e.getY());
                }
            }
        });
    }

    private static JPopupMenu createFilterColorPopup(JComponent parent,
                                                     Supplier<Color> colorSource,
                                                     Consumer<Color> colorSetter) {
        JPopupMenu popup = new JPopupMenu();

        ColorSwatchClickHandler clickHandler = (newColor, e) -> colorSetter.accept(newColor);
        Window window = SwingUtilities.windowForComponent(parent);

        popup.add(new TaskAction("Color Variations...", () ->
            PalettePanel.showFilterVariationsDialog(window, colorSource.get(), clickHandler)));
        popup.add(new TaskAction("Color History...", () ->
            ColorHistory.INSTANCE.showDialog(window, clickHandler, true)));

        popup.addSeparator();
        popup.add(createCopyColorAction(colorSource));
        popup.add(createPasteColorAction(window, colorSetter));

        popup.addSeparator();
        popup.add(new TaskAction("Set to Foreground Color", () ->
            colorSetter.accept(FgBgColors.getFGColor())));
        popup.add(new TaskAction("Set to Background Color", () ->
            colorSetter.accept(FgBgColors.getBGColor())));

        return popup;
    }

    public static Action createCopyColorAction(Supplier<Color> colorSource) {
        return new TaskAction("Copy Color", () ->
            copyColorToClipboard(colorSource.get()));
    }

    public static Action createPasteColorAction(Window window, Consumer<Color> colorSetter) {
        return new TaskAction("Paste Color", () -> {
            Color color = getColorFromClipboard();
            if (color == null) {
                Dialogs.showNotAColorOnClipboardDialog(window);
            } else {
                colorSetter.accept(color);
            }
        });
    }

    public static void fillWith(Color color, BufferedImage img) {
        Graphics2D g = img.createGraphics();
        fillWith(color, g, img.getWidth(), img.getHeight());
        g.dispose();
    }

    public static void fillWith(Color color, Graphics2D g2, int width, int height) {
        g2.setColor(color);
        g2.fillRect(0, 0, width, height);
    }

    public static void fillWithTransparent(Graphics2D g, int size) {
        Composite origComposite = g.getComposite();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(origComposite);
    }

    /**
     * Sets the alpha channel of the given ARGB packed int to the given 0-255 value.
     */
    public static int setAlpha(int rgb, int newAlpha) {
        // discard the original alpha and set it to the new value
        return (newAlpha << 24) | (rgb & 0x00_FF_FF_FF);
    }

    /**
     * Sets the alpha channel of a packed ARGB int, ensuring the
     * new alpha does not exceed the original alpha value.
     */
    public static int capAlpha(int rgb, int newAlpha) {
        int origAlpha = (rgb >>> 24) & 0xFF;
        return setAlpha(rgb, Math.min(origAlpha, newAlpha));
    }

    /**
     * Format a color's value in the format expected by G'MIC.
     */
    public static String formatGMIC(Color color) {
        return "%d,%d,%d,%d".formatted(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static String formatForDebugging(Color color) {
        return "(r=%d, g=%d, b=%d, a=%d)".formatted(
            color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static String formatForDebugging(Color[] colors) {
        return Arrays.stream(colors)
            .map(Colors::formatForDebugging)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
