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

package pixelitor.tools;

import pixelitor.Canvas;
import pixelitor.gui.View;
import pixelitor.tools.brushes.SymmetryBrush;
import pixelitor.tools.util.PPoint;

/**
 * The "Mirror" option for brushes
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
        public void finish(SymmetryBrush brush) {
            brush.finish(0);
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
        public void finish(SymmetryBrush brush) {
            brush.finish(0);
            brush.finish(1);
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
        public void finish(SymmetryBrush brush) {
            brush.finish(0);
            brush.finish(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return p.mirrorHorizontally(canvasHeight);
        }
    }, TWO_MIRRORS("Two Mirrors", 4) {
        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);
            brush.startAt(1, p.mirrorVertically(canvasWidth));
            brush.startAt(2, p.mirrorHorizontally(canvasHeight));
            brush.startAt(3, p.mirrorBoth(canvasWidth, canvasHeight));
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);
            brush.continueTo(1, p.mirrorVertically(canvasWidth));
            brush.continueTo(2, p.mirrorHorizontally(canvasHeight));
            brush.continueTo(3, p.mirrorBoth(canvasWidth, canvasHeight));
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);
            brush.lineConnectTo(1, p.mirrorVertically(canvasWidth));
            brush.lineConnectTo(2, p.mirrorHorizontally(canvasHeight));
            brush.lineConnectTo(3, p.mirrorBoth(canvasWidth, canvasHeight));
        }

        @Override
        public void finish(SymmetryBrush brush) {
            brush.finish(0);
            brush.finish(1);
            brush.finish(2);
            brush.finish(3);
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
        public void finish(SymmetryBrush brush) {
            brush.finish(0);
            brush.finish(1);
        }

        @Override
        public PPoint transform(PPoint p, int brushNo) {
            assert brushNo == 1 : "brushNo = " + brushNo;
            return p.mirrorBoth(canvasWidth, canvasHeight);
        }
    }, CENTRAL_3("Central 3", 3) {
        private static final double cos120 = -0.5;
        private static final double sin120 = 0.8660254037844386;
        private static final double cos240 = cos120;
        private static final double sin240 = -sin120;

        @Override
        public void startAt(SymmetryBrush brush, PPoint p) {
            brush.startAt(0, p);

            double x = p.getImX();
            double y = p.getImY();
            // coordinates relative to the center
            double relX = x - canvasCenterX;
            double relY = canvasCenterY - y; // calculate in upwards looking coords

            View view = p.getView();

            PPoint p1 = getRotatedPoint1(view, relX, relY);
            brush.startAt(1, p1);

            PPoint p2 = getRotatedPoint2(view, relX, relY);
            brush.startAt(2, p2);
        }

        private static PPoint getRotatedPoint1(View view, double relX, double relY) {
            // coordinates rotated with 120 degrees
            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            // translate back to the original coordinate system
            double finalX = canvasCenterX + rotX;
            double finalY = canvasCenterY - rotY;
            return PPoint.fromIm(finalX, finalY, view);
        }

        private static PPoint getRotatedPoint2(View view, double relX, double relY) {
            // coordinates rotated with 240 degrees
            double rotX = relX * cos240 - relY * sin240;
            double rotY = relX * sin240 + relY * cos240;

            // translate back to the original coordinate system
            double finalX = canvasCenterX + rotX;
            double finalY = canvasCenterY - rotY;
            return PPoint.fromIm(finalX, finalY, view);
        }

        @Override
        public void continueTo(SymmetryBrush brush, PPoint p) {
            brush.continueTo(0, p);

            double x = p.getImX();
            double y = p.getImY();
            // coordinates relative to the center
            double relX = x - canvasCenterX;
            double relY = canvasCenterY - y; // calculate in upwards looking coords

            View view = p.getView();

            PPoint p1 = getRotatedPoint1(view, relX, relY);
            brush.continueTo(1, p1);

            PPoint p2 = getRotatedPoint2(view, relX, relY);
            brush.continueTo(2, p2);
        }

        @Override
        public void lineConnectTo(SymmetryBrush brush, PPoint p) {
            brush.lineConnectTo(0, p);

            double x = p.getImX();
            double y = p.getImY();
            // coordinates relative to the center
            double relX = x - canvasCenterX;
            double relY = canvasCenterY - y; // calculate in upwards looking coords

            View view = p.getView();

            PPoint p1 = getRotatedPoint1(view, relX, relY);
            brush.lineConnectTo(1, p1);

            PPoint p2 = getRotatedPoint2(view, relX, relY);
            brush.lineConnectTo(2, p2);
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
                return getRotatedPoint1(view, relX, relY);
            } else if (brushNo == 2) {
                return getRotatedPoint2(view, relX, relY);
            } else {
                throw new IllegalArgumentException("brushNo = " + brushNo);
            }
        }

        @Override
        public void finish(SymmetryBrush brush) {
            brush.finish(0);
            brush.finish(1);
            brush.finish(2);
        }
    };

    private static int canvasWidth;
    private static int canvasHeight;
    private static double canvasCenterX;
    private static double canvasCenterY;

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

    public abstract void startAt(SymmetryBrush brush, PPoint p);

    public abstract void continueTo(SymmetryBrush brush, PPoint p);

    public abstract void lineConnectTo(SymmetryBrush brush, PPoint p);

    public abstract void finish(SymmetryBrush brush);

    /**
     * Transforms the given point, assuming that
     * it is the coordinate of the master (first) point
     */
    public abstract PPoint transform(PPoint p, int brushNo);

    public int getNumBrushes() {
        return numBrushes;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
