/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.Composition;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * A brush.
 */
public interface Brush {
    /**
     * Sets the Composition and the Graphics2D object on which this brush will draw
     */
    void setTarget(Composition comp, Graphics2D g);

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
    void onDragStart(double x, double y);

    /**
     * A new drawing mouse event has been received;
     *
     * @param x        the x of the mouse event (NOT translated with the radius)
     * @param y        the y of the mouse event (NOT translated with the radius)
     */
    void onNewMousePoint(double x, double y);

    default void dispose() {}

    DebugNode getDebugNode();
}
