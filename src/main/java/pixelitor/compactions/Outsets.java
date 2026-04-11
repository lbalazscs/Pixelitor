/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Insets;
import java.awt.geom.AffineTransform;

/**
 * Like {@link Insets}, but outwards. Represents an enlargement.
 */
public record Outsets(int top, int right, int bottom, int left) {

    public AffineTransform getTopLeftTranslation() {
        return AffineTransform.getTranslateInstance(left, top);
    }

    /**
     * Resizes the given canvas to accommodate these outsets.
     */
    public void resizeCanvas(Canvas canvas, View view) {
        int newWidth = canvas.getWidth() + left + right;
        int newHeight = canvas.getHeight() + top + bottom;

        canvas.resize(newWidth, newHeight, view, false);
    }

    public static Outsets createZero() {
        return new Outsets(0, 0, 0, 0);
    }

    public boolean isZero() {
        return top == 0 && left == 0 && bottom == 0 && right == 0;
    }
}
