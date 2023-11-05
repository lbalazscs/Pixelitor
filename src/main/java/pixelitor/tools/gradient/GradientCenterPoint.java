/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.gradient;

import pixelitor.gui.View;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PPoint;

/**
 * The draggable point at the half distance between the
 * start and end points, which moves the whole gradient.
 */
public class GradientCenterPoint extends DraggablePoint {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;

    public GradientCenterPoint(GradientDefiningPoint start,
                               GradientDefiningPoint end,
                               View view) {
        super("center", PPoint.halfPointBetween(start, end), view);
        this.start = start;
        this.end = end;
    }

    @Override
    public void setLocation(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;

        super.setLocation(x, y);

        // also move the start and end points
        start.translateOnlyThis(dx, dy);
        end.translateOnlyThis(dx, dy);
    }
}
