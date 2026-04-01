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

package pixelitor.tools;

import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.test.Assertions;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Defines how {@link AbstractBrushTool} subclasses draw: either to a
 * temporary layer to support blending modes and opacity or directly
 * to the layer's image.
 */
public enum DrawTarget {
    /**
     * Renders brush strokes to a temporary layer that is later merged down.
     */
    TMP_LAYER {
        @Override
        public Graphics2D createGraphics(Drawable dr, Composite composite) {
            return dr.createTmpLayer(composite, false).getGraphics();
        }

        @Override
        public BufferedImage prepareForBrushStroke(Drawable dr) {
            // it can simply return the drawable's image because
            // the drawing was done on a temporary layer
            return dr.getImage();
        }

        @Override
        public void finishBrushStroke(Drawable dr, BufferedImage originalImg) {
            dr.mergeTmpDrawingLayerDown();
        }
    },
    /**
     * Renders brush strokes directly to the target layer. This is
     * better for performance, but doesn't support blending modes or
     * opacity for the brush strokes.
     */
    DIRECT {
        @Override
        public Graphics2D createGraphics(Drawable dr, Composite composite) {
            // ignores the composite!
            return dr.getCanvasSizedSubImage().createGraphics();
        }

        @Override
        public BufferedImage prepareForBrushStroke(Drawable dr) {
            BufferedImage image = dr.getImage();
            assert Assertions.rasterStartsAtOrigin(image);

            return ImageUtils.copyImage(image);
        }

        @Override
        public void finishBrushStroke(Drawable dr, BufferedImage backupImg) {
            if (backupImg != null) {
                backupImg.flush();
            }
        }
    };

    public abstract Graphics2D createGraphics(Drawable dr, Composite composite);

    /**
     * Returns the backup/original image for undo support.
     */
    public abstract BufferedImage prepareForBrushStroke(Drawable dr);

    /**
     * Flushes/merges state to finalize the drawing.
     */
    public abstract void finishBrushStroke(Drawable dr, BufferedImage originalImg);
}
