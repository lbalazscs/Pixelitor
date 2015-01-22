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

package pixelitor.transform;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Shape;

public class Handle {
    // All handle coordinates and sizes are in component space
    private static final int RADIUS = 5;
    private static final int DIAMETER = 2 * RADIUS;

    private int x;
    private int y;

    private final Cursor cursor;
    private Rectangle shape; // the shape of the handle

    public Handle(int cursorType) {
        this.cursor = Cursor.getPredefinedCursor(cursorType);
    }

    public Shape getShape() {
        return shape;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isOver(int px, int py)  {
        if(Math.abs(px - x) < RADIUS) {
            if(Math.abs(py - y) < RADIUS) {
               return true;
            }
        }
        return false;
    }

    public Cursor getCursor() {
        return cursor;
    }

    /**
     * This must be given in component-space ("mouse") coordinates
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        if(shape == null) {
            shape = new Rectangle(x - RADIUS, y - RADIUS, DIAMETER, DIAMETER);
        } else {
            shape.setBounds(x - RADIUS, y - RADIUS, DIAMETER, DIAMETER);
        }
    }

    @Override
    public String toString() {
        return "Handle{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
