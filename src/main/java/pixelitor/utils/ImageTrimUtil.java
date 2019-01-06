/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


/**
 * Trim image from transparent pixels
 * See https://stackoverflow.com/questions/3224561/crop-image-to-smallest-size-by-removing-transparent-pixels-in-java
 *
 * There could be two possible needs:
 * 1. Full trim: only pixels that are in format argb (0,0,0,0)
 * 2. Trim transparent pixels: only pixels with alpha = 0
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ImageTrimUtil {
    private ImageTrimUtil() {
    }

    /**
     * Returns image bounding box trimmed from transparent pixels (alpha channel = 0)
     */
    public static Rectangle getTrimRect(BufferedImage image) {
        WritableRaster raster = image.getAlphaRaster();
        int width = raster.getWidth();
        int height = raster.getHeight();
        int left = 0;
        int top = 0;
        int right = width - 1;
        int bottom = height - 1;
        int minRight = width - 1;
        int minBottom = height - 1;

        top:
        for (; top < bottom; top++) {
            for (int x = 0; x < width; x++) {
                if (raster.getSample(x, top, 0) != 0) {
                    minRight = x;
                    minBottom = top;
                    break top;
                }
            }
        }

        left:
        for (; left < minRight; left++) {
            for (int y = height - 1; y > top; y--) {
                if (raster.getSample(left, y, 0) != 0) {
                    minBottom = y;
                    break left;
                }
            }
        }

        bottom:
        for (; bottom > minBottom; bottom--) {
            for (int x = width - 1; x >= left; x--) {
                if (raster.getSample(x, bottom, 0) != 0) {
                    minRight = x;
                    break bottom;
                }
            }
        }

        right:
        for (; right > minRight; right--) {
            for (int y = bottom; y >= top; y--) {
                if (raster.getSample(right, y, 0) != 0) {
                    break right;
                }
            }
        }

        return new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    public static BufferedImage trimImage(BufferedImage image) {
        Rectangle rect = getTrimRect(image);
        return image.getSubimage(rect.x, rect.y, rect.width, rect.height);
    }
}
