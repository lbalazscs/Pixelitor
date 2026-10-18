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

import net.jafama.FastMath;
import pixelitor.Canvas;
import pixelitor.tools.AbstractBrushTool;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.random.RandomGenerator;

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

    public AutoPaintSettings {
        // tools that ignore the paint color must never touch the FG color
        if (!AutoPaint.toolUsesColors(tool)) {
            colorMode = ColorMode.FOREGROUND;
        }
    }

    public static AutoPaintSettings of(AbstractBrushTool tool, int numStrokes, int baseStrokeLength,
                                       StrokeDirection strokeDirection, ColorMode colorMode,
                                       double lengthVariation, double maxCurvature) {
        double lengthRange = lengthVariation * baseStrokeLength;
        int minLength = Math.max(1, (int) Math.round(baseStrokeLength - lengthRange));
        int maxLength = Math.max(minLength, (int) Math.round(baseStrokeLength + lengthRange));

        return new AutoPaintSettings(tool, numStrokes, minLength, maxLength,
            strokeDirection, colorMode, maxCurvature);
    }

    public Path2D createRandomStrokePath(Canvas canvas, RandomGenerator rng) {
        Point2D start = canvas.genRandomPoint(rng);
        Point2D end = genRandomEndPoint(start, canvas, rng);

        var path = new Path2D.Double(Path2D.WIND_NON_ZERO, 2);
        path.moveTo(start.getX(), start.getY());

        if (maxCurvature <= 0.0) {
            path.lineTo(end.getX(), end.getY());
            return path;
        }

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double midX = start.getX() + dx * 0.5;
        double midY = start.getY() + dy * 0.5;

        // The control point is the segment midpoint displaced along the normal.
        // Unit normal = (-dy, dx) / length and displacement = (rand - 0.5) * length * maxCurvature,
        // so the length cancels out: control = mid + (-dy, dx) * (rand - 0.5) * maxCurvature
        double factor = (rng.nextDouble() - 0.5) * maxCurvature;
        double controlX = midX - dy * factor;
        double controlY = midY + dx * factor;

        path.quadTo(controlX, controlY, end.getX(), end.getY());
        return path;
    }

    /**
     * Generates a random end point for a stroke starting at the given point.
     */
    private Point2D genRandomEndPoint(Point2D start, Canvas canvas, RandomGenerator rng) {
        double angle = strokeDirection.calcAngle(start, canvas, rng);

        int strokeLength = genStrokeLength(rng);
        double endX = start.getX() + strokeLength * FastMath.cos(angle);
        double endY = start.getY() + strokeLength * FastMath.sin(angle);
        return new Point2D.Double(endX, endY);
    }

    private int genStrokeLength(RandomGenerator rng) {
        return rng.nextInt(minStrokeLength, maxStrokeLength + 1);
    }
}
