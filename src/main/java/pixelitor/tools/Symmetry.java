/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

/**
 *
 */
public enum Symmetry {
    NO_SYMMETRY {
        @Override
        public String toString() {
            return "None";
        }

        @Override
        public void drawPoint(Brushes brushes, int x, int y) {
            brushes.drawPoint(0, x, y);
        }

        @Override
        public void drawLine(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.drawLine(0, startX, startY, endX, endY);
        }
    }, VERTICAL_MIRROR {
        @Override
        public String toString() {
            return "Vertical";
        }

        @Override
        public void drawPoint(Brushes brushes, int x, int y) {
            brushes.drawPoint(0, x, y);
            brushes.drawPoint(1, compositionWidth - x, y);
        }

        @Override
        public void drawLine(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.drawLine(0, startX, startY, endX, endY);
            brushes.drawLine(1, compositionWidth - startX, startY, compositionWidth - endX, endY);
        }
    }, HORIZONTAL_MIRROR {
        @Override
        public String toString() {
            return "Horizontal";
        }

        @Override
        public void drawPoint(Brushes brushes, int x, int y) {
            brushes.drawPoint(0, x, y);
            brushes.drawPoint(1, x, compositionHeight - y);
        }

        @Override
        public void drawLine(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.drawLine(0, startX, startY, endX, endY);
            brushes.drawLine(1, startX, compositionHeight - startY, endX, compositionHeight - endY);
        }
    }, TWO_MIRRORS {
        @Override
        public String toString() {
            return "Two Mirrors";
        }

        @Override
        public void drawPoint(Brushes brushes, int x, int y) {
            brushes.drawPoint(0, x, y);
            brushes.drawPoint(1, compositionWidth - x, y);
            brushes.drawPoint(2, x, compositionHeight - y);
            brushes.drawPoint(3, compositionWidth - x, compositionHeight - y);
        }

        @Override
        public void drawLine(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.drawLine(0, startX, startY, endX, endY);
            brushes.drawLine(1, startX, compositionHeight - startY, endX, compositionHeight - endY);
            brushes.drawLine(2, compositionWidth - startX, startY, compositionWidth - endX, endY);
            brushes.drawLine(3, compositionWidth - startX, compositionHeight - startY, compositionWidth - endX, compositionHeight - endY);
        }
    }, CENTRAL_SYMMETRY {
        @Override
        public String toString() {
            return "Central Symmetry";
        }

        @Override
        public void drawPoint(Brushes brushes, int x, int y) {
            brushes.drawPoint(0, x, y);
            brushes.drawPoint(1, compositionWidth - x, compositionHeight - y);
        }

        @Override
        public void drawLine(Brushes brushes, int startX, int startY, int endX, int endY) {
            brushes.drawLine(0, startX, startY, endX, endY);
            brushes.drawLine(1, compositionWidth - startX, compositionHeight - startY, compositionWidth - endX, compositionHeight - endY);
        }
    };

    private static int compositionWidth;
    private static int compositionHeight;

    public static void setCompositionSize(int w, int h) {
        compositionWidth = w;
        compositionHeight = h;
    }

    public abstract void drawPoint(Brushes brushes, int x, int y);

    public abstract void drawLine(Brushes brushes, int startX, int startY, int endX, int endY);
}
