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

package pixelitor.tools.brushes;

import java.awt.Rectangle;

public class BrushAffectedArea {
    // affected area coordinates
    private int minX = 0;
    private int minY = 0;
    private int maxX = 0;
    private int maxY = 0;

    public void updateAffectedCoordinates(int x, int y) {
        if(x > maxX) {
            maxX = x;
        } else if(x < minX) {
            minX = x;
        }

        if(y > maxY) {
            maxY = y;
        } else if(y < minY) {
            minY = y;
        }
    }

    public void initAffectedCoordinates(int x, int y) {
        minX = x;
        minY = y;
        maxX = x;
        maxY = y;
    }

    /**
     * Calculates the rectangle affected by a brush stroke for the undo mechanism
     */
    public Rectangle getRectangleAffectedByBrush(int radius) {
        // To be on the safe side, save a little more than necessary - some brushes have randomness
        int radius2 = 2 * radius;
        int radius4 = 4 * radius;

        int saveX = minX - radius2;
        int saveY = minY - radius2;
        int saveWidth = maxX - minX + radius4;
        int saveHeight = maxY - minY + radius4;
        Rectangle rectangleAffectedByBrush = new Rectangle(saveX, saveY, saveWidth, saveHeight);
        return rectangleAffectedByBrush;
    }
}
