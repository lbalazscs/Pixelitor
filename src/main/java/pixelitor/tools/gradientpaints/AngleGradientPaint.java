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
 * A Paint that creates an "angle gradient"
 */
public class AngleGradientPaint implements Paint {
    private final UserDrag userDrag;
    private final Color startColor;
    private final Color endColor;
    private final MultipleGradientPaint.CycleMethod cycleMethod;

    public AngleGradientPaint(UserDrag userDrag, Color startColor, Color endColor, MultipleGradientPaint.CycleMethod cycleMethod) {
        this.userDrag = userDrag;
        this.startColor = startColor;
        this.endColor = endColor;
        this.cycleMethod = cycleMethod;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return new AngleGradientPaintContext(userDrag, startColor, endColor, cm, cycleMethod);
    }

    @Override
    public int getTransparency() {
        int a1 = startColor.getAlpha();
        int a2 = endColor.getAlpha();
        return (((a1 & a2) == 0xFF) ? OPAQUE : TRANSLUCENT);
    }

    static class AngleGradientPaintContext implements PaintContext {
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

        @Override
        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
            int[] rasterData = new int[w * h * 4];

            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int base = (j * w + i) * 4;

                    double relativeAngle = userDrag.getAngleFromStartTo(x + i, y + j) - drawAngle;

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
