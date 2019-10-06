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
     * The start of a new brush stroke.
     */
    void startAt(PPoint p);

    /**
     * The brush stroke should be continued to the given point.
     */
    void continueTo(PPoint p);

    /**
     * The brush stroke should be connected by a straight line
     * to the given point (usually because a shift-click happened),
     * even if it was already finished.
     */
    void lineConnectTo(PPoint p);

    /**
     * The brush stroke should be finished.
     */
    void finish();

    /**
     * Returns whether the brush is currently drawing.
     * This means that the mouse is down, except when automated (tracing, auto paint).
     *
     * While "brush stroke" methods are from the user perspective,
     * this is a low-level method, used only by delegating brushes.
     * In the case of shift-clicking, a single user-level brush stroke
     * is composed of multiple drawing sessions.
     */
    boolean isDrawing();

    /**
     * Initializes the brush for drawing.
     * This is a low-level method, used only by delegating brushes.
     */
    void initDrawing(PPoint p);

    /**
     * Manually free the resources used by this brush.
     */
    default void dispose() {}

    /**
     * Returns the previous position of the brush.
     */
    PPoint getPrevious();

    /**
     * Forces the given previous point on the brush
     */
    void setPrevious(PPoint previous);

    /**
     * Returns true if the brush has a previous position,
     * and false if it was not used yet.
     */
    default boolean hasPrevious() {
        return getPrevious() != null;
    }

    /**
     * Sets the Composition and the Graphics2D object
     * on which this brush will draw
     */
    void setTarget(Composition comp, Graphics2D g);

    /**
     * Sets the radius of the brush
     */
    void setRadius(double radius);

    /**
     * Used to determine the area saved for the undo.
     * Note that this can be bigger than the radius, because
     * some brushes use randomness and paint outside their radius.
     */
    double getEffectiveRadius();

    /**
     * Returns the space between the dabs.
     *
     * If the brush doesn't use uniform spacing, it can return
     * any spacing that looks good, or 0 to skip the decision.
     */
    double getPreferredSpacing();

    DebugNode getDebugNode();
}
