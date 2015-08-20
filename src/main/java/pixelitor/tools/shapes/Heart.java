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
package pixelitor.tools.shapes;

/**
 * A heart shape
 */
public class Heart extends GeneralShape {
    public Heart(double x, double y, double width, double height) {
        double maxX = x + width;

        double centerX = x + width / 2.0f;
        double bottomY = y + height;

        double cp1XRight = x + 0.58f * width;
        double cp1XLeft = x + 0.42f * width;
        double cp1Y = y + 0.78f * height;

        double cp2Y = y + 0.6f * height;
        double sideY = y + 0.3f * height;
        double cp3Y = y + 0.18f * height;
        double cp4XRight = x + 0.9f * width;
        double cp4XLeft = x + 0.1f * width;

        double cp5XRight = x + 0.6f * width;
        double cp5XLeft = x + 0.4f * width;
        double topCenterY = y + 0.18f * height;

        double topXRight = x + 0.75f * width;
        double topXLeft = x + 0.25f * width;

        path.moveTo(centerX, bottomY);
        // right side
        path.curveTo(cp1XRight, cp1Y, // control point 1
                maxX, cp2Y,    // control point 2
                maxX, sideY); // side point
        path.curveTo(maxX, cp3Y, // control point 3
                cp4XRight, y, // control point 4
                topXRight, y); // top point
        path.curveTo(cp5XRight, y, // control point 5
                centerX, topCenterY,  // this control point is the same as the following endpoint
                centerX, topCenterY); // top center point
        // left side
        path.curveTo(centerX, topCenterY,   // this control point is the same as the start point
                cp5XLeft, y, // left mirror of control point 5
                topXLeft, y);
        path.curveTo(cp4XLeft, y,
                x, cp3Y,
                x, sideY);
        path.curveTo(x, cp2Y,
                cp1XLeft, cp1Y,
                centerX, bottomY);
    }
}
