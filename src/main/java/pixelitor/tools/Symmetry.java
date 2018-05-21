/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
        public void onStrokeStart(SymmetryBrush brush, double x, double y) {
            brush.onStrokeStart(0, x, y);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, double x, double y) {
            brush.onNewStrokePoint(0, x, y);
        }
    }, VERTICAL_MIRROR("Vertical", 2) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, double x, double y) {
            brush.onStrokeStart(0, x, y);
            brush.onStrokeStart(1, compWidth - x, y);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, double x, double y) {
            brush.onNewStrokePoint(0, x, y);
            brush.onNewStrokePoint(1, compWidth - x, y);
        }
    }, HORIZONTAL_MIRROR("Horizontal", 2) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, double x, double y) {
            brush.onStrokeStart(0, x, y);
            brush.onStrokeStart(1, x, compHeight - y);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, double x, double y) {
            brush.onNewStrokePoint(0, x, y);
            brush.onNewStrokePoint(1, x, compHeight - y);
        }
    }, TWO_MIRRORS("Two Mirrors", 4) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, double x, double y) {
            brush.onStrokeStart(0, x, y);
            brush.onStrokeStart(1, compWidth - x, y);
            brush.onStrokeStart(2, x, compHeight - y);
            brush.onStrokeStart(3, compWidth - x, compHeight - y);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, double x, double y) {
            brush.onNewStrokePoint(0, x, y);
            brush.onNewStrokePoint(1, compWidth - x, y);
            brush.onNewStrokePoint(2, x, compHeight - y);
            brush.onNewStrokePoint(3, compWidth - x, compHeight - y);
        }
    }, CENTRAL_SYMMETRY("Central Symmetry", 2) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, double x, double y) {
            brush.onStrokeStart(0, x, y);
            brush.onStrokeStart(1, compWidth - x, compHeight - y);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, double x, double y) {
            brush.onNewStrokePoint(0, x, y);
            brush.onNewStrokePoint(1, compWidth - x, compHeight - y);
        }
    }, CENTRAL_3("Central 3", 3) {
        private static final double cos120 = -0.5;
        private static final double sin120 = 0.86602540378443864676372317075294;
        private static final double cos240 = cos120;
        private static final double sin240 = -sin120;

        @Override
        public void onStrokeStart(SymmetryBrush brush, double x, double y) {
            brush.onStrokeStart(0, x, y);

            // coordinates relative to the center
            double relX = x - compCenterX;
            double relY = compCenterY - y; // calculate in upwards looking coords

            // coordinates rotated with 120 degrees
            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            // translate back to the original coordinate system
            int finalX = (int) (compCenterX + rotX);
            int finalY = (int) (compCenterY - rotY);

            brush.onStrokeStart(1, finalX, finalY);

            // coordinates rotated with 240 degrees
            rotX = relX * cos240 - relY * sin240;
            rotY = relX * sin240 + relY * cos240;

            // translate back to the original coordinate system
            finalX = (int) (compCenterX + rotX);
            finalY = (int) (compCenterY - rotY);

            brush.onStrokeStart(2, finalX, finalY);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, double x, double y) {
            brush.onNewStrokePoint(0, x, y);

            double relX = x - compCenterX;
            double relY = compCenterY - y;

            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            int finalEndX = (int) (compCenterX + rotX);
            int finalEndY = (int) (compCenterY - rotY);

            brush.onNewStrokePoint(1, finalEndX, finalEndY);

            rotX = relX * cos240 - relY * sin240;
            rotY = relX * sin240 + relY * cos240;

            finalEndX = (int) (compCenterX + rotX);
            finalEndY = (int) (compCenterY - rotY);

            brush.onNewStrokePoint(2, finalEndX, finalEndY);
        }
    };

    private static int compWidth;
    private static int compHeight;
    private static double compCenterX;
    private static double compCenterY;

    public static void setCompositionSize(int w, int h) {
        compWidth = w;
        compHeight = h;
        compCenterX = w / 2.0;
        compCenterY = h / 2.0;
    }

    private final String guiName;
    private final int numBrushes;

    Symmetry(String guiName, int numBrushes) {
        this.guiName = guiName;
        this.numBrushes = numBrushes;
    }

    public abstract void onStrokeStart(SymmetryBrush brush, double x, double y);

    public abstract void onNewStrokePoint(SymmetryBrush brush, double x, double y);

    public int getNumBrushes() {
        return numBrushes;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
