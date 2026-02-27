/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.tools.gradient;

import pixelitor.tools.gradient.paints.AngleGradientPaint;
import pixelitor.tools.gradient.paints.DiamondGradientPaint;
import pixelitor.tools.gradient.paints.SpiralGradientPaint;
import pixelitor.tools.util.Drag;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static java.awt.MultipleGradientPaint.ColorSpaceType.SRGB;

/**
 * The type of a gradient.
 */
public enum GradientType {
    LINEAR("Linear") {
        @Override
        public Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle) {
            Point2D start = drag.getStartPoint();
            Point2D end = drag.getEndPoint();

            return new LinearGradientPaint(start, end, FRACTIONS,
                colors, cycle, SRGB, IDENTITY_TRANSFORM);
        }
    }, RADIAL("Radial") {
        @Override
        public Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle) {
            float radius = (float) drag.calcImLength();
            Point2D center = drag.getStartPoint();

            return new RadialGradientPaint(center, radius, center, FRACTIONS,
                colors, cycle, SRGB, IDENTITY_TRANSFORM);
        }
    }, ANGLE("Angle") {
        @Override
        public Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle) {
            return new AngleGradientPaint(drag, colors[0], colors[1], cycle);
        }
    }, SPIRAL_CW("CW Spiral") {
        @Override
        public Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle) {
            return new SpiralGradientPaint(true, drag, colors[0], colors[1], cycle);
        }
    }, SPIRAL_CCW("CCW Spiral") {
        @Override
        public Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle) {
            return new SpiralGradientPaint(false, drag, colors[0], colors[1], cycle);
        }
    }, DIAMOND("Diamond") {
        @Override
        public Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle) {
            return new DiamondGradientPaint(drag, colors[0], colors[1], cycle);
        }
    };

    private static final float[] FRACTIONS = {0.0f, 1.0f};
    private static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();
    public static final String PRESET_KEY = "Gradient Type";
    
    private final String displayName;

    GradientType(String displayName) {
        this.displayName = displayName;
    }

    public abstract Paint createPaint(Drag drag, Color[] colors, CycleMethod cycle);

    @Override
    public String toString() {
        return displayName;
    }
}
