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

package pixelitor.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

public enum ResizeUnit {
    PIXELS("pixels") {
        @Override
        public int toPixels(double value, int originalSize) {
            return (int) Math.round(value);
        }

        @Override
        public double fromPixels(int pixelValue, int originalSize) {
            return pixelValue;
        }

        @Override
        public String format(double value) {
            return String.valueOf((int) Math.round(value));
        }

        @Override
        public double parse(String text) throws NumberFormatException {
            if (text.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(text);
        }
    },
    PERCENTAGE("percent") {
        private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#0.00");

        @Override
        public int toPixels(double value, int originalSize) {
            return (int) Math.round(originalSize * value / 100.0);
        }

        @Override
        public double fromPixels(int pixelValue, int originalSize) {
            return ((double) pixelValue) * 100.0 / originalSize;
        }

        @Override
        public String format(double value) {
            return PERCENT_FORMAT.format(value);
        }

        @Override
        public double parse(String text) throws ParseException {
            if (text.isEmpty()) {
                return 0;
            }
            return Utils.parseLocalizedDouble(text);
        }
    };

    private final String displayName;

    ResizeUnit(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Converts a value from this unit to pixels.
     */
    public abstract int toPixels(double value, int originalSize);

    /**
     * Converts a pixel value to this unit.
     */
    public abstract double fromPixels(int pixelValue, int originalSize);

    /**
     * Formats a value in this unit for display.
     */
    public abstract String format(double value);

    /**
     * Parses a string representation of a value in this unit.
     */
    public abstract double parse(String text) throws ParseException, NumberFormatException;
}
