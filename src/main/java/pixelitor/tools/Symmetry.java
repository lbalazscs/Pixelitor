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
    }, HORIZONTAL_MIRROR("Horizontal", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, mirrorHorizontally(p));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, mirrorHorizontally(p));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, mirrorHorizontally(p));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return mirrorHorizontally(p);
        }
    }, VERTICAL_MIRROR("Vertical", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, mirrorVertically(p));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, mirrorVertically(p));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, mirrorVertically(p));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return mirrorVertically(p);
        }
    }, TWO_MIRRORS("Horizontal + Vertical", 4) {
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
                case 1 -> mirrorVertically(p);
                case 2 -> mirrorHorizontally(p);
                case 3 -> mirrorBoth(p);
                default -> throw new IllegalArgumentException("brushNo = " + brushNo);
            };
        }

        private void applyToAllBrushes(BrushAction action, SymmetryBrush brush, PPoint p) {
            action.apply(brush, 0, p); // original
            action.apply(brush, 1, transform(p, 1)); // vertical
            action.apply(brush, 2, transform(p, 2)); // horizontal
            action.apply(brush, 3, transform(p, 3)); // both
        }
    }, DIAGONAL_MIRROR_A("Diagonal /", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, transform(p, 1));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, transform(p, 1));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, transform(p, 1));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return transformDiagonalA(p);
        }
    }, DIAGONAL_MIRROR_B("Diagonal \\", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, transform(p, 1));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, transform(p, 1));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, transform(p, 1));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return transformDiagonalB(p);
        }
    }, CENTRAL_SYMMETRY("Central Symmetry", 2) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, mirrorBoth(p));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, mirrorBoth(p));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, mirrorBoth(p));
        }

        @Override
        public void finishBrushStroke(SymmetryBrush brush) {
            brush.finishBrushStroke(0);
            brush.finishBrushStroke(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return mirrorBoth(p);
        }
    }, CENTRAL_3("Central 3", 3) {
        private static final double COS_120 = -0.5;
        private static final double SIN_120 = 0.8660254037844386; // sqrt(3)/2
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
            // coordinates relative to the center
            double relX = p.getImX() - canvasCenterX;
            double relY = canvasCenterY - p.getImY(); // calculate in upwards looking coords
            View view = p.getView();

            return switch (brushNo) {
                case 1 -> getRotatedPoint(view, relX, relY, COS_120, SIN_120);
                case 2 -> getRotatedPoint(view, relX, relY, COS_240, SIN_240);
                default -> throw new IllegalArgumentException("brushNo = " + brushNo);
            };
        }

        private void applyToAllBrushes(BrushAction action, SymmetryBrush brush, PPoint p) {
            action.apply(brush, 0, p); // original
            action.apply(brush, 1, transform(p, 1)); // 120 degree rotation
            action.apply(brush, 2, transform(p, 2)); // 240 degree rotation
        }
    };

    // parameters of the currently active canvas
    private static double canvasWidth;
    private static double canvasHeight;
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

    // abstract methods defining the core symmetry operations delegated by SymmetryBrush

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

    private static PPoint mirrorVertically(PPoint p) {
        return PPoint.fromIm(canvasWidth - p.getImX(), p.getImY(), p.getView());
    }

    private static PPoint mirrorHorizontally(PPoint p) {
        return PPoint.fromIm(p.getImX(), canvasHeight - p.getImY(), p.getView());
    }

    private static PPoint mirrorBoth(PPoint p) {
        return PPoint.fromIm(canvasWidth - p.getImX(), canvasHeight - p.getImY(), p.getView());
    }

    private static PPoint transformDiagonalB(PPoint p) {
        double imX = p.getImX();
        double imY = p.getImY();
        double den = canvasWidth * canvasWidth + canvasHeight * canvasHeight;
        double mirrorImX = ((canvasWidth * canvasWidth - canvasHeight * canvasHeight) * imX + 2 * canvasWidth * canvasHeight * imY) / den;
        double mirrorImY = (2 * canvasWidth * canvasHeight * imX + (canvasHeight * canvasHeight - canvasWidth * canvasWidth) * imY) / den;

        return PPoint.fromIm(mirrorImX, mirrorImY, p.getView());
    }

    private static PPoint transformDiagonalA(PPoint p) {
        double imX = p.getImX();
        double imY = p.getImY();
        double den = canvasWidth * canvasWidth + canvasHeight * canvasHeight;
        double mirrorImX = ((canvasWidth * canvasWidth - canvasHeight * canvasHeight) * imX - 2 * canvasWidth * canvasHeight * imY + 2 * canvasHeight * canvasHeight * canvasWidth) / den;
        double mirrorImY = (-2 * canvasWidth * canvasHeight * imX + (canvasHeight * canvasHeight - canvasWidth * canvasWidth) * imY + 2 * canvasWidth * canvasWidth * canvasHeight) / den;

        return PPoint.fromIm(mirrorImX, mirrorImY, p.getView());
    }

    /**
     * Calculates a rotated point around the canvas center.
     */
    private static PPoint getRotatedPoint(View view, double relX, double relY, double cosTheta, double sinTheta) {
        // rotate relative coordinates
        double rotX = relX * cosTheta - relY * sinTheta;
        double rotY = relX * sinTheta + relY * cosTheta;

        // translate back to the original coordinate system
        double finalX = canvasCenterX + rotX;
        double finalY = canvasCenterY - rotY;
        return PPoint.fromIm(finalX, finalY, view);
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
