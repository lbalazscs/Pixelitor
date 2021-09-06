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

package pixelitor.filters.levels;

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.lookup.LuminanceLookup;

import java.awt.Color;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.utils.Texts.i18n;

/**
 * Determines which channels are currently edited
 * (used by Levels and Curves filters)
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
        public double getValue(int r, int g, int b) {
            return LuminanceLookup.from(r, g, b);
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
        public double getValue(int r, int g, int b) {
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
        public double getValue(int r, int g, int b) {
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
        public double getValue(int r, int g, int b) {
            return b;
        }
    };

    private static final Color LIGHT_PINK = new Color(255, 128, 128);
    private static final Color LIGHT_GREEN = new Color(128, 255, 128);
    private static final Color LIGHT_BLUE = new Color(128, 128, 255);

    private static final Color DARK_YELLOW_GREEN = new Color(128, 128, 0);
    private static final Color DARK_CYAN = new Color(0, 128, 128);
    private static final Color DARK_PURPLE = new Color(128, 0, 128);

    private final String name;
    private final String presetKey;
    private final Color color;
    private final Color inactiveColor;

    Channel(String name, String presetKey, Color color) {
        this.name = name;
        this.presetKey = presetKey;
        this.color = color;
        inactiveColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }

    public abstract Color getDarkColor();

    public abstract Color getLightColor();

    public Color getDrawColor(boolean active) {
        return active ? color : inactiveColor;
    }

    public abstract double getValue(int r, int g, int b);

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

    public static EnumParam<Channel> asParam() {
        return new EnumParam<>("Channel", Channel.class);
    }
}
