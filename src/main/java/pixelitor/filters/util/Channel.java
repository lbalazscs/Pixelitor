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

package pixelitor.filters.util;

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.lookup.LuminanceLookup;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.utils.Texts.i18n;

/**
 * Represents the currently edited color channel(s).
 */
public enum Channel {
    RGB("RGB", "rgb", BLACK) {
        @Override
        public Color getDarkColor() {
            return BLACK;
        }

        @Override
        public Color getLightColor() {
            return WHITE;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            return LuminanceLookup.from(r, g, b);
        }

        @Override
        public Color getDrawColor(boolean active, boolean darkTheme) {
            if (darkTheme) {
                return active ? WHITE : FADED_WHITE;
            } else {
                return super.getDrawColor(active, darkTheme);
            }
        }
    }, RED(i18n("red"), "red", Color.RED) {
        @Override
        public Color getDarkColor() {
            return DARK_CYAN;
        }

        @Override
        public Color getLightColor() {
            return LIGHT_PINK;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            return r;
        }
    }, GREEN(i18n("green"), "green", Color.GREEN) {
        @Override
        public Color getDarkColor() {
            return DARK_PURPLE;
        }

        @Override
        public Color getLightColor() {
            return LIGHT_GREEN;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            return g;
        }
    }, BLUE(i18n("blue"), "blue", Color.BLUE) {
        @Override
        public Color getDarkColor() {
            return DARK_YELLOW_GREEN;
        }

        @Override
        public Color getLightColor() {
            return LIGHT_BLUE;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            return b;
        }
    }, OK_L(GUIText.LIGHTNESS, "L", BLACK) {
        @Override
        public Color getDarkColor() {
            return BLACK;
        }

        @Override
        public Color getLightColor() {
            return WHITE;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            throw new UnsupportedOperationException();
        }
    }, OK_A(GUIText.RED_GREEN_A, "a", BLACK) {
        @Override
        public Color getDarkColor() {
            return OK_GREEN;
        }

        @Override
        public Color getLightColor() {
            return OK_RED;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            throw new UnsupportedOperationException();
        }
    }, OK_B(GUIText.BLUE_YELLOW_B, "b", BLACK) {
        @Override
        public Color getDarkColor() {
            return OK_BLUE;
        }

        @Override
        public Color getLightColor() {
            return OK_YELLOW;
        }

        @Override
        public double getIntensity(int r, int g, int b) {
            throw new UnsupportedOperationException();
        }
    };

    private static final int INACTIVE_ALPHA = 100;
    private static final Color FADED_WHITE = new Color(0x64_FF_FF_FF, true);

    // light colors for gradients
    private static final Color LIGHT_PINK = new Color(255, 128, 128);
    private static final Color LIGHT_GREEN = new Color(128, 255, 128);
    private static final Color LIGHT_BLUE = new Color(128, 128, 255);

    // dark colors for gradients
    private static final Color DARK_CYAN = new Color(0, 128, 128);
    private static final Color DARK_PURPLE = new Color(128, 0, 128);
    private static final Color DARK_YELLOW_GREEN = new Color(128, 128, 0);

    // Oklab gradient colors
    private static final Color OK_GREEN = new Color(0, 157, 113);
    private static final Color OK_RED = new Color(213, 63, 63);
    private static final Color OK_BLUE = new Color(66, 0, 254);
    private static final Color OK_YELLOW = new Color(254, 157, 0);

    private final String name;
    private final String presetKey;
    private final Color color;
    private final Color inactiveColor;

    Channel(String name, String presetKey, Color color) {
        this.name = name;
        this.presetKey = presetKey;
        this.color = color;
        this.inactiveColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), INACTIVE_ALPHA);
    }

    /**
     * Returns the dark gradient color used in the UI.
     */
    public abstract Color getDarkColor();

    /**
     * Returns the light gradient color used in the UI.
     */
    public abstract Color getLightColor();

    public Color getDrawColor(boolean active, boolean darkTheme) {
        return active ? color : inactiveColor;
    }

    /**
     * Calculates the intensity of this channel from the given RGB values.
     */
    public abstract double getIntensity(int r, int g, int b);

    public String getName() {
        return name;
    }

    public String getPresetKey() {
        return presetKey;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<Channel> getRGBChoices() {
        return List.of(RGB, RED, GREEN, BLUE);
    }

    public static List<Channel> getLABChoices() {
        return List.of(OK_L, OK_A, OK_B);
    }

    public static Channel[] getRGBValues() {
        return new Channel[]{RGB, RED, GREEN, BLUE};
    }

    public static List<Channel> getChoices(ColorSpace space) {
        return switch (space) {
            case SRGB -> getRGBChoices();
            case OKLAB -> getLABChoices();
        };
    }

    public static EnumParam<Channel> asParam(ColorSpace startingSpace) {
        return new EnumParam<>("Channel", getChoices(startingSpace));
    }
}
