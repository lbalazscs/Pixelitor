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
    EUCLIDEAN("Euclidean") {
        @Override
        public double distance(int x1, int x2, int y1, int y2) {
            int dx = x1 - x2;
            int dy = y1 - y2;
            return Math.sqrt(dx * dx + dy * dy);
        }
    }, TAXICAB("Taxicab (Manhattan)") {
        @Override
        public double distance(int x1, int x2, int y1, int y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }
    }, MAX("Chessboard (Chebyshev)") {
        @Override
        public double distance(int x1, int x2, int y1, int y2) {
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

    public abstract double distance(int x1, int x2, int y1, int y2);
}
