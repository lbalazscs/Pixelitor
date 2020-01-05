/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Color;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;

/**
 * A Paint that creates an "angle gradient"
 */
public class AngleGradientPaint implements Paint {
    private final ImDrag imDrag;
    private final Color startColor;
    private final Color endColor;
    private final CycleMethod cycleMethod;

    private static final int AA_RES = 4; // the resolution of AA supersampling
    private static final int AA_RES2 = AA_RES * AA_RES;

    public AngleGradientPaint(ImDrag imDrag, Color startColor, Color endColor, CycleMethod cycleMethod) {
        this.imDrag = imDrag;
        this.startColor = startColor;
        this.endColor = endColor;
        this.cycleMethod = cycleMethod;
    }

    @Override
    public PaintContext createContext(ColorModel cm,
                                      Rectangle deviceBounds, Rectangle2D userBounds,
                                      AffineTransform xform, RenderingHints hints) {
        int numComponents = cm.getNumComponents();

        if (numComponents == 1) {
            return new GrayAngleGradientPaintContext(imDrag, startColor, endColor, cm, cycleMethod);
        }

        return new AngleGradientPaintContext(imDrag, startColor, endColor, cm, cycleMethod);
    }

    @Override
    public int getTransparency() {
        int a1 = startColor.getAlpha();
        int a2 = endColor.getAlpha();
        return (a1 & a2) == 0xFF ? OPAQUE : TRANSLUCENT;
    }

    private static class AngleGradientPaintContext implements PaintContext {
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
        protected final double drawAngle;

        private AngleGradientPaintContext(ImDrag imDrag,
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
            drawAngle = imDrag.getDrawAngle();
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
            var raster = cm.createCompatibleWritableRaster(width, height);
            int[] rasterData = new int[width * height * 4];

            for (int j = 0; j < height; j++) {
                int y = startY + j;
                for (int i = 0; i < width; i++) {
                    int base = (j * width + i) * 4;
                    int x = startX + i;
                    double interpolation = getInterpolation(x, y);

                    boolean needsAA = false;
                    if (cycleMethod != REFLECT) {
                        double distance = imDrag.taxiCabMetric(x, y);
                        double threshold = 0.2 / distance;
                        needsAA = interpolation > (1.0 - threshold) || interpolation < threshold;
                    }

                    if (needsAA) {
                        int a = 0;
                        int r = 0;
                        int g = 0;
                        int b = 0;

                        for (int m = 0; m < AA_RES; m++) {
                            double yy = y + 1.0 / AA_RES * m - 0.5;
                            for (int n = 0; n < AA_RES; n++) {
                                double xx = x + 1.0 / AA_RES * n - 0.5;

                                double interpolationAA = getInterpolation(xx, yy);

                                a += (int) (startAlpha + interpolationAA * (endAlpha - startAlpha));
                                r += (int) (startRed + interpolationAA * (endRed - startRed));
                                g += (int) (startGreen + interpolationAA * (endGreen - startGreen));
                                b += (int) (startBlue + interpolationAA * (endBlue - startBlue));
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
                        int a = (int) (startAlpha + interpolation * (endAlpha - startAlpha));
                        int r = (int) (startRed + interpolation * (endRed - startRed));
                        int g = (int) (startGreen + interpolation * (endGreen - startGreen));
                        int b = (int) (startBlue + interpolation * (endBlue - startBlue));

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

        public double getInterpolation(double x, double y) {
            double relativeAngle = imDrag.getAngleFromStartTo(x, y) - drawAngle;

            // relativeAngle is now between -2*PI and 2*PI, and the -2*PI..0 range is the same as 0..2*PI

            double interpolation = relativeAngle / (Math.PI * 2) + 1.0; // between 0..2
            interpolation %= 1.0f; // between 0..1

            if (cycleMethod == REFLECT) {
                if (interpolation < 0.5) {
                    interpolation = 2.0f * interpolation;
                } else {
                    interpolation = 2.0f * (1 - interpolation);
                }
            } else if (cycleMethod == REPEAT) {
                if (interpolation < 0.5) {
                    interpolation = 2.0f * interpolation;
                } else {
                    interpolation = 2.0f * (interpolation - 0.5);
                }
            }
            return interpolation;
        }
    }

    private static class GrayAngleGradientPaintContext extends AngleGradientPaintContext {
        private final int startGray;
        private final int endGray;

        private GrayAngleGradientPaintContext(ImDrag imDrag,
                                              Color startColor, Color endColor,
                                              ColorModel cm, CycleMethod cycleMethod) {
            super(imDrag, startColor, endColor, cm, cycleMethod);

            startGray = startColor.getRed();
            endGray = endColor.getRed();
        }

        @Override
        public Raster getRaster(int startX, int startY, int width, int height) {
            var raster = cm.createCompatibleWritableRaster(width, height);
            int[] rasterData = new int[width * height];

            for (int j = 0; j < height; j++) {
                int y = startY + j;
                for (int i = 0; i < width; i++) {
                    int base = j * width + i;
                    int x = startX + i;
                    double interpolation = getInterpolation(x, y);

                    boolean needsAA = false;
                    if (cycleMethod != REFLECT) {
                        double distance = imDrag.taxiCabMetric(x, y);
                        double threshold = 0.2 / distance;
                        needsAA = interpolation > (1.0 - threshold) || interpolation < threshold;
                    }

                    if (needsAA) {
                        int g = 0;

                        for (int m = 0; m < AA_RES; m++) {
                            double yy = y + 1.0 / AA_RES * m - 0.5;
                            for (int n = 0; n < AA_RES; n++) {
                                double xx = x + 1.0 / AA_RES * n - 0.5;

                                double interpolationAA = getInterpolation(xx, yy);

                                g += (int) (startGray + interpolationAA * (endGray - startGray));
                            }
                        }
                        g /= AA_RES2;

                        rasterData[base] = g;
                    } else { // no AA
                        int g = (int) (startGray + interpolation * (endGray - startGray));

                        rasterData[base] = g;
                    }
                }
            }

            raster.setPixels(0, 0, width, height, rasterData);
            return raster;
        }
    }
}
