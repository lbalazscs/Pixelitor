/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.gui.View;
import pixelitor.layers.ContentLayer;

import java.awt.Insets;
import java.awt.Rectangle;
import java.util.StringJoiner;

/**
 * Like {@link Insets}, but outwards. Represents an enlargement.
 */
public class Outsets {
    public int top;
    public int left;
    public int bottom;
    public int right;

    public Outsets(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public static Outsets createZero() {
        return new Outsets(0, 0, 0, 0);
    }

    public boolean isZero() {
        return top == 0 && left == 0 && bottom == 0 && right == 0;
    }

    public void ensureFitsContentOf(ContentLayer contentLayer) {
        Rectangle contentBounds = contentLayer.getContentBounds();
        if (contentBounds == null) {
            // can happen for gradient layers and for uninitialized shape layers
            return;
        }

        Canvas canvas = contentLayer.getComp().getCanvas();

        if (contentBounds.x < -left) {
            left = -contentBounds.x;
        }

        if (contentBounds.y < -top) {
            top = -contentBounds.y;
        }

        int contentMaxX = contentBounds.x + contentBounds.width;
        if (contentMaxX > canvas.getWidth() + right) {
            right = contentMaxX - canvas.getWidth();
        }

        int contentMaxY = contentBounds.y + contentBounds.height;
        if (contentMaxY > canvas.getHeight() + bottom) {
            bottom = contentMaxY - canvas.getHeight();
        }
    }

    public void negate() {
        left = -left;
        right = -right;
        top = -top;
        bottom = -bottom;
    }

    /**
     * Resizes the given canvas to accommodate these outsets.
     */
    public void resizeCanvas(Canvas canvas, View view) {
        int newWidth = canvas.getWidth() + left + right;
        int newHeight = canvas.getHeight() + top + bottom;

        canvas.resize(newWidth, newHeight, view, false);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Outsets.class.getSimpleName() + "[", "]")
            .add("top=" + top)
            .add("left=" + left)
            .add("bottom=" + bottom)
            .add("right=" + right)
            .toString();
    }
}
