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

/**
 * Different distance metrics for calculating distances between two points.
 */
public enum Metric {
    /**
     * Squared Euclidean distance: (x₁-x₂)² + (y₁-y₂)².
     * Without the square root for better performance.
     */
    EUCLIDEAN_SQUARED("Euclidean", false) {
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
    MANHATTAN("Taxicab (Manhattan)", false) {
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
    CHEBYSHEV("Chessboard (Chebyshev)", false) {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
        }
    },

    /**
     * Minkowski distance with p=3: (|x₁-x₂|³ + |y₁-y₂|³)^(1/3)
     * (without the cube root)
     */
    MINKOWSKI_3_CUBED("Minkowski p=3", false) {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            int dx = Math.abs(x1 - x2);
            int dy = Math.abs(y1 - y2);
            return dx * dx * dx + dy * dy * dy;
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            double dx = Math.abs(x1 - x2);
            double dy = Math.abs(y1 - y2);
            return dx * dx * dx + dy * dy * dy;
        }
    },
    /**
     * Minkowski distance with p=-1: (|x₁-x₂|⁻¹ + |y₁-y₂|⁻¹)⁻¹
     */
    MINKOWSKI_NEG_1("Minkowski p=-1", false) {
        private static final double EPSILON = 1.0e-10;

        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            return distanceDouble(x1, y1, x2, y2);
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            double absX = Math.abs(x1 - x2);
            double absY = Math.abs(y1 - y2);
            // avoid division by zero
            absX = Math.max(absX, EPSILON);
            absY = Math.max(absY, EPSILON);
            return 1.0 / (1.0 / absX + 1.0 / absY);
        }
    }, POLAR_RAD("Polar Radial", true) {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            return distanceDouble(x1, y1, x2, y2);
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            // polar coordinates
            double theta1 = Math.atan2(y1, x1);
            double theta2 = Math.atan2(y2, x2);
            double r1 = Math.sqrt(x1 * x1 + y1 * y1);
            double r2 = Math.sqrt(x2 * x2 + y2 * y2);

            // angular difference considering periodicity
            double dTheta = Math.abs(theta1 - theta2);
            if (dTheta > Math.PI) {
                dTheta = 2 * Math.PI - dTheta;
            }

            double dr = r1 - r2;
            double dt = r1 * r2 * dTheta;
            return Math.sqrt(dr * dr + dt * dt);
        }
    }, POLAR_CON("Polar Concentric", true) {
        @Override
        public double distanceInt(int x1, int y1, int x2, int y2) {
            return distanceDouble(x1, y1, x2, y2);
        }

        @Override
        public double distanceDouble(double x1, double y1, double x2, double y2) {
            // polar coordinates
            double theta1 = Math.atan2(y1, x1);
            double theta2 = Math.atan2(y2, x2);
            double r1 = Math.sqrt(x1 * x1 + y1 * y1);
            double r2 = Math.sqrt(x2 * x2 + y2 * y2);

            // angular difference considering periodicity
            double dTheta = Math.abs(theta1 - theta2);
            if (dTheta > Math.PI) {
                dTheta = 2 * Math.PI - dTheta;
            }

            double dr = r1 - r2;
            return Math.sqrt(dr * dr + dTheta * dTheta);
        }
    };

    private final String displayName;
    private final boolean centered;

    Metric(String displayName, boolean centered) {
        this.displayName = displayName;
        this.centered = centered;
    }

    public boolean isCentered() {
        return centered;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * The distance with int precision.
     */
    public abstract double distanceInt(int x1, int y1, int x2, int y2);

    /**
     * The distance with double precision (slower).
     */
    public abstract double distanceDouble(double x1, double y1, double x2, double y2);

    public DistanceFunction asIntPrecisionDistance(int cx, int cy) {
        return centered
            ? (x1, y1, x2, y2) -> distanceInt((int) x1 - cx, (int) y1 - cy, (int) x2 - cx, (int) y2 - cy)
            : (x1, y1, x2, y2) -> distanceInt((int) x1, (int) y1, (int) x2, (int) y2);
    }

    public DistanceFunction asDoublePrecisionDistance(int cx, int cy) {
        return centered
            ? (x1, y1, x2, y2) -> distanceDouble(x1 - cx, y1 - cy, x2 - cx, y2 - cy)
            : this::distanceDouble;
    }

    /**
     * Abstracts away the precision of the calculations.
     */
    public interface DistanceFunction {
        double apply(double x1, double y1, double x2, double y2);
    }
}
