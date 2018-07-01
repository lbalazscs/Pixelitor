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

package pixelitor.tools.util;

import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.PartialImageEdit;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Represents the area affected by a tool
 * in image space, relative to the image.
 */
public class ToolAffectedArea {
    private final String toolName;
    private Rectangle rect;
    private final Drawable dr;
    private final BufferedImage originalImage;

    public ToolAffectedArea(Rectangle rect, BufferedImage originalImage, Drawable dr,
                            boolean relativeToImage, String toolName) {
        assert rect.width > 0 : "rectangle.width = " + rect.width;
        assert rect.height > 0 : "rectangle.height = " + rect.height;

        this.toolName = toolName;
        this.dr = dr;
        this.rect = rect;
        this.originalImage = originalImage;

        if (!relativeToImage) {
            // if the coordinates are relative to the canvas,
            // translate them to be relative to the image
            int dx = -dr.getTX();
            int dy = -dr.getTY();
            this.rect.translate(dx, dy);
        }

        this.rect = SwingUtilities.computeIntersection(
                0, 0, originalImage.getWidth(), originalImage.getHeight(), // full image bounds
                this.rect
        );

    }

    public Drawable getDrawable() {
        return dr;
    }

    /**
     * Save only the affected area for undo.
     */
    public void addToHistory() {
        assert (originalImage != null);
        if (rect.isEmpty()) {
            return;
        }

        Composition comp = dr.getComp();

        // we could intersect with the selection bounds,
        // but typically the extra savings would be minimal

        PartialImageEdit edit = new PartialImageEdit(toolName, comp, dr, originalImage, rect, false);
        History.addEdit(edit);
    }
}
