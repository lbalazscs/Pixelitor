/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.composite;

import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

public final class OverlayComposite extends RGBComposite {

    public OverlayComposite(float alpha) {
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
                int dir = dst[i];
                int sg = src[i + 1];
                int dig = dst[i + 1];
                int sb = src[i + 2];
                int dib = dst[i + 2];
                int sa = src[i + 3];
                int dia = dst[i + 3];
                int dor, dog, dob;

                int t;
                if (dir < 128) {
                    t = dir * sr + 0x80;
                    dor = 2 * (((t >> 8) + t) >> 8);
                } else {
                    t = (255 - dir) * (255 - sr) + 0x80;
                    dor = 2 * (255 - (((t >> 8) + t) >> 8));
                }
                if (dig < 128) {
                    t = dig * sg + 0x80;
                    dog = 2 * (((t >> 8) + t) >> 8);
                } else {
                    t = (255 - dig) * (255 - sg) + 0x80;
                    dog = 2 * (255 - (((t >> 8) + t) >> 8));
                }
                if (dib < 128) {
                    t = dib * sb + 0x80;
                    dob = 2 * (((t >> 8) + t) >> 8);
                } else {
                    t = (255 - dib) * (255 - sb) + 0x80;
                    dob = 2 * (255 - (((t >> 8) + t) >> 8));
                }

                float a = alpha * sa / 255f;
                float ac = 1 - a;

                int newRed = (int) (a * dor + ac * dir);
                int newGreen = (int) (a * dog + ac * dig);
                int newBlue = (int) (a * dob + ac * dib);
                int newAlpha = (int) (sa * alpha + dia * ac);

//                if(alpha == 1.0f) {
                dst[i] = newRed;
                dst[i + 1] = newGreen;
                dst[i + 2] = newBlue;
                dst[i + 3] = newAlpha;
//                } else {
//                    dst[i] = PixelUtils.clamp(newRed);
//                    dst[i + 1] = PixelUtils.clamp(newGreen);
//                    dst[i + 2] = PixelUtils.clamp(newBlue);
//                    dst[i + 3] = PixelUtils.clamp(newAlpha);
//                }
            }
        }
    }

}
