
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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Helps with interactive manipulation of selection
 */
public class TransformHelper {
    private TransformHelper() {
        // should not be instantiated
    }

    public static double calcAspectRatio(Rectangle rect) {
        if (rect.height > 0) {
            return (double) rect.width / rect.height;
        } else {
            return 0;
        }
    }

    public static boolean isResizeMode(int cursorType) {
        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
            case Cursor.SE_RESIZE_CURSOR:
            case Cursor.SW_RESIZE_CURSOR:
            case Cursor.NE_RESIZE_CURSOR:
            case Cursor.N_RESIZE_CURSOR:
            case Cursor.S_RESIZE_CURSOR:
            case Cursor.E_RESIZE_CURSOR:
            case Cursor.W_RESIZE_CURSOR:
                return true;
        }

        return false;
    }

    /**
     * Recalculate rect position and size, according to mouse offset and direction
     *
     * @param rect rectangle to adjust
     * @param cursorType user modification direction (N,S,W,E,NW,NE,SE,SW)
     * @param moveOffset offset must be calculated as: mousePos - mouseStartPos
     */
    public static void resize(Rectangle rect, int cursorType, Point moveOffset) {
        int offsetX = moveOffset.x;
        int offsetY = moveOffset.y;
        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
                rect.width -= offsetX;
                rect.height -= offsetY;
                rect.x += offsetX;
                rect.y += offsetY;
                break;
            case Cursor.SE_RESIZE_CURSOR:
                rect.width += offsetX;
                rect.height += offsetY;
                break;
            case Cursor.SW_RESIZE_CURSOR:
                rect.width -= offsetX;
                rect.height += offsetY;
                rect.x += offsetX;
                break;
            case Cursor.NE_RESIZE_CURSOR:
                rect.width += offsetX;
                rect.height -= offsetY;
                rect.y += offsetY;
                break;
            case Cursor.N_RESIZE_CURSOR:
                rect.height -= offsetY;
                rect.y += offsetY;
                break;
            case Cursor.S_RESIZE_CURSOR:
                rect.height += offsetY;
                break;
            case Cursor.E_RESIZE_CURSOR:
                rect.width += offsetX;
                break;
            case Cursor.W_RESIZE_CURSOR:
                rect.width -= offsetX;
                rect.x += offsetX;
                break;
        }
    }

    /**
     * Adjust rect to keep aspectRatio.
     * Recalculate rect x,y according to user direction
     *
     * @param rect rectangle to adjust
     * @param cursorType user modification direction (N,S,W,E,NW,NE,SE,SW)
     * @param aspectRatio aspect ratio of original rectangle. Required > 0
     */
    public static void keepAspectRatio(Rectangle rect, int cursorType, double aspectRatio) {

        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
            case Cursor.NE_RESIZE_CURSOR:
            case Cursor.SE_RESIZE_CURSOR:
            case Cursor.SW_RESIZE_CURSOR:
                // to find out which size adjust (width or height) we compare rect ratios
                // this allows to keep new rect most closely to user needs
                double aspectRatioNew = calcAspectRatio(rect);
                if (aspectRatioNew > aspectRatio) {
                    int height = (int) (rect.width / aspectRatio);
                    if (cursorType == Cursor.NW_RESIZE_CURSOR ||
                        cursorType == Cursor.NE_RESIZE_CURSOR) {
                        rect.y -= (height - rect.height);
                    }

                    rect.height = height;
                } else {
                    int width = (int) (rect.height * aspectRatio);
                    if (cursorType == Cursor.NW_RESIZE_CURSOR ||
                        cursorType == Cursor.SW_RESIZE_CURSOR) {
                        rect.x -= (width - rect.width);
                    }

                    rect.width = width;
                }
                break;
            case Cursor.N_RESIZE_CURSOR:
            case Cursor.S_RESIZE_CURSOR:
                // adjust width and center horizontally new rect
                int width = (int) (rect.height * aspectRatio);
                rect.x -= (width - rect.width) / 2;
                rect.width = width;
                break;
            case Cursor.E_RESIZE_CURSOR:
            case Cursor.W_RESIZE_CURSOR:
                // adjust height and center vertically new rect
                int height = (int) (rect.width / aspectRatio);
                rect.y -= (height - rect.height) / 2;
                rect.height = height;
                break;
        }
    }
}

