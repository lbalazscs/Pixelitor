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
package pixelitor.utils.test;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 */
public class FastMultiplyComposite implements Composite {
    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new FastMultiplyCompositeContext(srcColorModel, dstColorModel, hints);
    }
}

class FastMultiplyCompositeContext implements CompositeContext {
    private final ColorModel srcColorModel;
    private final ColorModel dstColorModel;
    private final RenderingHints hints;

    FastMultiplyCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        this.srcColorModel = srcColorModel;
        this.dstColorModel = dstColorModel;
        this.hints = hints;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
//        int[] srcPixels = new int[src.getWidth() * src.getHeight()];
//        srcPixels = src.getPixels(0, 0, src.getWidth(), src.getHeight(), srcPixels);

        DataBufferInt srcDataBuffer = (DataBufferInt) src.getDataBuffer();
        int[] srcPixels = srcDataBuffer.getData();

        DataBufferInt dstDataBuffer = (DataBufferInt) dstIn.getDataBuffer();
        int[] dstPixels = dstDataBuffer.getData();

        DataBufferInt dstOutDataBuffer = (DataBufferInt) dstOut.getDataBuffer();
        int[] dstOutPixels = dstOutDataBuffer.getData();

        for (int i = 0; i < dstOutPixels.length; i++) {
            int srcPixel = srcPixels[i];

            int srcA = (srcPixel >>> 24) & 0xFF;
            int srcR = (srcPixel >>> 16) & 0xFF;
            int srcG = (srcPixel >>> 8) & 0xFF;
            int srcB = (srcPixel) & 0xFF;

            int dstPixel = dstPixels[i];

//            int dstA = (dstPixel >>> 24) & 0xFF;
            int dstR = (dstPixel >>> 16) & 0xFF;
            int dstG = (dstPixel >>> 8) & 0xFF;
            int dstB = (dstPixel) & 0xFF;

            int outA = srcA;
            int outR = (srcR * dstR) >> 8;
            int outG = (srcG * dstG) >> 8;
            int outB = (srcB * dstB) >> 8;

            dstOutPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }
}