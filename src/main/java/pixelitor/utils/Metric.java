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

package pixelitor.utils;

/**
 * Different distance metrics for calculating distances between two points.
 */
public enum Metric {
    /**
     * Squared Euclidean distance: (x₁-x₂)² + (y₁-y₂)².
     * Without the square root for better performance.
     */
    EUCLIDEAN_SQUARED("Euclidean") {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            int dx = x1 - x2;
            int dy = y1 - y2;
            return dx * dx + dy * dy;
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            double dx = x1 - x2;
            double dy = y1 - y2;
            return dx * dx + dy * dy;
        }
    },
    /**
     * Manhattan (Taxicab) distance: |x₁-x₂| + |y₁-y₂|.
     * Represents the distance a taxi would drive in a city laid out in a grid-like pattern.
     */
    MANHATTAN("Taxicab (Manhattan)") {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }
    },
    /**
     * Chebyshev (Chessboard) distance: max(|x₁-x₂|, |y₁-y₂|).
     * Represents the minimum number of moves a king would need to make on a chessboard.
     */
    CHEBYSHEV("Chessboard (Chebyshev)") {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
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

    /**
     * The distance with int precision.
     */
    public abstract double distanceInt(int x1, int y1, int x2, int y2);

    /**
     * The distance with double precision (slower).
     */
    public abstract double distanceDouble(double x1, double y1, double x2, double y2);

    public DistanceFunction asIntPrecisionDistance() {
        return (x1, y1, x2, y2) -> distanceInt((int) x1, (int) y1, (int) x2, (int) y2);
    }

    public DistanceFunction asDoublePrecisionDistance() {
        return this::distanceDouble;
    }

    /**
     * Abstracts away the precision of the calculations.
     */
    public interface DistanceFunction {
        double apply(double x1, double y1, double x2, double y2);
    }
}
