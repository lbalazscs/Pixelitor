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

import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * The type of an image-based brush.
 */
public enum ImageBrushType {
    REAL {
        @Override
        BufferedImage createTemplateBrush(int maxRadius) {
            return ImageUtils.createRandomPointsTemplateBrush(2 * maxRadius, 0.2f);
        }
    }, HAIR {
        @Override
        BufferedImage createTemplateBrush(int maxRadius) {
            return ImageUtils.createRandomPointsTemplateBrush(2 * maxRadius, 0.03f);
        }
    }, SOFT {
        @Override
        BufferedImage createTemplateBrush(int maxRadius) {
            return ImageUtils.createSoftTemplateBrush(2 * maxRadius);
        }
    }, GREEK {
        @Override
        BufferedImage createTemplateBrush(int maxRadius) {
            int size = 2 * maxRadius;
            BufferedImage template = ImageUtils.loadBufferedImage("greek.png");
            BufferedImage resizedImage = ImageUtils.resizeImage(size, template);
            return resizedImage;
        }
    }, ARROW {
        @Override
        BufferedImage createTemplateBrush(int maxRadius) {
            int size = 2 * maxRadius;
            BufferedImage template = ImageUtils.loadBufferedImage("arrow.png");
            BufferedImage resizedImage = ImageUtils.resizeImage(size, template);
            return resizedImage;
        }

    };

    /**
     * Creates a brush template that is not colorized yet. Areas that should be transparent in the final
     * brush image are white, and semi-transparent images are gray
     */
    abstract BufferedImage createTemplateBrush(int maxRadius);
}
