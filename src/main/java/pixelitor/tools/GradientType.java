/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.tools;

import pixelitor.tools.gradientpaints.AngleGradientPaint;
import pixelitor.tools.gradientpaints.DiamondGradientPaint;
import pixelitor.tools.gradientpaints.SpiralGradientPaint;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * The type of the gradient in the gradient tool
 */
public enum GradientType {
    LINEAR("Linear") {
        @Override
        public Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod) {
            Point2D.Double start = imDrag.getStartPoint();
            Point2D.Double end = imDrag.getEndPoint();

            return new LinearGradientPaint(start, end, ImageUtils.FRACTIONS_2_COLOR_UNIFORM, colors, cycleMethod, colorSpaceType, gradientTransform);
        }
    }, RADIAL("Radial") {
        @Override
        public Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod) {
            float radius = (float) imDrag.getDistance();
            Point2D.Double center = imDrag.getStartPoint();

            return new RadialGradientPaint(center, radius, center, ImageUtils.FRACTIONS_2_COLOR_UNIFORM, colors, cycleMethod, colorSpaceType, gradientTransform);
        }
    }, ANGLE("Angle") {
        @Override
        public Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod) {
            return new AngleGradientPaint(imDrag, colors[0], colors[1], cycleMethod);
        }
    }, SPIRAL_CW("CW Spiral") {
        @Override
        public Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod) {
            return new SpiralGradientPaint(true, imDrag, colors[0], colors[1], cycleMethod);
        }
    }, SPIRAL_CCW("CCW Spiral") {
        @Override
        public Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod) {
            return new SpiralGradientPaint(false, imDrag, colors[0], colors[1], cycleMethod);
        }
    }, DIAMOND("Diamond") {
        @Override
        public Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod) {
            return new DiamondGradientPaint(imDrag, colors[0], colors[1], cycleMethod);
        }
    };

    private static final AffineTransform gradientTransform = new AffineTransform();
    private static final ColorSpaceType colorSpaceType = ColorSpaceType.SRGB;

    private final String guiName;

    GradientType(String guiName) {
        this.guiName = guiName;
    }

    public abstract Paint getGradient(ImDrag imDrag, Color[] colors, CycleMethod cycleMethod);

    @Override
    public String toString() {
        return guiName;
    }
}
