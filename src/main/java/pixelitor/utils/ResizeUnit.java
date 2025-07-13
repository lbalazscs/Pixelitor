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
    PIXELS("pixels", false) {
        @Override
        public int toPixels(double value, int originalSize, int dpi) {
            return (int) Math.round(value);
        }

        @Override
        public double fromPixels(int pixelValue, int originalSize, int dpi) {
            return pixelValue;
        }

        @Override
        public String format(double value) {
            return String.valueOf((int) Math.round(value));
        }

        @Override
        public double parse(String text) throws NumberFormatException {
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        }
    }, PERCENTAGE("percent", false) {
        private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#0.00");

        @Override
        public int toPixels(double value, int originalSize, int dpi) {
            return (int) Math.round(originalSize * value / 100.0);
        }

        @Override
        public double fromPixels(int pixelValue, int originalSize, int dpi) {
            return ((double) pixelValue) * 100.0 / originalSize;
        }

        @Override
        public String format(double value) {
            return PERCENT_FORMAT.format(value);
        }

        @Override
        public double parse(String text) throws ParseException {
            return text.isEmpty() ? 0 : Utils.parseLocalizedDouble(text);
        }
    }, CENTIMETERS("cm", true) {
        private static final NumberFormat FORMAT = new DecimalFormat("#0.00");
        private static final double INCH_TO_CM = 2.54;

        @Override
        public int toPixels(double value, int originalSize, int dpi) {
            return (int) Math.round(value / INCH_TO_CM * dpi);
        }

        @Override
        public double fromPixels(int pixelValue, int originalSize, int dpi) {
            return ((double) pixelValue) / dpi * INCH_TO_CM;
        }

        @Override
        public String format(double value) {
            return FORMAT.format(value);
        }

        @Override
        public double parse(String text) throws ParseException {
            return text.isEmpty() ? 0 : Utils.parseLocalizedDouble(text);
        }
    }, INCHES("inches", true) {
        private static final NumberFormat FORMAT = new DecimalFormat("#0.00");

        @Override
        public int toPixels(double value, int originalSize, int dpi) {
            return (int) Math.round(value * dpi);
        }

        @Override
        public double fromPixels(int pixelValue, int originalSize, int dpi) {
            return ((double) pixelValue) / dpi;
        }

        @Override
        public String format(double value) {
            return FORMAT.format(value);
        }

        @Override
        public double parse(String text) throws ParseException {
            return text.isEmpty() ? 0 : Utils.parseLocalizedDouble(text);
        }
    };

    private final String displayName;
    private final boolean physical;

    ResizeUnit(String displayName, boolean physical) {
        this.displayName = displayName;
        this.physical = physical;
    }

    public boolean isPhysical() {
        return physical;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Converts a value from this unit to pixels.
     */
    public abstract int toPixels(double value, int originalSize, int dpi);

    /**
     * Converts a pixel value to this unit.
     */
    public abstract double fromPixels(int pixelValue, int originalSize, int dpi);

    /**
     * Formats a value in this unit for display.
     */
    public abstract String format(double value);

    /**
     * Parses a string representation of a value in this unit.
     */
    public abstract double parse(String text) throws ParseException, NumberFormatException;
}
