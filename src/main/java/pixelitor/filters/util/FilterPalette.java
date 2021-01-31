/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.util;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

/**
 * Filters only the palette of an image with indexed color
 */
public abstract class FilterPalette {
    private final IndexColorModel srcCM;
    private final BufferedImage src;

    protected FilterPalette(BufferedImage src) {
        this.src = src;
        this.srcCM = (IndexColorModel) src.getColorModel();
    }

    public BufferedImage filter() {
        int arrayLength = srcCM.getMapSize();
        byte[] reds = new byte[arrayLength];
        byte[] greens = new byte[arrayLength];
        byte[] blues = new byte[arrayLength];
        srcCM.getReds(reds);
        srcCM.getGreens(greens);
        srcCM.getBlues(blues);

        for (int i = 0; i < arrayLength; i++) {
            int r = Byte.toUnsignedInt(reds[i]);
            r = changeRed(r);
            reds[i] = (byte) r;
        }
        for (int i = 0; i < arrayLength; i++) {
            int g = Byte.toUnsignedInt(greens[i]);
            g = changeGreen(g);
            greens[i] = (byte) g;
        }
        for (int i = 0; i < arrayLength; i++) {
            int b = Byte.toUnsignedInt(blues[i]);
            b = changeBlue(b);
            blues[i] = (byte) b;
        }

        var dstCM = new IndexColorModel(8, arrayLength, reds, greens, blues,
            srcCM.getTransparentPixel());
        return new BufferedImage(dstCM, src.getRaster(),
            srcCM.isAlphaPremultiplied(), null);
    }

    protected abstract int changeRed(int r);

    protected abstract int changeGreen(int g);

    protected abstract int changeBlue(int b);
}
