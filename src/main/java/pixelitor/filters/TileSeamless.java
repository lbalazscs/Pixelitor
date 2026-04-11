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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.OffsetFilter;
import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.utils.ImageUtils;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class TileSeamless extends ParametrizedFilter {
    public static final String NAME = "Tile Seamless";

    private final ImagePositionParam center = new ImagePositionParam("Center");

    public TileSeamless() {
        super(true);
        help = Help.fromHTML("Modifies an image to make it \"seamless\", meaning it<br>can be tiled (repeated) without visible edges or discontinuities.");
        initParams(center);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (dest == null) {
            dest = ImageUtils.createImageWithSameCM(src);
        }

        Point2D p = center.getAbsolutePoint(src);
        double cx = p.getX();
        double cy = p.getY();
        int width = src.getWidth();
        int height = src.getHeight();

        // generate the offset image into the destination buffer
        OffsetFilter offsetFilter = new OffsetFilter(NAME, TransformFilter.WRAP_AROUND, p);
        offsetFilter.filter(src, dest);

        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        int i = 0;
        for (int y = 0; y < height; y++) {
            // y distance for the entire row
            double distY = (y > cy) ? (y - cy) / (height - cy) : (cy - y) / cy;

            for (int x = 0; x < width; x++) {
                double distX = (x > cx) ? (x - cx) / (width - cx) : (cx - x) / cx;

                double mul = distX * distY;
                double denominator = mul + (1.0 - distX) * (1.0 - distY);
                double f = denominator == 0 ? 0 : mul / denominator;

                // blend original src pixel with the offset pixel
                destPixels[i] = ImageMath.mixColors((float) f, srcPixels[i], destPixels[i]);
                i++;
            }
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
