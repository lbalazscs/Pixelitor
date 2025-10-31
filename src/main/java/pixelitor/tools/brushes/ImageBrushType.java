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
        public BufferedImage createTemplateImage() {
            return ImageUtils.createGrayRandomPointsBrushImage(SIZE, 0.2f);
        }
    }, HAIR {
        @Override
        public BufferedImage createTemplateImage() {
            return ImageUtils.createGrayRandomPointsBrushImage(SIZE, 0.03f);
        }
    }, SOFT {
        @Override
        public BufferedImage createTemplateImage() {
            return ImageUtils.createSoftGrayBrushImage(SIZE);
        }
    };

    private static final int SIZE = 2 * AbstractBrushTool.MAX_BRUSH_RADIUS;

    /**
     * Creates a brush template that is not colorized yet. Areas that should be transparent in the final
     * brush image are white, and semi-transparent images are gray
     */
    public abstract BufferedImage createTemplateImage();
}
