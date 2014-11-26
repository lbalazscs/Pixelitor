/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

import pixelitor.tools.gradientpaints.AngleGradientPaint;
import pixelitor.tools.gradientpaints.DiamondGradientPaint;
import pixelitor.tools.gradientpaints.SpiralGradientPaint;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 *
 */
public enum GradientType {
    LINEAR {
        @Override
        public String toString() {
            return "Linear";
        }

        @Override
        public Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
            Point2D.Float start = userDrag.getStartPoint();
            Point2D.Float end = userDrag.getEndPoint();

            return new LinearGradientPaint(start, end, ImageUtils.FRACTIONS_2_COLOR_UNIFORM, colors, cycleMethod, colorSpaceType, gradientTransform);
        }
    }, RADIAL {
        @Override
        public String toString() {
            return "Radial";
        }

        @Override
        public Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
            float radius = userDrag.getDistance();
            Point2D.Float center = userDrag.getStartPoint();

            return new RadialGradientPaint(center, radius, center, ImageUtils.FRACTIONS_2_COLOR_UNIFORM, colors, cycleMethod, colorSpaceType, gradientTransform);
        }
    }, ANGLE {
        @Override
        public String toString() {
            return "Angle";
        }

        @Override
        public Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
            return new AngleGradientPaint(userDrag, colors[0], colors[1], cycleMethod);
        }
    }, SPIRAL_CW {
        @Override
        public String toString() {
            return "CW Spiral";
        }

        @Override
        public Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
            return new SpiralGradientPaint(true, userDrag, colors[0], colors[1], cycleMethod);
        }
    }, SPIRAL_CCW {
        @Override
        public String toString() {
            return "CCW Spiral";
        }

        @Override
        public Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
            return new SpiralGradientPaint(false, userDrag, colors[0], colors[1], cycleMethod);
        }
    }, DIAMOND {
        @Override
        public String toString() {
            return "Diamond";
        }

        @Override
        public Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
            return new DiamondGradientPaint(userDrag, colors[0], colors[1], cycleMethod);
        }
    };

    private static final AffineTransform gradientTransform = new AffineTransform();
    private static final MultipleGradientPaint.ColorSpaceType colorSpaceType = MultipleGradientPaint.ColorSpaceType.SRGB;

    public abstract Paint getGradient(UserDrag userDrag, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod);
}
