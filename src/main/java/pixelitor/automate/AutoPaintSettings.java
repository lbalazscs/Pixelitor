/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.automate;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.tools.Tool;
import pixelitor.tools.util.PPoint;

import java.awt.geom.Point2D;
import java.util.SplittableRandom;

/**
 * The settings of Auto Paint
 */
class AutoPaintSettings {
    private final Tool tool;
    private final int numStrokes;
    private final float maxCurvature;
    private final int minStrokeLength;
    private final int maxStrokeLength;
    private final boolean randomColors;
    private final boolean interpolatedColors;

    public static final int ANGLE_TYPE_RANDOM = 0;
    public static final int ANGLE_TYPE_RADIAL = 1;
    public static final int ANGLE_TYPE_CIRCULAR = 2;
    public static final int ANGLE_TYPE_NOISE = 3;
    private final int angleType;

    AutoPaintSettings(Tool tool, int numStrokes, int strokeLength,
                      boolean randomColors, float lengthVariability,
                      float maxCurvature, boolean interpolatedColors, int angleType) {
        this.tool = tool;
        this.numStrokes = numStrokes;
        this.maxCurvature = maxCurvature;
        this.angleType = angleType;

        if (lengthVariability == 0.0f) {
            minStrokeLength = strokeLength;
            maxStrokeLength = strokeLength;
        } else {
            minStrokeLength = (int) (strokeLength - lengthVariability * strokeLength);
            maxStrokeLength = (int) (strokeLength + lengthVariability * strokeLength);
        }

        this.randomColors = randomColors;
        this.interpolatedColors = interpolatedColors;
    }

    public PPoint calcRandomEndPoint(PPoint start, Composition comp, SplittableRandom rand) {
        Canvas canvas = comp.getCanvas();
        double angle = switch (angleType) {
            case ANGLE_TYPE_RANDOM -> rand.nextDouble() * 2 * Math.PI;
            case ANGLE_TYPE_RADIAL -> getRadialAngle(start, canvas);
            case ANGLE_TYPE_CIRCULAR -> getRadialAngle(start, canvas) + Math.PI / 2.0;
            case ANGLE_TYPE_NOISE -> {
                float nx = (float) (start.getImX() * canvas.getAspectRatio() / canvas.getWidth());
                float ny = (float) (start.getImY() / canvas.getHeight());
                yield Noise.noise2(nx * 3.0f, ny * 3.0f) * Math.PI;
            }
            default -> throw new IllegalStateException("Unexpected value: " + angleType);
        };

        int strokeLength = genStrokeLength(rand);
        double endX = start.getImX() + strokeLength * FastMath.cos(angle);
        double endY = start.getImY() + strokeLength * FastMath.sin(angle);
        return PPoint.lazyFromIm(endX, endY, comp.getView());
    }

    private static double getRadialAngle(PPoint point, Canvas canvas) {
        Point2D center = canvas.getImCenter();
        return FastMath.atan2(point.getImY() - center.getY(),
            point.getImX() - center.getX());
    }

    public float getMaxCurvature() {
        return maxCurvature;
    }

    public Tool getTool() {
        return tool;
    }

    public int getNumStrokes() {
        return numStrokes;
    }

    private int genStrokeLength(SplittableRandom rand) {
        if (minStrokeLength == maxStrokeLength) {
            return minStrokeLength;
        } else {
            return rand.nextInt(minStrokeLength, maxStrokeLength + 1);
        }
    }

    public boolean useRandomColors() {
        return randomColors;
    }

    public boolean useInterpolatedColors() {
        return interpolatedColors;
    }

    public boolean changeColors() {
        return randomColors || interpolatedColors;
    }
}
