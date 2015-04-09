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
 * A Paint that creates an "spiral gradient"
 */
public class SpiralGradientPaint implements Paint {
    private final boolean clockwise;
    private final UserDrag userDrag;
    private final Color startColor;
    private final Color endColor;
    private final MultipleGradientPaint.CycleMethod cycleMethod;

    public SpiralGradientPaint(boolean clockwise, UserDrag userDrag, Color startColor, Color endColor, MultipleGradientPaint.CycleMethod cycleMethod) {
        this.clockwise = clockwise;
        this.userDrag = userDrag;
        this.startColor = startColor;
        this.endColor = endColor;
        this.cycleMethod = cycleMethod;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return new SpiralGradientPaintContext(clockwise, userDrag, startColor, endColor, cm, cycleMethod);
    }

    @Override
    public int getTransparency() {
        int a1 = startColor.getAlpha();
        int a2 = endColor.getAlpha();
        return (((a1 & a2) == 0xFF) ? OPAQUE : TRANSLUCENT);
    }

    static class SpiralGradientPaintContext implements PaintContext {
        private final boolean clockwise;
        private final UserDrag userDrag;
        private final MultipleGradientPaint.CycleMethod cycleMethod;

        private final int startAlpha;
        private final int startRed;
        private final int startGreen;
        private final int startBlue;

        private final int endAlpha;
        private final int endRed;
        private final int endGreen;
        private final int endBlue;

        private final ColorModel cm;
        private final double drawAngle;
        private final double drawDistance;

        private SpiralGradientPaintContext(boolean clockwise, UserDrag userDrag, Color startColor, Color endColor, ColorModel cm, MultipleGradientPaint.CycleMethod cycleMethod) {
            this.clockwise = clockwise;
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
            drawAngle = userDrag.getDrawAngle() + Math.PI;  // between 0 and 2*PI


            drawDistance = userDrag.getDistance();
        }

        @Override
        public void dispose() {

        }

        @Override
        public ColorModel getColorModel() {
            return cm;
        }

        @Override
        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
            int[] rasterData = new int[w * h * 4];

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int base = (j * w + i) * 4;

                    int renderX = x + i;
                    int renderY = y + j;
//                    double renderAngle = Math.atan2(renderRelativeX, renderRelativeY) + Math.PI;

                    double renderAngle = userDrag.getAngleFromStartTo(renderX, renderY) + Math.PI;


                    double relativeAngle;
                    if (clockwise) {
                        relativeAngle = renderAngle - drawAngle;
                    } else {
                        relativeAngle = drawAngle - renderAngle;
                    }
                    if (relativeAngle < 0) {
                        relativeAngle += (2 * Math.PI);
                    }
                    relativeAngle /= (2.0 * Math.PI);

//                    double renderDist = Math.sqrt(renderRelativeX*renderRelativeX + renderRelativeY*renderRelativeY);
                    double renderDist = userDrag.getStartDistanceFrom(renderX, renderY);

                    double relativeDist = renderDist / drawDistance;

                    // relativeAngle alone would be a kind of angle gradient, and relativeDist alone would ne a kind of radial gradient
                    // but together...
                    double interpolationValue = relativeAngle + relativeDist;

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

            raster.setPixels(0, 0, w, h, rasterData);
            return raster;
        }
    }
}