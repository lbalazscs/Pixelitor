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

package pixelitor.utils;

import com.jhlabs.composite.RGBComposite;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * A custom Composite that performs a 3-way linear interpolation between
 * a source image, a destination image, and an explicitly given mask.
 * The alpha channel of the mask image determines the blend ratio.
 */
public class MaskedReplaceComposite extends RGBComposite {
    private final BufferedImage maskImage;
    private final int maskX;
    private final int maskY;

    public MaskedReplaceComposite(BufferedImage maskImage, int maskX, int maskY) {
        this(maskImage, maskX, maskY, 1.0f);
    }

    public MaskedReplaceComposite(BufferedImage maskImage, int maskX, int maskY, float alpha) {
        super(alpha);

        this.maskImage = maskImage;
        this.maskX = maskX;
        this.maskY = maskY;
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new Context(extraAlpha, srcColorModel, dstColorModel, maskImage, maskX, maskY);
    }

    static class Context extends RGBCompositeContext {
        private final float globalAlpha;
        private final BufferedImage maskImage;
        private final int maskX;
        private final int maskY;

        public Context(float globalAlpha, ColorModel srcColorModel, ColorModel dstColorModel, BufferedImage maskImage, int maskX, int maskY) {
            super(globalAlpha, srcColorModel, dstColorModel);
            this.globalAlpha = globalAlpha;
            this.maskImage = maskImage;
            this.maskX = maskX;
            this.maskY = maskY;
        }

        @Override
        public void composeRGB(int[] src, int[] dst, float alpha) {
            // not used because we override the main compose method
            // in order to have access to the x and y coordinates
        }

        @Override
        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            int x = dstOut.getMinX();
            int w = dstOut.getWidth();
            int y0 = dstOut.getMinY();
            int y1 = y0 + dstOut.getHeight();

            // buffers for the un-packed pixel data (R, G, B, A)
            int[] srcPix = new int[w * 4];
            int[] dstPix = new int[w * 4];

            int maskWidth = maskImage.getWidth();
            int maskHeight = maskImage.getHeight();
            int[] maskRow = new int[maskWidth];

            for (int y = y0; y < y1; y++) {
                srcPix = src.getPixels(x, y, w, 1, srcPix);
                dstPix = dstIn.getPixels(x, y, w, 1, dstPix);

                int my = y - maskY;
                boolean yInMask = (my >= 0 && my < maskHeight);

                // fetch a row from the mask image
                if (yInMask) {
                    maskImage.getRGB(0, my, maskWidth, 1, maskRow, 0, maskWidth);
                }

                for (int i = 0; i < w; i++) {
                    int mAlpha = 0;
                    if (yInMask) {
                        int mx = x + i - maskX;
                        if (mx >= 0 && mx < maskWidth) {
                            // extract the alpha channel of the mask image
                            mAlpha = (maskRow[mx] >>> 24);
                        }
                    }

                    // the interpolation factor
                    float maskF = (mAlpha / 255.0f) * globalAlpha;
                    float invMaskF = 1.0f - maskF;

                    int idx = i * 4;

                    int sr = srcPix[idx];
                    int sg = srcPix[idx + 1];
                    int sb = srcPix[idx + 2];
                    int sa = srcPix[idx + 3];

                    int dir = dstPix[idx];
                    int dig = dstPix[idx + 1];
                    int dib = dstPix[idx + 2];
                    int dia = dstPix[idx + 3];

                    // 3-way lerping: result = replacement * mask + original * (1 - mask)
                    dstPix[idx] = (int) (sr * maskF + dir * invMaskF + 0.5f);
                    dstPix[idx + 1] = (int) (sg * maskF + dig * invMaskF + 0.5f);
                    dstPix[idx + 2] = (int) (sb * maskF + dib * invMaskF + 0.5f);
                    dstPix[idx + 3] = (int) (sa * maskF + dia * invMaskF + 0.5f);
                }

                dstOut.setPixels(x, y, w, 1, dstPix);
            }
        }
    }
}
