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

package pixelitor.tools;

import pixelitor.Canvas;
import pixelitor.gui.View;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.tools.util.PPoint;

/**
 *  The different symmetry modes for brush tools, handling the transformation
 *  of points and delegation of drawing actions to a {@link SymmetryBrush}.
 */
public enum Symmetry {
    NONE("None", 1) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            throw new IllegalStateException("Should not be called, brushNo = " + brushNo);
        }
    }, VERTICAL_MIRROR("Vertical", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, p.mirrorVertically(canvasWidth));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, p.mirrorVertically(canvasWidth));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, p.mirrorVertically(canvasWidth));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return p.mirrorVertically(canvasWidth);
        }
    }, HORIZONTAL_MIRROR("Horizontal", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, p.mirrorHorizontally(canvasHeight));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, p.mirrorHorizontally(canvasHeight));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, p.mirrorHorizontally(canvasHeight));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return p.mirrorHorizontally(canvasHeight);
        }
    }, TWO_MIRRORS("Two Mirrors", 4) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            applyToAllBrushes(SymmetryBrush::startAt, brush, p);
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            applyToAllBrushes(SymmetryBrush::continueTo, brush, p);
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            applyToAllBrushes(SymmetryBrush::lineConnectTo, brush, p);
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            for (int i = 0; i < 4; i++) {
                brush.finishBrushStroke(i);
            }
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            return switch (brushNo) {
                case 1 -> p.mirrorVertically(canvasWidth);
                case 2 -> p.mirrorHorizontally(canvasHeight);
                case 3 -> p.mirrorBoth(canvasWidth, canvasHeight);
                default -> throw new IllegalArgumentException("brushNo = " + brushNo);
            };
        }

        // Helper to apply a brush action to all brushes
        private void applyToAllBrushes(BrushAction action, SymmetryBrush brush, PPoint p) {
            action.apply(brush, 0, p);             // original
            action.apply(brush, 1, transform(p, 1)); // vertical mirror
            action.apply(brush, 2, transform(p, 2)); // horizontal mirror
            action.apply(brush, 3, transform(p, 3)); // both mirrors
        }
    }, CENTRAL_SYMMETRY("Central Symmetry", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, p.mirrorBoth(canvasWidth, canvasHeight));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, p.mirrorBoth(canvasWidth, canvasHeight));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, p.mirrorBoth(canvasWidth, canvasHeight));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return p.mirrorBoth(canvasWidth, canvasHeight);
        }
    }, CENTRAL_3("Central 3", 3) {
        private static final double COS_120 = -0.5;
        private static final double SIN_120 = 0.8660254037844386;
        private static final double COS_240 = COS_120;
        private static final double SIN_240 = -SIN_120;

        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            applyToAllBrushes(SymmetryBrush::startAt, brush, p);
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            applyToAllBrushes(SymmetryBrush::continueTo, brush, p);
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            applyToAllBrushes(SymmetryBrush::lineConnectTo, brush, p);
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
            brush.finishBrushStroke(2);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            double x = p.getImX();
            double y = p.getImY();
            // coordinates relative to the center
            double relX = x - canvasCenterX;
            double relY = canvasCenterY - y; // calculate in upwards looking coords

            View view = p.getView();
            if (brushNo == 1) {
                return getRotatedPoint(view, relX, relY, COS_120, SIN_120);
            } else if (brushNo == 2) {
                return getRotatedPoint(view, relX, relY, COS_240, SIN_240);
            } else {
                throw new IllegalArgumentException("brushNo = " + brushNo);
            }
        }

        // Helper to apply a brush action to all brushes
        private void applyToAllBrushes(BrushAction action, SymmetryBrush brush, PPoint p) {
            action.apply(brush, 0, p);             // original point
            action.apply(brush, 1, transform(p, 1)); // 120 degree rotation
            action.apply(brush, 2, transform(p, 2)); // 240 degree rotation
        }

        private static PPoint getRotatedPoint(View view, double relX, double relY, double cosTheta, double sinTheta) {
            // rotate relative coordinates
            double rotX = relX * cosTheta - relY * sinTheta;
            double rotY = relX * sinTheta + relY * cosTheta;

            // translate back to the original coordinate system
            double finalX = canvasCenterX + rotX;
            double finalY = canvasCenterY - rotY;
            return PPoint.fromIm(finalX, finalY, view);
        }
    };

    // parameters of the *currently active* canvas
    private static int canvasWidth;
    private static int canvasHeight;
    private static double canvasCenterX;
    private static double canvasCenterY;

    /**
     * Updates the canvas dimensions used for symmetry calculations.
     */
    public static void activeCanvasSizeChanged(Canvas canvas) {
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();
        canvasCenterX = canvasWidth / 2.0;
        canvasCenterY = canvasHeight / 2.0;
    }

    private final String displayName;
    private final int numBrushes;

    Symmetry(String displayName, int numBrushes) {
        this.displayName = displayName;
        this.numBrushes = numBrushes;
    }

    // Abstract methods defining the core symmetry operations delegated by SymmetryBrush

    /**
     * Starts a brush stroke, applying symmetry.
     */
    public abstract void startAt(SymmetryBrush brush, PPoint p);

    /**
     * Continues a brush stroke, applying symmetry.
     */
    public abstract void continueTo(SymmetryBrush brush, PPoint p);

    /**
     * Connects the last point with a line, applying symmetry.
     */
    public abstract void lineConnectTo(SymmetryBrush brush, PPoint p);

    /**
     * Finishes the brush stroke, applying symmetry.
     */
    public abstract void finishBrushStroke(SymmetryBrush brush);

    /**
     * Transforms the given point, assuming that
     * it is the coordinate of the master (first) point
     */
    public abstract PPoint transform(PPoint p, int brushNo);

    /**
     * Returns the number of brushes required for this symmetry mode.
     */
    public int getNumBrushes() {
        return numBrushes;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @FunctionalInterface
    protected interface BrushAction {
        void apply(SymmetryBrush brush, int brushNo, PPoint point);
    }
}
