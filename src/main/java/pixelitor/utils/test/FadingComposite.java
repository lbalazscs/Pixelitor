/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
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
public class FadingComposite implements Composite {
    private final Composite realComposite;
    private final float fadeFactor;

    public FadingComposite(Composite realComposite, float fadeFactor) {
        this.realComposite = realComposite;
        this.fadeFactor = fadeFactor;
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new FadingCompositeContext(realComposite, srcColorModel, dstColorModel, hints, fadeFactor);
    }
}

class FadingCompositeContext implements CompositeContext {
    private final Composite realComposite;
    private final ColorModel srcColorModel;
    private final ColorModel dstColorModel;
    private final RenderingHints hints;
    private final float fadeFactor;

    FadingCompositeContext(Composite realComposite, ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints, float fadeFactor) {
        this.realComposite = realComposite;
        this.srcColorModel = srcColorModel;
        this.dstColorModel = dstColorModel;
        this.hints = hints;
        this.fadeFactor = fadeFactor;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
        realComposite.createContext(srcColorModel, dstColorModel, hints).compose(src, dstIn, dstOut);
        // now dstOut is filled with the result of the real composite
        // we still have to fade between the original and this

        DataBufferInt srcDataBuffer = (DataBufferInt) src.getDataBuffer();
        int[] originalPixels = srcDataBuffer.getData();

        DataBufferInt dstOutDataBuffer = (DataBufferInt) dstOut.getDataBuffer();
        int[] blendedPixels = dstOutDataBuffer.getData();

        for (int i = 0; i < blendedPixels.length; i++) {
            int originalPixel = originalPixels[i];

            int origA = (originalPixel >>> 24) & 0xFF;
            int origR = (originalPixel >>> 16) & 0xFF;
            int origG = (originalPixel >>> 8) & 0xFF;
            int origB = (originalPixel) & 0xFF;

            int blendedPixel = blendedPixels[i];

            int blendedA = (blendedPixel >>> 24) & 0xFF;
            int blendedR = (blendedPixel >>> 16) & 0xFF;
            int blendedG = (blendedPixel >>> 8) & 0xFF;
            int blendedB = (blendedPixel) & 0xFF;

            int outA = (int) (origA + fadeFactor * (blendedA - origA));
            int outR = (int) (origR + fadeFactor * (blendedR - origR));
            int outG = (int) (origG + fadeFactor * (blendedG - origG));
            int outB = (int) (origB + fadeFactor * (blendedB - origB));

            blendedPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }
}