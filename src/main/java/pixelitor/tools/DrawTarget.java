/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
        public void prepareForBrushStroke(Drawable dr) {
            // no preparation needed
        }

        @Override
        public void finalizeBrushStroke(Drawable dr) {
            dr.mergeTmpDrawingLayerDown();
        }

        @Override
        public BufferedImage getOriginalImage(Drawable dr, AbstractBrushTool tool) {
            // it can simply return the drawable image because
            // the drawing was on the temporary layer
            return dr.getImage();
        }
    },
    /**
     * Renders brush strokes directly to the target layer. This is
     * better for performance, but doesn't support blending modes or
     * opacity for the brush strokes.
     */
    DIRECT {
        private BufferedImage backupImg;

        @Override
        public Graphics2D createGraphics(Drawable dr, Composite composite) {
            // ignores the composite!
            return dr.getCanvasSizedSubImage().createGraphics();
        }

        @Override
        public void prepareForBrushStroke(Drawable dr) {
            BufferedImage image = dr.getImage();

            assert Assertions.rasterStartsAtOrigin(image);

            // store the original image for undo support
            backupImg = ImageUtils.copyImage(image);
        }

        @Override
        public void finalizeBrushStroke(Drawable dr) {
            backupImg.flush();
            backupImg = null;
        }

        @Override
        public BufferedImage getOriginalImage(Drawable dr, AbstractBrushTool tool) {
            if (backupImg == null) {
                throw new IllegalStateException("no backup image in " + tool.getName());
            }

            return backupImg;
        }
    };

    public abstract Graphics2D createGraphics(Drawable dr, Composite composite);

    public abstract void prepareForBrushStroke(Drawable dr);

    public abstract void finalizeBrushStroke(Drawable dr);

    /**
     * Returns the original (unchanged) image for undo support.
     */
    public abstract BufferedImage getOriginalImage(Drawable dr, AbstractBrushTool tool);
}
