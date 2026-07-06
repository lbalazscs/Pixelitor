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

package pixelitor.autopaint;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.util.PPoint;

import java.awt.geom.Point2D;
import java.util.SplittableRandom;

/**
 * Immutable configuration for a single auto-paint run.
 */
public record AutoPaintSettings(
    AbstractBrushTool tool,
    int numStrokes,
    int minStrokeLength,
    int maxStrokeLength,
    StrokeDirection strokeDirection,
    ColorMode colorMode,
    double maxCurvature) {

    // controls the "frequency" of the noise field
    private static final float NOISE_SCALE = 3.0f;

    public static AutoPaintSettings of(AbstractBrushTool tool, int numStrokes, int baseStrokeLength,
                                       StrokeDirection strokeDirection, ColorMode colorMode,
                                       double lengthVariation, double maxCurvature) {
        double lengthRange = lengthVariation * baseStrokeLength;
        return new AutoPaintSettings(tool, numStrokes,
            (int) (baseStrokeLength - lengthRange), (int) (baseStrokeLength + lengthRange),
            strokeDirection, colorMode, maxCurvature);
    }

    /**
     * Generates a random end point for a stroke starting at the given point.
     */
    public PPoint genRandomEndPoint(PPoint start, Composition comp, SplittableRandom rand) {
        Canvas canvas = comp.getCanvas();
        double angle = switch (strokeDirection) {
            case RANDOM -> rand.nextDouble() * 2 * Math.PI;
            case RADIAL -> getRadialAngle(start, canvas);
            case CIRCULAR -> getRadialAngle(start, canvas) + Math.PI / 2.0;
            case NOISE -> {
                float nx = (float) (start.getImX() * canvas.getAspectRatio() / canvas.getWidth());
                float ny = (float) (start.getImY() / canvas.getHeight());
                yield Noise.noise2(nx * NOISE_SCALE, ny * NOISE_SCALE) * Math.PI;
            }
        };

        int strokeLength = genStrokeLength(rand);
        double endX = start.getImX() + strokeLength * FastMath.cos(angle);
        double endY = start.getImY() + strokeLength * FastMath.sin(angle);
        return PPoint.lazyFromIm(endX, endY, comp.getView());
    }

    /**
     * Calculates the angle from the canvas center to the given point.
     */
    private static double getRadialAngle(PPoint point, Canvas canvas) {
        Point2D center = canvas.getImCenter();
        return FastMath.atan2(point.getImY() - center.getY(),
            point.getImX() - center.getX());
    }

    private int genStrokeLength(SplittableRandom rand) {
        return rand.nextInt(minStrokeLength, maxStrokeLength + 1);
    }
}
