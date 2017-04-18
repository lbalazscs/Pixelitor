/*
 * Copyright 2017 Laszlo Balazs-Csiki
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

import pixelitor.layers.Drawable;

import java.awt.Rectangle;

/**
 * Represents the area affected by a tool. It can be relative to the image or
 * relative to the canvas
 */
public class ToolAffectedArea {
    private final Rectangle rectangle;
    private final Drawable dr;

    public ToolAffectedArea(Drawable dr, Rectangle rectangle, boolean relativeToImage) {
        assert rectangle.width > 0 : "rectangle.width = " + rectangle.width;
        assert rectangle.height > 0 : "rectangle.height = " + rectangle.height;

        this.dr = dr;
        this.rectangle = rectangle;

        if (!relativeToImage) {
            int dx = -dr.getTX();
            int dy = -dr.getTY();
            this.rectangle.translate(dx, dy);
        }
    }

    /**
     * @return The affected rectangle relative to the image
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    public Drawable getDrawable() {
        return dr;
    }
}
