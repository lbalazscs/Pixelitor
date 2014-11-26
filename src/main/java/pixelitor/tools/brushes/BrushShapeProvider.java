/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
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
package pixelitor.tools.brushes;

import pixelitor.tools.shapes.Heart;

import java.awt.Shape;

/**
 * A shape factory for AngledShapeBrush
 */
public enum BrushShapeProvider {
    HEART {
        @Override
        Shape getShape(int x, int y, int width, int height) {
            return new Heart(x, y, width, height);
        }
    };

    abstract Shape getShape(int x, int y, int width, int height);
}
