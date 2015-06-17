/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

/**
 * https://en.wikipedia.org/wiki/Metric_%28mathematics%29
 */
public enum Metric {
    EUCLIDEAN_SQUARED("Euclidean") {
        @Override
        public double distanceInt(int x1, int x2, int y1, int y2) {
            int dx = x1 - x2;
            int dy = y1 - y2;
            return dx * dx + dy * dy; // much faster without square root
        }

        @Override
        public double distanceDouble(double x1, double x2, double y1, double y2) {
            double dx = x1 - x2;
            double dy = y1 - y2;
            return dx * dx + dy * dy; // much faster without square root
        }
    }, TAXICAB("Taxicab (Manhattan)") {
        @Override
        public double distanceInt(int x1, int x2, int y1, int y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }

        @Override
        public double distanceDouble(double x1, double x2, double y1, double y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }
    }, MAX("Chessboard (Chebyshev)") {
        @Override
        public double distanceInt(int x1, int x2, int y1, int y2) {
            return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
        }

        @Override
        public double distanceDouble(double x1, double x2, double y1, double y2) {
            return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
        }
    };

    private final String guiName;

    Metric(String guiName) {
        this.guiName = guiName;
    }

    @Override
    public String toString() {
        return guiName;
    }

    public abstract double distanceInt(int x1, int x2, int y1, int y2);

    // a slower version with double arguments
    public abstract double distanceDouble(double x1, double x2, double y1, double y2);
}
