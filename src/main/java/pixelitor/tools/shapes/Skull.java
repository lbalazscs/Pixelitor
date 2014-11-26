/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.shapes;

import java.awt.geom.Path2D;

public class Skull extends GeneralShape {
    public Skull(int x, int y, int width, int height) {
        float cp1X; // x of control point 1
        float cp1Y; // y of control point 1
        float cp2X; // x of control point 2
        float cp2Y; // y of control point 2
        float epX;  // x of end point
        float epY;  // y of end point

        path.setWindingRule(Path2D.WIND_EVEN_ODD);

        epX = x + 0.026878307f * width;
        epY = y + 0.5058735f * height;
        path.moveTo(epX, epY);

        epX = x + 0.026878307f * width;
        epY = y + 0.5058735f * height;
        path.lineTo(epX, epY);

        epX = x + 0.587483f * width;
        epY = y + 0.0036021383f * height;
        path.lineTo(epX, epY);

        epX = x + 0.99449736f * width;
        epY = y + 0.89411575f * height;
        path.lineTo(epX, epY);

        epX = x + 0.026878307f * width;
        epY = y + 0.5058735f * height;
        path.lineTo(epX, epY);

        path.closePath();

        epX = x + 0.4876493f * width;
        epY = y + 0.29682004f * height;
        path.moveTo(epX, epY);

        epX = x + 0.4876493f * width;
        epY = y + 0.29682004f * height;
        path.lineTo(epX, epY);

        epX = x + 0.66043836f * width;
        epY = y + 0.6334776f * height;
        path.lineTo(epX, epY);

        epX = x + 0.27262282f * width;
        epY = y + 0.4705788f * height;
        path.lineTo(epX, epY);

        path.closePath();
    }
}