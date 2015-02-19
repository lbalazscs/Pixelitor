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

/**
 * The "Mirror" option for brushes
 */
public enum Symmetry {
    NO_SYMMETRY("None") {
        @Override
        public void onDragStart(Brushes brushes, int x, int y) {
            brushes.onDragStart(0, x, y);
        }

        @Override
        public void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.onNewMousePoint(0, startX, startY, endX, endY);
        }
    }, VERTICAL_MIRROR("Vertical") {
        @Override
        public void onDragStart(Brushes brushes, int x, int y) {
            brushes.onDragStart(0, x, y);
            brushes.onDragStart(1, compositionWidth - x, y);
        }

        @Override
        public void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.onNewMousePoint(0, startX, startY, endX, endY);
            brushes.onNewMousePoint(1, compositionWidth - startX, startY, compositionWidth - endX, endY);
        }
    }, HORIZONTAL_MIRROR("Horizontal") {
        @Override
        public void onDragStart(Brushes brushes, int x, int y) {
            brushes.onDragStart(0, x, y);
            brushes.onDragStart(1, x, compositionHeight - y);
        }

        @Override
        public void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.onNewMousePoint(0, startX, startY, endX, endY);
            brushes.onNewMousePoint(1, startX, compositionHeight - startY, endX, compositionHeight - endY);
        }
    }, TWO_MIRRORS("Two Mirrors") {
        @Override
        public void onDragStart(Brushes brushes, int x, int y) {
            brushes.onDragStart(0, x, y);
            brushes.onDragStart(1, compositionWidth - x, y);
            brushes.onDragStart(2, x, compositionHeight - y);
            brushes.onDragStart(3, compositionWidth - x, compositionHeight - y);
        }

        @Override
        public void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.onNewMousePoint(0, startX, startY, endX, endY);
            brushes.onNewMousePoint(1, startX, compositionHeight - startY, endX, compositionHeight - endY);
            brushes.onNewMousePoint(2, compositionWidth - startX, startY, compositionWidth - endX, endY);
            brushes.onNewMousePoint(3, compositionWidth - startX, compositionHeight - startY, compositionWidth - endX, compositionHeight - endY);
        }
    }, CENTRAL_SYMMETRY("Central Symmetry") {
        @Override
        public void onDragStart(Brushes brushes, int x, int y) {
            brushes.onDragStart(0, x, y);
            brushes.onDragStart(1, compositionWidth - x, compositionHeight - y);
        }

        @Override
        public void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.onNewMousePoint(0, startX, startY, endX, endY);
            brushes.onNewMousePoint(1, compositionWidth - startX, compositionHeight - startY, compositionWidth - endX, compositionHeight - endY);
        }
    }, CENTRAL_3("Central 3") {
        private static final double cos120 = -0.5;
        private static final double sin120 = 0.86602540378443864676372317075294;
        private static final double cos240 = cos120;
        private static final double sin240 = -sin120;

        @Override
        public void onDragStart(Brushes brushes, int x, int y) {
            brushes.onDragStart(0, x, y);

            // coordinates relative to the center
            double relX = x - compositionCenterX;
            double relY = compositionCenterY - y; // calculate in upwards looking coords

            // coordinates rotated with 120 degrees
            double rotX = relX * cos120 - relY * sin120;
            double rotY = relX * sin120 + relY * cos120;

            // translate back to the original coordinate system
            int finalX = (int) (compositionCenterX + rotX);
            int finalY = (int) (compositionCenterY - rotY);

            brushes.onDragStart(1, finalX, finalY);

            // coordinates rotated with 240 degrees
            rotX = relX * cos240 - relY * sin240;
            rotY = relX * sin240 + relY * cos240;

            // translate back to the original coordinate system
            finalX = (int) (compositionCenterX + rotX);
            finalY = (int) (compositionCenterY - rotY);

            brushes.onDragStart(2, finalX, finalY);
        }

        @Override
        public void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.onNewMousePoint(0, startX, startY, endX, endY);

            double relStartX = startX - compositionCenterX;
            double relStartY = compositionCenterY - startY;
            double relEndX = endX - compositionCenterX;
            double relEndY = compositionCenterY - endY;

            double rotStartX = relStartX * cos120 - relStartY * sin120;
            double rotStartY = relStartX * sin120 + relStartY * cos120;
            double rotEndX = relEndX * cos120 - relEndY * sin120;
            double rotEndY = relEndX * sin120 + relEndY * cos120;

            int finalStartX = (int) (compositionCenterX + rotStartX);
            int finalStartY = (int) (compositionCenterY - rotStartY);
            int finalEndX = (int) (compositionCenterX + rotEndX);
            int finalEndY = (int) (compositionCenterY - rotEndY);

            brushes.onNewMousePoint(1, finalStartX, finalStartY, finalEndX, finalEndY);

            rotStartX = relStartX * cos240 - relStartY * sin240;
            rotStartY = relStartX * sin240 + relStartY * cos240;
            rotEndX = relEndX * cos240 - relEndY * sin240;
            rotEndY = relEndX * sin240 + relEndY * cos240;

            finalStartX = (int) (compositionCenterX + rotStartX);
            finalStartY = (int) (compositionCenterY - rotStartY);
            finalEndX = (int) (compositionCenterX + rotEndX);
            finalEndY = (int) (compositionCenterY - rotEndY);

            brushes.onNewMousePoint(2, finalStartX, finalStartY, finalEndX, finalEndY);
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

    Symmetry(String guiName) {
        this.guiName = guiName;
    }

    public abstract void onDragStart(Brushes brushes, int x, int y);

    public abstract void onNewMousePoint(Brushes brushes, int startX, int startY, int endX, int endY);

    @Override
    public String toString() {
        return guiName;
    }
}
