/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public enum DrawStrategy {
    TMP_LAYER {
        @Override
        public Graphics2D createDrawGraphics(ImageLayer layer, Composite composite) {
            return layer.createTmpDrawingLayer(composite).getGraphics();
        }

        @Override
        public void prepareBrushStroke(ImageLayer layer) {

        }

        @Override
        public void finishBrushStroke(ImageLayer layer) {
            layer.mergeTmpDrawingLayerDown();
        }

        @Override
        public BufferedImage getOriginalImage(ImageLayer layer, AbstractBrushTool tool) {
            // it can simply return the layer image because
            // the drawing was on the temporary layer
            return layer.getImage();
        }
    }, DIRECT {
        private BufferedImage copyBeforeStart;

        @Override
        public Graphics2D createDrawGraphics(ImageLayer layer, Composite composite) {
            BufferedImage drawImage = layer.getCanvasSizedSubImage();
            Graphics2D g = drawImage.createGraphics();
            return g;
        }

        @Override
        public void prepareBrushStroke(ImageLayer layer) {
            BufferedImage image = layer.getImage();

            assert Utils.checkRasterMinimum(image);

            copyBeforeStart = ImageUtils.copyImage(image);
        }

        @Override
        public void finishBrushStroke(ImageLayer layer) {
            copyBeforeStart.flush();
            copyBeforeStart = null;
        }

        @Override
        public BufferedImage getOriginalImage(ImageLayer layer, AbstractBrushTool tool) {
            if (copyBeforeStart == null) {
                throw new IllegalStateException("copyBeforeStart is null for " + tool.getName());
            }

            return copyBeforeStart;
        }
    };

    public abstract Graphics2D createDrawGraphics(ImageLayer layer, Composite composite);

    public abstract void prepareBrushStroke(ImageLayer layer);

    public abstract void finishBrushStroke(ImageLayer layer);

    /**
     * Returns the original (untouched) image for undo
     */
    public abstract BufferedImage getOriginalImage(ImageLayer layer, AbstractBrushTool tool);
}
