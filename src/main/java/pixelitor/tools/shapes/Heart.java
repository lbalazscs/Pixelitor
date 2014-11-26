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

/**
 * A heart shape
 */
public class Heart extends GeneralShape {
    public Heart(int x, int y, int width, int height) {
        float maxX = x + width;

        float centerX = x + width / 2.0f;
        float bottomY = y + height;

        float cp1XRight = x + 0.58f * width;
        float cp1XLeft = x + 0.42f * width;
        float cp1Y = y + 0.78f * height;

        float cp2Y = y + 0.6f * height;
        float sideY = y + 0.3f * height;
        float cp3Y = y + 0.18f * height;
        float cp4XRight = x + 0.9f * width;
        float cp4XLeft = x + 0.1f * width;

        float cp5XRight = x + 0.6f * width;
        float cp5XLeft = x + 0.4f * width;
        float topCenterY = y + 0.18f * height;

        float topXRight = x + 0.75f * width;
        float topXLeft = x + 0.25f * width;

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
