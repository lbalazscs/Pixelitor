/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools.gradientpaints;

import pixelitor.tools.UserDrag;

import java.awt.Color;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;

/**
 * A Paint that creates an "angle gradient"
 */
public class AngleGradientPaint implements Paint {
    private final UserDrag userDrag;
    private final Color startColor;
    private final Color endColor;
    private final MultipleGradientPaint.CycleMethod cycleMethod;

    private static final int AA_RES = 4; // the resolution of AA supersampling
    private static final int AA_RES2 = AA_RES * AA_RES;

    public AngleGradientPaint(UserDrag userDrag, Color startColor, Color endColor, MultipleGradientPaint.CycleMethod cycleMethod) {
        this.userDrag = userDrag;
        this.startColor = startColor;
        this.endColor = endColor;
        this.cycleMethod = cycleMethod;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        int numComponents = cm.getNumComponents();

        if (numComponents == 1) {
            return new GrayAngleGradientPaintContext(userDrag, startColor, endColor, cm, cycleMethod);
        }

        return new AngleGradientPaintContext(userDrag, startColor, endColor, cm, cycleMethod);
    }

    @Override
    public int getTransparency() {
        int a1 = startColor.getAlpha();
        int a2 = endColor.getAlpha();
        return (((a1 & a2) == 0xFF) ? OPAQUE : TRANSLUCENT);
    }

    private static class AngleGradientPaintContext implements PaintContext {
        protected final UserDrag userDrag;
        protected final MultipleGradientPaint.CycleMethod cycleMethod;

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

        private AngleGradientPaintContext(UserDrag userDrag, Color startColor, Color endColor, ColorModel cm, MultipleGradientPaint.CycleMethod cycleMethod) {
            this.userDrag = userDrag;
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
            drawAngle = userDrag.getDrawAngle();
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
                    double interpolationValue = getInterpolationValue(x, y);

                    boolean needsAA = false;
                    if (cycleMethod != REFLECT) {
                        double distance = userDrag.taxiCabMetric(x, y);
                        double threshold = 0.2 / distance;
                        needsAA = interpolationValue > (1.0 - threshold) || interpolationValue < threshold;
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

                                double interpolationValueAA = getInterpolationValue(xx, yy);

                                a += (int) (startAlpha + interpolationValueAA * (endAlpha - startAlpha));
                                r += (int) (startRed + interpolationValueAA * (endRed - startRed));
                                g += (int) (startGreen + interpolationValueAA * (endGreen - startGreen));
                                b += (int) (startBlue + interpolationValueAA * (endBlue - startBlue));
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
                        int a = (int) (startAlpha + interpolationValue * (endAlpha - startAlpha));
                        int r = (int) (startRed + interpolationValue * (endRed - startRed));
                        int g = (int) (startGreen + interpolationValue * (endGreen - startGreen));
                        int b = (int) (startBlue + interpolationValue * (endBlue - startBlue));

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

        public double getInterpolationValue(double x, double y) {
            double relativeAngle = userDrag.getAngleFromStartTo(x, y) - drawAngle;

            // relativeAngle is now between -2*PI and 2*PI, and the -2*PI..0 range is the same as 0..2*PI

            double interpolationValue = (relativeAngle / (Math.PI * 2)) + 1.0; // between 0..2
            interpolationValue %= 1.0f; // between 0..1

            if (cycleMethod == REFLECT) {
                if (interpolationValue < 0.5) {
                    interpolationValue = 2.0f * interpolationValue;
                } else {
                    interpolationValue = 2.0f * (1 - interpolationValue);
                }
            } else if (cycleMethod == REPEAT) {
                if (interpolationValue < 0.5) {
                    interpolationValue = 2.0f * interpolationValue;
                } else {
                    interpolationValue = 2.0f * (interpolationValue - 0.5);
                }
            }
            return interpolationValue;
        }
    }

    private static class GrayAngleGradientPaintContext extends AngleGradientPaintContext {
        private final int startGray;
        private final int endGray;

        private GrayAngleGradientPaintContext(UserDrag userDrag, Color startColor, Color endColor, ColorModel cm, MultipleGradientPaint.CycleMethod cycleMethod) {
            super(userDrag, startColor, endColor, cm, cycleMethod);

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
                    double interpolationValue = getInterpolationValue(x, y);

                    boolean needsAA = false;
                    if (cycleMethod != REFLECT) {
                        double distance = userDrag.taxiCabMetric(x, y);
                        double threshold = 0.2 / distance;
                        needsAA = interpolationValue > (1.0 - threshold) || interpolationValue < threshold;
                    }

                    if (needsAA) {
                        int g = 0;

                        for (int m = 0; m < AA_RES; m++) {
                            double yy = y + 1.0 / AA_RES * m - 0.5;
                            for (int n = 0; n < AA_RES; n++) {
                                double xx = x + 1.0 / AA_RES * n - 0.5;

                                double interpolationValueAA = getInterpolationValue(xx, yy);

                                g += (int) (startGray + interpolationValueAA * (endGray - startGray));
                            }
                        }
                        g /= AA_RES2;

                        rasterData[base] = g;
                    } else { // no AA
                        int g = (int) (startGray + interpolationValue * (endGray - startGray));

                        rasterData[base] = g;
                    }
                }
            }

            raster.setPixels(0, 0, width, height, rasterData);
            return raster;
        }
    }
}
