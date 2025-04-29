/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.Drawable;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;

/**
 * The behavior of a brush used for drawing on a {@link Drawable}.
 * The received coordinates correspond to the mouse events;
 * they are not translated with the brush radius.
 */
public interface Brush extends Debuggable {
    /**
     * Starts a new brush stroke at the given point.
     */
    void startAt(PPoint p);

    /**
     * Continues the current brush stroke to the given point.
     */
    void continueTo(PPoint p);

    /**
     * Connects the last point of the stroke to the given point with a straight line segment.
     */
    void lineConnectTo(PPoint p);

    /**
     * Finishes the current brush stroke.
     */
    void finishBrushStroke();

    /**
     * Returns true if the brush is currently in an active drawing state.
     * This means that the mouse is down, except when automated (tracing, auto paint).
     *
     * This is a low-level method, used only by delegating brushes.
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
     * Releases any resources held by the brush.
     */
    void dispose();

    /**
     * Returns the previous position of the brush.
     */
    PPoint getPrevious();

    /**
     * Sets the given previous point on the brush.
     */
    void setPrevious(PPoint previous);

    /**
     * Returns true if the brush has a recorded previous position.
     */
    default boolean hasPrevious() {
        return getPrevious() != null;
    }

    /**
     * Sets the target {@link Drawable} and Graphics2D for drawing.
     */
    void setTarget(Drawable dr, Graphics2D g);

    /**
     * Sets the radius of the brush.
     */
    void setRadius(double radius);

    /**
     * Returns the maximum effective radius used during the last stroke, for undo purposes.
     * This can be bigger than the radius because some brushes paint
     * outside their radius. Since the radius can change during a brush
     * stroke (via keyboard), the maximum value must be returned.
     */
    double getMaxEffectiveRadius();

    /**
     * Returns the space between the between brush applications (dabs) in image-space pixels.
     *
     * If the brush doesn't use uniform spacing, it can return
     * any spacing that looks good, or 0 to skip the decision.
     */
    double getPreferredSpacing();
}
