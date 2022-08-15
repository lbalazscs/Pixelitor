/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.View;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Something that can be drawn over the image as part of a tool's
 * functionality and can respond to mouse and keyboard events.
 */
public interface ToolWidget {
    /**
     * If a {@link DraggablePoint}'s handle contains the given
     * component space location, return the point, otherwise return null.
     */
    DraggablePoint findHandleAt(double x, double y);

    /**
     * Paint the widget on the given Graphics2D,
     * which is expected to be in component space
     */
    void paint(Graphics2D g);

    /**
     * The component-space coordinates of this widget
     * have to be recalculated based on the image-space coordinates
     * because the active view changed (zoom, canvas resize etc.)
     */
    void coCoordsChanged(View view);

    void imCoordsChanged(AffineTransform at, View view);

    void arrowKeyPressed(ArrowKey key, View view);
}
