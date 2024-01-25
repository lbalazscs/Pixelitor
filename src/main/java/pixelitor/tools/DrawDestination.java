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
 * {@link AbstractBrushTool} subclasses either draw into
 * a temporary layer (so that the tool can have blending
 * mode and opacity) or directly into the layer image
 */
public enum DrawDestination {
    TMP_LAYER {
        @Override
        public Graphics2D createGraphics(Drawable dr, Composite composite) {
            return dr.createTmpLayer(composite, false).getGraphics();
        }

        @Override
        public void prepareBrushStroke(Drawable dr) {
            // nothing to be done
        }

        @Override
        public void finishBrushStroke(Drawable dr) {
            dr.mergeTmpDrawingLayerDown();
        }

        @Override
        public BufferedImage getOriginalImage(Drawable dr, AbstractBrushTool tool) {
            // it can simply return the drawable image because
            // the drawing was on the temporary layer
            return dr.getImage();
        }
    }, DIRECT {
        private BufferedImage backupImg;

        @Override
        public Graphics2D createGraphics(Drawable dr, Composite composite) {
            // ignores the composite!
            BufferedImage drawImage = dr.getCanvasSizedSubImage();
            return drawImage.createGraphics();
        }

        @Override
        public void prepareBrushStroke(Drawable dr) {
            BufferedImage image = dr.getImage();

            assert Assertions.rasterStartsAtZero(image);

            backupImg = ImageUtils.copyImage(image);
        }

        @Override
        public void finishBrushStroke(Drawable dr) {
            backupImg.flush();
            backupImg = null;
        }

        @Override
        public BufferedImage getOriginalImage(Drawable dr, AbstractBrushTool tool) {
            if (backupImg == null) {
                throw new IllegalStateException("copyBeforeStart is null for " + tool.getName());
            }

            return backupImg;
        }
    };

    public abstract Graphics2D createGraphics(Drawable dr, Composite composite);

    public abstract void prepareBrushStroke(Drawable dr);

    public abstract void finishBrushStroke(Drawable dr);

    /**
     * Returns the original (untouched) image for undo
     */
    public abstract BufferedImage getOriginalImage(Drawable dr, AbstractBrushTool tool);
}
