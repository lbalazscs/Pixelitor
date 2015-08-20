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

package pixelitor.tools;

import pixelitor.tools.brushes.SymmetryBrush;

/**
 * The "Mirror" option for brushes
 */
public enum Symmetry {
    NONE("None", 1) {
        @Override
        public void onDragStart(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onDragStart(0, x, y);
        }

        @Override
        public void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onNewMousePoint(0, x, y);
        }
    }, VERTICAL_MIRROR("Vertical", 2) {
        @Override
        public void onDragStart(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onDragStart(0, x, y);
            symmetryBrush.onDragStart(1, compositionWidth - x, y);
        }

        @Override
        public void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onNewMousePoint(0, x, y);
            symmetryBrush.onNewMousePoint(1, compositionWidth - x, y);
        }
    }, HORIZONTAL_MIRROR("Horizontal", 2) {
        @Override
        public void onDragStart(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onDragStart(0, x, y);
            symmetryBrush.onDragStart(1, x, compositionHeight - y);
        }

        @Override
        public void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onNewMousePoint(0, x, y);
            symmetryBrush.onNewMousePoint(1, x, compositionHeight - y);
        }
    }, TWO_MIRRORS("Two Mirrors", 4) {
        @Override
        public void onDragStart(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onDragStart(0, x, y);
            symmetryBrush.onDragStart(1, compositionWidth - x, y);
            symmetryBrush.onDragStart(2, x, compositionHeight - y);
            symmetryBrush.onDragStart(3, compositionWidth - x, compositionHeight - y);
        }

        @Override
        public void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onNewMousePoint(0, x, y);
            symmetryBrush.onNewMousePoint(1, compositionWidth - x, y);
            symmetryBrush.onNewMousePoint(2, x, compositionHeight - y);
            symmetryBrush.onNewMousePoint(3, compositionWidth - x, compositionHeight - y);
        }
    }, CENTRAL_SYMMETRY("Central Symmetry", 2) {
        @Override
        public void onDragStart(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onDragStart(0, x, y);
            symmetryBrush.onDragStart(1, compositionWidth - x, compositionHeight - y);
        }

        @Override
        public void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onNewMousePoint(0, x, y);
            symmetryBrush.onNewMousePoint(1, compositionWidth - x, compositionHeight - y);
        }
    }, CENTRAL_3("Central 3", 3) {
        private static final double cos120 = -0.5;
        private static final double sin120 = 0.86602540378443864676372317075294;
        private static final double cos240 = cos120;
        private static final double sin240 = -sin120;

        @Override
        public void onDragStart(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onDragStart(0, x, y);

            // coordinates relative to the center
            double relX = x - compositionCenterX;
            double relY = compositionCenterY - y; // calculate in upwards looking coords

            // coordinates rotated with 120 degrees
            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            // translate back to the original coordinate system
            int finalX = (int) (compositionCenterX + rotX);
            int finalY = (int) (compositionCenterY - rotY);

            symmetryBrush.onDragStart(1, finalX, finalY);

            // coordinates rotated with 240 degrees
            rotX = relX * cos240 - relY * sin240;
            rotY = relX * sin240 + relY * cos240;

            // translate back to the original coordinate system
            finalX = (int) (compositionCenterX + rotX);
            finalY = (int) (compositionCenterY - rotY);

            symmetryBrush.onDragStart(2, finalX, finalY);
        }

        @Override
        public void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y) {
            symmetryBrush.onNewMousePoint(0, x, y);

            double relX = x - compositionCenterX;
            double relY = compositionCenterY - y;

            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            int finalEndX = (int) (compositionCenterX + rotX);
            int finalEndY = (int) (compositionCenterY - rotY);

            symmetryBrush.onNewMousePoint(1, finalEndX, finalEndY);

            rotX = relX * cos240 - relY * sin240;
            rotY = relX * sin240 + relY * cos240;

            finalEndX = (int) (compositionCenterX + rotX);
            finalEndY = (int) (compositionCenterY - rotY);

            symmetryBrush.onNewMousePoint(2, finalEndX, finalEndY);
        }
    };

    private static int compositionWidth;
    private static int compositionHeight;
    private static double compositionCenterX;
    private static double compositionCenterY;

    public static void setCompositionSize(int w, int h) {
        compositionWidth = w;
        compositionHeight = h;
        compositionCenterX = w / 2.0;
        compositionCenterY = h / 2.0;
    }

    private final String guiName;
    private final int numBrushes;

    Symmetry(String guiName, int numBrushes) {
        this.guiName = guiName;
        this.numBrushes = numBrushes;
    }

    public abstract void onDragStart(SymmetryBrush symmetryBrush, double x, double y);

    public abstract void onNewMousePoint(SymmetryBrush symmetryBrush, double x, double y);

    public int getNumBrushes() {
        return numBrushes;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
