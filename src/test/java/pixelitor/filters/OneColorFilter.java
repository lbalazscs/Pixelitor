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

package pixelitor.filters;

import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A test filter that always return an image with a single color
 */
public class OneColorFilter extends Filter {
    private final Color color;

    public OneColorFilter(Color color) {
        this.color = color;
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.createImageWithSameColorModel(src);
        Graphics2D g = dest.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.dispose();
        return dest;
    }

    @Override
    public void randomizeSettings() {
    }
}
