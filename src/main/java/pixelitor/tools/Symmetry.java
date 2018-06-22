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

import pixelitor.gui.ImageComponent;
import pixelitor.tools.brushes.SymmetryBrush;

/**
 * The "Mirror" option for brushes
 */
public enum Symmetry {
    NONE("None", 1) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, PPoint p) {
            brush.onStrokeStart(0, p);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, PPoint p) {
            brush.onNewStrokePoint(0, p);
        }
    }, VERTICAL_MIRROR("Vertical", 2) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, PPoint p) {
            brush.onStrokeStart(0, p);
            brush.onStrokeStart(1, p.mirrorVertically(compWidth));
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, PPoint p) {
            brush.onNewStrokePoint(0, p);
            brush.onNewStrokePoint(1, p.mirrorVertically(compWidth));
        }
    }, HORIZONTAL_MIRROR("Horizontal", 2) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, PPoint p) {
            brush.onStrokeStart(0, p);
            brush.onStrokeStart(1, p.mirrorHorizontally(compHeight));
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, PPoint p) {
            brush.onNewStrokePoint(0, p);
            brush.onNewStrokePoint(1, p.mirrorHorizontally(compHeight));
        }
    }, TWO_MIRRORS("Two Mirrors", 4) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, PPoint p) {
            brush.onStrokeStart(0, p);
            brush.onStrokeStart(1, p.mirrorVertically(compWidth));
            brush.onStrokeStart(2, p.mirrorHorizontally(compHeight));
            brush.onStrokeStart(3, p.mirrorBoth(compWidth, compHeight));
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, PPoint p) {
            brush.onNewStrokePoint(0, p);
            brush.onNewStrokePoint(1, p.mirrorVertically(compWidth));
            brush.onNewStrokePoint(2, p.mirrorHorizontally(compHeight));
            brush.onNewStrokePoint(3, p.mirrorBoth(compWidth, compHeight));
        }
    }, CENTRAL_SYMMETRY("Central Symmetry", 2) {
        @Override
        public void onStrokeStart(SymmetryBrush brush, PPoint p) {
            brush.onStrokeStart(0, p);
            brush.onStrokeStart(1, p.mirrorBoth(compWidth, compHeight));
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, PPoint p) {
            brush.onNewStrokePoint(0, p);
            brush.onNewStrokePoint(1, p.mirrorBoth(compWidth, compHeight));
        }
    }, CENTRAL_3("Central 3", 3) {
        private static final double cos120 = -0.5;
        private static final double sin120 = 0.86602540378443864676372317075294;
        private static final double cos240 = cos120;
        private static final double sin240 = -sin120;

        @Override
        public void onStrokeStart(SymmetryBrush brush, PPoint p) {
            brush.onStrokeStart(0, p);

            double x = p.getImX();
            double y = p.getImY();
            // coordinates relative to the center
            double relX = x - compCenterX;
            double relY = compCenterY - y; // calculate in upwards looking coords

            ImageComponent ic = p.getIC();

            PPoint p1 = getRotatedPoint1(ic, relX, relY);
            brush.onStrokeStart(1, p1);

            PPoint finalPos = getRotatedPoint2(ic, relX, relY);
            brush.onStrokeStart(2, finalPos);
        }

        private PPoint getRotatedPoint1(ImageComponent ic, double relX, double relY) {
            // coordinates rotated with 120 degrees
            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            // translate back to the original coordinate system
            double finalX = compCenterX + rotX;
            double finalY = compCenterY - rotY;
            return new PPoint.Image(ic, finalX, finalY);
        }

        private PPoint getRotatedPoint2(ImageComponent ic, double relX, double relY) {
            // coordinates rotated with 240 degrees
            double rotX = relX * cos240 - relY * sin240;
            double rotY = relX * sin240 + relY * cos240;

            // translate back to the original coordinate system
            double finalX = (int) (compCenterX + rotX);
            double finalY = (int) (compCenterY - rotY);
            return new PPoint.Image(ic, finalX, finalY);
        }

        @Override
        public void onNewStrokePoint(SymmetryBrush brush, PPoint p) {
            brush.onNewStrokePoint(0, p);

            double x = p.getImX();
            double y = p.getImY();
            // coordinates relative to the center
            double relX = x - compCenterX;
            double relY = compCenterY - y; // calculate in upwards looking coords

            ImageComponent ic = p.getIC();

            PPoint p1 = getRotatedPoint1(ic, relX, relY);
            brush.onNewStrokePoint(1, p1);

            PPoint finalPos = getRotatedPoint2(ic, relX, relY);
            brush.onNewStrokePoint(2, finalPos);
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

    public abstract void onStrokeStart(SymmetryBrush symmetryBrush, PPoint p);

    public abstract void onNewStrokePoint(SymmetryBrush brush, PPoint p);

    public int getNumBrushes() {
        return numBrushes;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
