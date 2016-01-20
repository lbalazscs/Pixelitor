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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.filters.gui.RangeParam;

import java.awt.geom.Path2D;

/**
 * Renders a Lissajous curve
 */
public class Lissajous extends ShapeFilter {
    private static final int NUMBER_OF_STEPS = 2000;

    private final RangeParam a = new RangeParam("a", 1, 4, 42);
    private final RangeParam b = new RangeParam("b", 1, 5, 42);

    public Lissajous() {
        addParamsToFront(
                a,
                b
        );
    }

    @Override
    protected Path2D createShape(int width, int height) {
        Path2D shape = new Path2D.Double();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        double aVal = a.getValueAsDouble();
        double bVal = b.getValueAsDouble();

        double w = width / 2.0;
        double h = height / 2.0;
        double dt = (2 * Math.PI) / NUMBER_OF_STEPS;

        shape.moveTo(cx, cy);
        for (double t = 0; t < 2 * Math.PI; t += dt) {
            double x = w * FastMath.sin(aVal * t) + cx;
            double y = h * FastMath.sin(bVal * t) + cy;
            shape.lineTo(x, y);
        }
        shape.closePath();

        return shape;
    }
}