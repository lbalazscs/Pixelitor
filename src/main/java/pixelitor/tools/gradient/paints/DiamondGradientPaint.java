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

package pixelitor.tools.gradient.paints;

import pixelitor.tools.util.ImDrag;

import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;

/**
 * A Paint that creates a "diamond gradient"
 */
public record DiamondGradientPaint(ImDrag imDrag, Color startColor,
                                   Color endColor,
                                   CycleMethod cycleMethod) implements Paint {
    private static final int AA_RES = 4; // the resolution of AA supersampling
    private static final int AA_RES2 = AA_RES * AA_RES;

    @Override
    public PaintContext createContext(ColorModel cm,
                                      Rectangle deviceBounds, Rectangle2D userBounds,
                                      AffineTransform xform, RenderingHints hints) {
        int numComponents = cm.getNumComponents();

        if (numComponents == 1) {
            return new GrayDiamondGradientPaintContext(imDrag,
                startColor, endColor, cm, cycleMethod);
        }

        return new DiamondGradientPaintContext(imDrag,
            startColor, endColor, cm, cycleMethod);
    }

    @Override
    public int getTransparency() {
        int a1 = startColor.getAlpha();
        int a2 = endColor.getAlpha();
        return (((a1 & a2) == 0xFF) ? OPAQUE : TRANSLUCENT);
    }

    private static class DiamondGradientPaintContext implements PaintContext {
        protected final ImDrag imDrag;
        protected final CycleMethod cycleMethod;

        private final int startAlpha;
        private final int startRed;
        private final int startGreen;
        private final int startBlue;

        private final int endAlpha;
        private final int endRed;
        private final int endGreen;
        private final int endBlue;

        protected final ColorModel cm;

        protected final float dragRelDX;
        protected final float dragRelDY;
        protected final double dragDist;

        private DiamondGradientPaintContext(ImDrag imDrag,
                                            Color startColor, Color endColor,
                                            ColorModel cm, CycleMethod cycleMethod) {
            this.imDrag = imDrag;
            this.cycleMethod = cycleMethod;

            startAlpha = startColor.getAlpha();
            startRed = startColor.getRed();
            startGreen = startColor.getGreen();
            startBlue = startColor.getBlue();

            endAlpha = endColor.getAlpha();
            endRed = endColor.getRed();
            endGreen = endColor.getGreen();
            endBlue = endColor.getBlue();

            this.cm = cm;

            dragDist = imDrag.getDistance();
            double dragDistSqr = dragDist * dragDist;
            dragRelDX = (float) (imDrag.getDX() / dragDistSqr);
            dragRelDY = (float) (imDrag.getDY() / dragDistSqr);
        }

        @Override
        public void dispose() {

        }

        @Override
        public ColorModel getColorModel() {
            return cm;
        }

        // Warning: gray subclass has exact copy of the algorithm
        @Override
        public Raster getRaster(int startX, int startY, int width, int height) {
            WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
            int[] rasterData = new int[width * height * 4];

            for (int j = 0; j < height; j++) {
                int y = startY + j;
                for (int i = 0; i < width; i++) {
                    int base = (j * width + i) * 4;
                    int x = startX + i;

                    double interpolated = interpolate(x, y);

                    boolean needsAA = false;
                    if (cycleMethod == REPEAT) {
                        double threshold = 1.0 / dragDist;
                        needsAA = interpolated > 1.0 - threshold
                            || interpolated < threshold;
                    }

                    if (needsAA) {
                        int a = 0;
                        int r = 0;
                        int g = 0;
                        int b = 0;

                        for (int m = 0; m < AA_RES; m++) {
                            float yy = (y + 1.0f / AA_RES * m - 0.5f);
                            for (int n = 0; n < AA_RES; n++) {
                                float xx = x + 1.0f / AA_RES * n - 0.5f;

                                double interpolatedAA = interpolate(xx, yy);

                                a += (int) (startAlpha + interpolatedAA * (endAlpha - startAlpha));
                                r += (int) (startRed + interpolatedAA * (endRed - startRed));
                                g += (int) (startGreen + interpolatedAA * (endGreen - startGreen));
                                b += (int) (startBlue + interpolatedAA * (endBlue - startBlue));
                            }
                        }
                        a /= AA_RES2;
                        r /= AA_RES2;
                        g /= AA_RES2;
                        b /= AA_RES2;

                        rasterData[base] = r;
                        rasterData[base + 1] = g;
                        rasterData[base + 2] = b;
                        rasterData[base + 3] = a;
                    } else { // no AA
                        int a = (int) (startAlpha + interpolated * (endAlpha - startAlpha));
                        int r = (int) (startRed + interpolated * (endRed - startRed));
                        int g = (int) (startGreen + interpolated * (endGreen - startGreen));
                        int b = (int) (startBlue + interpolated * (endBlue - startBlue));

                        rasterData[base] = r;
                        rasterData[base + 1] = g;
                        rasterData[base + 2] = b;
                        rasterData[base + 3] = a;
                    }
                }
            }

            raster.setPixels(0, 0, width, height, rasterData);
            return raster;
        }

        public double interpolate(double x, double y) {
            double dx = x - imDrag.getStartX();
            double dy = y - imDrag.getStartY();

            double v1 = Math.abs((dx * dragRelDX) + (dy * dragRelDY));
            double v2 = Math.abs((dx * dragRelDY) - (dy * dragRelDX));

            double interpolated = v1 + v2;

            switch (cycleMethod) {
                case NO_CYCLE:
                    if (interpolated > 1.0) {
                        interpolated = 1.0f;
                    }
                    break;
                case REFLECT:
                    interpolated %= 1.0;
                    if (interpolated < 0.5) {
                        interpolated = 2.0f * interpolated;
                    } else {
                        interpolated = 2.0f * (1 - interpolated);
                    }
                    break;
                case REPEAT:
                    interpolated %= 1.0;
                    if (interpolated < 0.5) {
                        interpolated = 2.0f * interpolated;
                    } else {
                        interpolated = 2.0f * (interpolated - 0.5f);
                    }
                    break;
            }
            return interpolated;
        }
    }

    private static class GrayDiamondGradientPaintContext extends DiamondGradientPaintContext {
        private final int startGray;
        private final int endGray;

        private GrayDiamondGradientPaintContext(ImDrag imDrag,
                                                Color startColor, Color endColor,
                                                ColorModel cm, CycleMethod cycleMethod) {
            super(imDrag, startColor, endColor, cm, cycleMethod);

            startGray = startColor.getRed();
            endGray = endColor.getRed();
        }

        @Override
        public Raster getRaster(int startX, int startY, int width, int height) {
            WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
            int[] rasterData = new int[width * height];

            for (int j = 0; j < height; j++) {
                int y = startY + j;
                for (int i = 0; i < width; i++) {
                    int base = (j * width + i);
                    int x = startX + i;

                    double interpolated = interpolate(x, y);

                    boolean needsAA = false;
                    if (cycleMethod == REPEAT) {
                        double threshold = 1.0 / dragDist;
                        needsAA = interpolated > (1.0 - threshold) || interpolated < threshold;
                    }

                    if (needsAA) {
                        int g = 0;

                        for (int m = 0; m < AA_RES; m++) {
                            float yy = (y + 1.0f / AA_RES * m - 0.5f);
                            for (int n = 0; n < AA_RES; n++) {
                                float xx = x + 1.0f / AA_RES * n - 0.5f;

                                double interpolatedAA = interpolate(xx, yy);

                                g += (int) (startGray + interpolatedAA * (endGray - startGray));
                            }
                        }
                        g /= AA_RES2;

                        rasterData[base] = g;
                    } else { // no AA
                        int g = (int) (startGray + interpolated * (endGray - startGray));

                        rasterData[base] = g;
                    }
                }
            }

            raster.setPixels(0, 0, width, height, rasterData);
            return raster;
        }
    }
}