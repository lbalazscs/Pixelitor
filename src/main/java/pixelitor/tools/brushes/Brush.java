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

import java.awt.Graphics2D;

/**
 * A brush.
 */
public interface Brush {
    /**
     * Sets the Graphics2D object on which this brush will draw
     */
    void setTargetGraphics(Graphics2D g);

    /**
     * Sets the radius of the brush
     */
    void setRadius(int radius);

    /**
     * The start of a new brush stroke
     *
     * @param x        the x of the mouse event (NOT translated with the radius)
     * @param y        the y of the mouse event (NOT translated with the radius)
     */
    void onDragStart(int x, int y);

    /**
     * A new drawing mouse event has been received;
     *
     * @param x        the x of the mouse event (NOT translated with the radius)
     * @param y        the y of the mouse event (NOT translated with the radius)
     */
    void onNewMousePoint(int x, int y);

//    /**
//     * Draws a point, usually at the first mouse click
//     *
//     * @param g
//     * @param x        the x of the mouse event (NOT translated with the radius)
//     * @param y        the y of the mouse event (NOT translated with the radius)
//     * @param radius
//     */
//    void drawPoint(Graphics2D g, int x, int y, int radius);
//
//    /**
//     * Draws a line between the two points
//     *
//     * @param g
//     * @param startX   the x of the first mouse drag event(NOT translated with the radius)
//     * @param startY   the y of the first mouse drag event(NOT translated with the radius)
//     * @param endX     the x of the second mouse drag event(NOT translated with the radius)
//     * @param endY     the y of the second mouse drag event(NOT translated with the radius)
//     * @param radius
//     */
//    void drawLine(Graphics2D g, int startX, int startY, int endX, int endY, int radius);
//

    /**
     * TODO
     * Resets the state of the brush. Called when a new brushstroke is started.
     */
    void reset();
}
