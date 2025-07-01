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

package pixelitor.filters.lookup;

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.util.FilterPalette;
import pixelitor.utils.ImageUtils;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * Performs 4-5 times faster than {@link LookupOp} if
 * the image has packed ints
 */
public class FastLookupOp implements BufferedImageOp {
    private final ShortLookupTable lut;

    public FastLookupOp(ShortLookupTable lut) {
        this.lut = lut;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (ImageUtils.hasPackedIntArray(src)) {
            return filterIntPacked(src, dst);
        }

        if (src.getColorModel() instanceof IndexColorModel) {
            return filterIndexed(src);
        }

        // for all other image types, fall back to the standard LookupOp
        dst = ImageUtils.createImageWithSameCM(src);
        BufferedImageOp lookupOp = new LookupOp(lut, null);
        lookupOp.filter(src, dst);
        return dst;
    }

    private BufferedImage filterIntPacked(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = ImageUtils.createImageWithSameCM(src);
        }
        boolean notPremultiplied = !src.isAlphaPremultiplied();

        int[] srcPixels = ((DataBufferInt) src.getRaster()
            .getDataBuffer()).getData();

        int[] destPixels = ((DataBufferInt) dst.getRaster()
            .getDataBuffer()).getData();

        int numPixels = srcPixels.length;
        assert numPixels == destPixels.length;

        short[][] table = lut.getTable();

        for (int i = 0; i < numPixels; i++) {
            int rgb = srcPixels[i];
            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            if (a == 255 || notPremultiplied) {
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
            destPixels[i] = a << 24 | r << 16 | g << 8 | b;
        }
        return dst;
    }

    private BufferedImage filterIndexed(BufferedImage src) {
        short[][] table = lut.getTable();
        return new FilterPalette(src) {
            @Override
            protected int changeRed(int r) {
                return table[0][r];
            }

            @Override
            protected int changeGreen(int g) {
                return table[1][g];
            }

            @Override
            protected int changeBlue(int b) {
                return table[2][b];
            }
        }.filter();
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
