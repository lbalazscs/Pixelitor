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

package pixelitor.tools;

import pixelitor.layers.Drawable;

import java.awt.Rectangle;

/**
 * Represents the area affected by a tool.
 * It can be relative to the image or to the canvas
 */
public class ToolAffectedArea {
    private final Rectangle rect;
    private final Drawable dr;

    public ToolAffectedArea(Drawable dr, Rectangle rect, boolean relativeToImage) {
        assert rect.width > 0 : "rectangle.width = " + rect.width;
        assert rect.height > 0 : "rectangle.height = " + rect.height;

        this.dr = dr;
        this.rect = rect;

        if (!relativeToImage) {
            int dx = -dr.getTX();
            int dy = -dr.getTY();
            this.rect.translate(dx, dy);
        }
    }

    /**
     * @return The affected rectangle relative to the image
     */
    public Rectangle getRect() {
        return rect;
    }

    public Drawable getDrawable() {
        return dr;
    }
}
