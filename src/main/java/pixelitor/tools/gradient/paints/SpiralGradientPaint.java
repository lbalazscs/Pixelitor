/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.util.Drag;

import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
import static java.awt.MultipleGradientPaint.CycleMethod.REPEAT;

/**
 * A Paint that creates a "spiral gradient"
 */
public record SpiralGradientPaint(boolean clockwise, Drag drag,
                                  Color startColor, Color endColor,
                                  CycleMethod cycleMethod) implements Paint {
    private static final int AA_RES = 4; // the resolution of AA supersampling
    private static final int AA_RES2 = AA_RES * AA_RES;

    @Override
    public PaintContext createContext(ColorModel cm,
                                      Rectangle deviceBounds, Rectangle2D userBounds,
                                      AffineTransform xform, RenderingHints hints) {
        Drag trDrag = drag.imTransformedCopy(xform);
        if (cm.getNumComponents() == 1) {
            return new GraySpiralGradientPaintContext(clockwise, trDrag,
                startColor, endColor, cm, cycleMethod);
        } else {
            return new SpiralGradientPaintContext(clockwise, trDrag,
                startColor, endColor, cm, cycleMethod);
        }
    }

    @Override
    public int getTransparency() {
        int a1 = startColor.getAlpha();
        int a2 = endColor.getAlpha();
        return (a1 & a2) == 0xFF ? OPAQUE : TRANSLUCENT;
    }

    static class SpiralGradientPaintContext implements PaintContext {
        protected final boolean clockwise;
        protected final Drag drag;
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
        protected final double dragDistance;

        private SpiralGradientPaintContext(boolean clockwise, Drag drag,
                                           Color startColor, Color endColor,
                                           ColorModel cm, CycleMethod cycleMethod) {
            this.clockwise = clockwise;
            this.drag = drag;
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
            drawAngle = drag.getDrawAngle() + Math.PI;  // between 0 and 2*PI

            dragDistance = drag.calcImDist();
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
                for (int i = 0; i < width; i++) {
                    int base = (j * width + i) * 4;

                    int x = startX + i;
                    int y = startY + j;

                    double interpolated = interpolate(x, y);

                    boolean needsAA = false;
                    if (cycleMethod != REFLECT) {
                        double threshold;
                        if (cycleMethod == NO_CYCLE) {
                            threshold = 0.5 / dragDistance;
                        } else { // REPEAT
                            threshold = 1.0 / dragDistance;
                        }
                        needsAA = interpolated > 1.0 - threshold || interpolated < threshold;
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
            double renderAngle = drag.getAngleFromStartTo(x, y) + Math.PI;
            double relativeAngle;
            if (clockwise) {
                relativeAngle = renderAngle - drawAngle;
            } else {
                relativeAngle = drawAngle - renderAngle;
            }
            if (relativeAngle < 0) {
                relativeAngle += 2 * Math.PI;
            }
            relativeAngle /= 2.0 * Math.PI;

            double renderDist = drag.getStartDistanceFrom(x, y);

            double relativeDist = renderDist / dragDistance;

            // relativeAngle alone would be a kind of angle gradient,
            // and relativeDist alone would be a kind of radial gradient
            // but together...
            double interpolated = relativeAngle + relativeDist;

            interpolated %= 1.0f; // between 0..1

            if (cycleMethod == REFLECT) {
                if (interpolated < 0.5) {
                    interpolated = 2.0f * interpolated;
                } else {
                    interpolated = 2.0f * (1 - interpolated);
                }
            } else if (cycleMethod == REPEAT) {
                if (interpolated < 0.5) {
                    interpolated = 2.0f * interpolated;
                } else {
                    interpolated = 2.0f * (interpolated - 0.5);
                }
            }
            return interpolated;
        }
    }

    private static class GraySpiralGradientPaintContext extends SpiralGradientPaintContext {
        private final int startGray;
        private final int endGray;

        private GraySpiralGradientPaintContext(boolean clockwise, Drag drag,
                                               Color startColor, Color endColor,
                                               ColorModel cm, CycleMethod cycleMethod) {
            super(clockwise, drag, startColor, endColor, cm, cycleMethod);

            startGray = startColor.getRed();
            endGray = endColor.getRed();
        }

        @Override
        public Raster getRaster(int startX, int startY, int width, int height) {
            WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
            int[] rasterData = new int[width * height];

            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    int base = j * width + i;

                    int x = startX + i;
                    int y = startY + j;

                    double interpolated = interpolate(x, y);

                    boolean needsAA = false;
                    if (cycleMethod != REFLECT) {
                        double threshold;
                        if (cycleMethod == NO_CYCLE) {
                            threshold = 0.5 / dragDistance;
                        } else { // REPEAT
                            threshold = 1.0 / dragDistance;
                        }
                        needsAA = interpolated > 1.0 - threshold || interpolated < threshold;
                    }

                    if (needsAA) {
                        int g = 0;

                        for (int m = 0; m < AA_RES; m++) {
                            double yy = y + 1.0 / AA_RES * m - 0.5;
                            for (int n = 0; n < AA_RES; n++) {
                                double xx = x + 1.0 / AA_RES * n - 0.5;

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