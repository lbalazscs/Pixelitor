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

package pixelitor.filters.lookup;

import pixelitor.filters.util.FilterPalette;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Objects;

/**
 * A {@link BufferedImageOp} that performs 4–5 times faster than
 * {@link LookupOp} for images with packed integer pixel data.
 */
public class FastLookupOp implements BufferedImageOp {
    private final ShortLookupTable lut;

    public FastLookupOp(ShortLookupTable lut) {
        this.lut = lut;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        Objects.requireNonNull(src, "src cannot be null");

        if (dst != null && (src.getWidth() != dst.getWidth() || src.getHeight() != dst.getHeight())) {
            throw new IllegalArgumentException(
                "src and dst must have the same dimensions: src is " +
                    src.getWidth() + "x" + src.getHeight() + ", dst is " +
                    dst.getWidth() + "x" + dst.getHeight());
        }

        if (ImageUtils.hasPackedIntArray(src)) {
            if (dst == null || (ImageUtils.hasPackedIntArray(dst) && src.getType() == dst.getType())) {
                return filterPackedIntImg(src, dst);
            }
        } else if (src.getColorModel() instanceof IndexColorModel) {
            return filterIndexed(src, dst);
        }

        // for all other image types (or incompatible dst), fall back to the standard LookupOp
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        BufferedImageOp lookupOp = new LookupOp(lut, null);
        return lookupOp.filter(src, dst);
    }

    private BufferedImage filterPackedIntImg(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        boolean premultiplied = src.isAlphaPremultiplied();

        int[] srcPixels = ((DataBufferInt) src.getRaster()
            .getDataBuffer()).getData();

        int[] destPixels = ((DataBufferInt) dst.getRaster()
            .getDataBuffer()).getData();

        int numPixels = srcPixels.length;
        assert numPixels == destPixels.length;

        short[][] table = lut.getTable();
        short[] rTable = table[0];
        short[] gTable = table.length > 1 ? table[1] : table[0];
        short[] bTable = table.length > 2 ? table[2] : table[0];

        for (int i = 0; i < numPixels; i++) {
            int rgb = srcPixels[i];
            int a = rgb >>> 24;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            if (a == 255 || !premultiplied) {
                r = rTable[r];
                g = gTable[g];
                b = bTable[b];
            } else if (a == 0) {
                r = 0;
                g = 0;
                b = 0;
            } else {
                // unpremultiply
                float f = 255.0f / a;
                int ur = (int) (r * f + 0.5f);
                int ug = (int) (g * f + 0.5f);
                int ub = (int) (b * f + 0.5f);

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
                ur = rTable[ur];
                ug = gTable[ug];
                ub = bTable[ub];

                // premultiply
                float f2 = a * (1.0f / 255.0f);
                r = (int) (ur * f2 + 0.5f);
                g = (int) (ug * f2 + 0.5f);
                b = (int) (ub * f2 + 0.5f);
            }
            destPixels[i] = a << 24 | r << 16 | g << 8 | b;
        }
        return dst;
    }

    private BufferedImage filterIndexed(BufferedImage src, BufferedImage dst) {
        short[][] table = lut.getTable();
        short[] rTable = table[0];
        short[] gTable = table.length > 1 ? table[1] : table[0];
        short[] bTable = table.length > 2 ? table[2] : table[0];

        BufferedImage filtered = new FilterPalette(src) {
            @Override
            protected int changeRed(int r) {
                return rTable[r];
            }

            @Override
            protected int changeGreen(int g) {
                return gTable[g];
            }

            @Override
            protected int changeBlue(int b) {
                return bTable[b];
            }
        }.filter();

        if (dst == null) {
            return filtered;
        }

        Graphics2D g = dst.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(filtered, 0, 0, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return src.getRaster().getBounds();
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        if (destCM == null) {
            return ImageUtils.createImageWithSameCM(src);
        }
        WritableRaster wr = destCM.createCompatibleWritableRaster(
            src.getWidth(), src.getHeight());
        return new BufferedImage(destCM, wr, destCM.isAlphaPremultiplied(), null);
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = (Point2D) srcPt.clone();
        } else {
            dstPt.setLocation(srcPt.getX(), srcPt.getY());
        }
        return dstPt;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    public ShortLookupTable getLookupTable() {
        return lut;
    }
}
