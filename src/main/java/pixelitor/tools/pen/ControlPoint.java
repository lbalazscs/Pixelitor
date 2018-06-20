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

package pixelitor.tools.pen;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.DraggablePoint;

import java.awt.Color;

/**
 * A control point of a {@link CurvePoint}
 */
public class ControlPoint extends DraggablePoint {
    private final CurvePoint curvePoint;
    private ControlPoint sibling;

    public ControlPoint(String name, int x, int y, ImageComponent ic, CurvePoint curvePoint, Color color, Color activeColor) {
        super(name, x, y, ic, color, activeColor);
        this.curvePoint = curvePoint;
    }

    public void setSibling(ControlPoint sibling) {
        this.sibling = sibling;
    }

    // the third argument is needed to avoid infinite loops
    public void setLocation(int x, int y) {
        if (curvePoint.isSymmetric()) {
            int dx = x - curvePoint.x;
            int dy = y - curvePoint.y;

            sibling.setLocationOnlyForThis(curvePoint.x - dx, curvePoint.y - dy);
        }

        super.setLocation(x, y);
    }

    @Override
    protected void afterMouseReleasedActions() {
        calcImCoords();
        if (curvePoint.isSymmetric()) {
            sibling.calcImCoords();
        }
    }
}
