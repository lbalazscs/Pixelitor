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

package pixelitor.tools.brushes;

import pixelitor.Composition;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.Graphics2D;

/**
 * A brush.
 * The received coordinates correspond to the mouse events,
 * they are not translated with the brush radius.
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
     * The start of a new brush stroke.
     */
    void onStrokeStart(PPoint p);

    /**
     * A new drawing mouse event has been received.
     */
    void onNewStrokePoint(PPoint p);

    /**
     * A shift-click happened.
     */
    void lineConnectTo(PPoint p);

    default void dispose() {}

    DebugNode getDebugNode();

    /**
     * Returns the space between the dabs.
     *
     * If the brush doesn't use uniform spacing, it can return
     * any spacing that looks good, or 0 to skip the decision.
     */
    double getPreferredSpacing();
}
