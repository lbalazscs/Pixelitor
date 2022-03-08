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

public final class MultiplyComposite extends RGBComposite {
    public MultiplyComposite(float alpha) {
        super(alpha);
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        // The commented out code was an attempt to get this working if
        // the source has no transparency.

//        DebugNode srcNode = DebugNodes.createColorModelNode("src", srcColorModel);
//        System.out.printf("MultiplyComposite::createContext: srcNode.toJSON() = '%s'%n", srcNode.toJSON());
//        DebugNode dstNode = DebugNodes.createColorModelNode("dst", dstColorModel);
//        System.out.printf("MultiplyComposite::createContext: dstNode.toJSON() = '%s'%n", dstNode.toJSON());

//        int numComponents = srcColorModel.getNumComponents();
//        if (numComponents == 4) {
        return new Context4(extraAlpha, srcColorModel, dstColorModel);
//        } else if (numComponents == 3) {
//            return new Context3(extraAlpha, srcColorModel, dstColorModel);
//        }
//        throw new IllegalStateException();
    }

//    static class Context3 extends RGBCompositeContext {
//        public Context3(float alpha, ColorModel srcColorModel, ColorModel dstColorModel) {
//            super(alpha, srcColorModel, dstColorModel);
//        }
//
//        @Override
//        public void composeRGB(int[] src, int[] dst, float alpha) {
//            int w = Math.min(src.length, dst.length);
//
//            int srcIndex = 0;
//            int dstIndex = 0;
//            while (srcIndex < w) {
//                int sr = src[srcIndex];
//                int dir = dst[dstIndex];
//                int sg = src[srcIndex + 1];
//                int dig = dst[dstIndex + 1];
//                int sb = src[srcIndex + 2];
//                int dib = dst[dstIndex + 2];
//                int dia = dst[dstIndex + 3];
//
//                int t = dir * sr + 0x80;
//                int dor = ((t >> 8) + t) >> 8;
//                t = dig * sg + 0x80;
//                int dog = ((t >> 8) + t) >> 8;
//                t = dib * sb + 0x80;
//                int dob = ((t >> 8) + t) >> 8;
//
//                float a = alpha / 255.0f;
//                float ac = 1 - a;
//
//                dst[dstIndex] = (int) (a * dor + ac * dir);
//                dst[dstIndex + 1] = (int) (a * dog + ac * dig);
//                dst[dstIndex + 2] = (int) (a * dob + ac * dib);
//                dst[dstIndex + 3] = (int) (alpha + dia * ac);
//
//                srcIndex += 3;
//                dstIndex += 4;
//            }
//        }
//    }

    static class Context4 extends RGBCompositeContext {
        public Context4(float alpha, ColorModel srcColorModel, ColorModel dstColorModel) {
            super(alpha, srcColorModel, dstColorModel);
        }

        @Override
        public void composeRGB(int[] src, int[] dst, float alpha) {
            int w = Math.min(src.length, dst.length);

            for (int i = 0; i < w; i += 4) {
                int sr = src[i];
                int dir = dst[i];
                int sg = src[i + 1];
                int dig = dst[i + 1];
                int sb = src[i + 2];
                int dib = dst[i + 2];
                int sa = src[i + 3];
                int dia = dst[i + 3];

                int t = dir * sr + 0x80;
                int dor = ((t >> 8) + t) >> 8;
                t = dig * sg + 0x80;
                int dog = ((t >> 8) + t) >> 8;
                t = dib * sb + 0x80;
                int dob = ((t >> 8) + t) >> 8;

                float a = alpha * sa / 255.0f;
                float ac = 1 - a;

                dst[i] = (int) (a * dor + ac * dir);
                dst[i + 1] = (int) (a * dog + ac * dig);
                dst[i + 2] = (int) (a * dob + ac * dib);
                dst[i + 3] = (int) (sa * alpha + dia * ac);
            }
        }
    }
}
