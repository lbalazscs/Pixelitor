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

package pixelitor.filters.lookup;

import com.jhlabs.image.PixelUtils;
import pixelitor.utils.ImageUtils;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;

/**
 * Performs 4-5 times faster than java.awt.image.LookupOp
 */
public class FastLookupOp implements BufferedImageOp {
    private final ShortLookupTable lookupTable;

    public FastLookupOp(ShortLookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        boolean packedInt = ImageUtils.hasPackedIntArray(src);
        if (packedInt) {
            boolean simple = !src.isAlphaPremultiplied();

            DataBufferInt srcDataBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            int[] srcData = srcDataBuffer.getData();

            DataBufferInt destDataBuffer = (DataBufferInt) dst.getRaster().getDataBuffer();
            int[] destData = destDataBuffer.getData();

            int length = srcData.length;
            assert length == destData.length;

            short[][] table = lookupTable.getTable();

            for (int i = 0; i < length; i++) {
                int rgb = srcData[i];
                int a = (rgb >>> 24) & 0xFF;
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                if (a == 255 || simple) {
                    r = table[0][r];
                    g = table[1][g];
                    b = table[2][b];
                } else if (a == 0) {
                    r = 0;
                    g = 0;
                    b = 0;
                } else {
                    // unpremultiply
                    float f = 255.0f / a;
                    int ur = (int) (r * f);
                    int ug = (int) (g * f);
                    int ub = (int) (b * f);

                    // TODO these checks shouldn't be necessary
                    if (ur > 255) {
                        ur = 255;
                    }
                    if (ug > 255) {
                        ug = 255;
                    }
                    if (ub > 255) {
                        ub = 255;
                    }

                    // lookup
                    ur = table[0][ur];
                    ug = table[1][ug];
                    ub = table[2][ub];

                    // premultiply
                    float f2 = a * (1.0f / 255.0f);
                    r = (int) (ur * f2);
                    g = (int) (ug * f2);
                    b = (int) (ub * f2);

                    r = PixelUtils.clamp(r);
                    g = PixelUtils.clamp(g);
                    b = PixelUtils.clamp(b);
                }
                destData[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }

        } else { // fall back to a normal LookupOp
            BufferedImageOp lookupOp = new LookupOp(lookupTable, null);
            lookupOp.filter(src, dst);
        }

        return dst;
    }


    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return null;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return null;
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return null;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}
