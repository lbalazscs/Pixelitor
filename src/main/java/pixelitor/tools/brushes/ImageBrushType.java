/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools.brushes;

import pixelitor.tools.AbstractBrushTool;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * The type of an image-based brush.
 */
public enum ImageBrushType {
    REAL {
        @Override
        protected BufferedImage createImpl() {
            return ImageUtils.createRandomPointsTemplateBrush(SIZE, 0.2f);
        }
    }, HAIR {
        @Override
        protected BufferedImage createImpl() {
            return ImageUtils.createRandomPointsTemplateBrush(SIZE, 0.03f);
        }
    }, SOFT {
        @Override
        protected BufferedImage createImpl() {
            return ImageUtils.createSoftBWBrush(SIZE);
        }
    }, GREEK {
        @Override
        protected BufferedImage createImpl() {
            BufferedImage template = ImageUtils.loadBufferedImage("greek.png");
            BufferedImage resizedImage = ImageUtils.resizeImage(SIZE, template);
            return resizedImage;
        }
    }, ARROW {
        @Override
        protected BufferedImage createImpl() {
            BufferedImage template = ImageUtils.loadBufferedImage("arrow.png");
            BufferedImage resizedImage = ImageUtils.resizeImage(SIZE, template);
            return resizedImage;
        }
    };

    private static final int SIZE = 2 * AbstractBrushTool.MAX_BRUSH_RADIUS;
    private boolean used = false;

    protected abstract BufferedImage createImpl();

    /**
     * Creates a brush template that is not colorized yet. Areas that should be transparent in the final
     * brush image are white, and semi-transparent images are gray
     */
    BufferedImage createBWBrushImage() {
        if (used) {
            throw new IllegalStateException(getClass().getName() + " used twice");
        }
        BufferedImage image = createImpl();
        used = true;
        return image;
    }
}
