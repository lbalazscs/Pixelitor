package com.jhlabs.composite;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

/**
 * This special composite solves the problem described at http://javagraphics.blogspot.hu/2008/06/crossfades-what-is-and-isnt-possible.html
 * TODO it is not working with all image types.
 */
public class CrossFadeComposite extends RGBComposite {
    public CrossFadeComposite(float alpha) {
        super(alpha);
    }

    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new Context(extraAlpha, srcColorModel, dstColorModel);
    }

    static class Context extends RGBCompositeContext {
        public Context(float alpha, ColorModel srcColorModel, ColorModel dstColorModel) {
            super(alpha, srcColorModel, dstColorModel);
        }

        public void composeRGB(int[] src, int[] dst, float alpha) {
            int w = src.length;

            for (int i = 0; i < w; i += 4) {
                int sr = src[i];
                int sg = src[i + 1];
                int sb = src[i + 2];
                int sa = src[i + 3];

                float a = alpha * sa / 255f;
                float ac = 1 - a;

                dst[i] = (int) (a * sr);
                dst[i + 1] = (int) (a * sg);
                dst[i + 2] = (int) (a * sb);
                dst[i + 3] = (int) (sa * alpha);
            }
        }
    }

}
