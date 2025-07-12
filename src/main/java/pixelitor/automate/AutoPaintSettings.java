/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.util.PPoint;

import java.awt.geom.Point2D;
import java.util.SplittableRandom;

/**
 * The settings that control the auto-painting.
 */
public class AutoPaintSettings {
    public static final int DIRECTION_RANDOM = 0;
    public static final int DIRECTION_RADIAL = 1;
    public static final int DIRECTION_CIRCULAR = 2;
    public static final int DIRECTION_NOISE = 3;
    private final int strokeDirection;

    private static final float NOISE_SCALE = 3.0f;

    private final AbstractBrushTool tool;
    private final int numStrokes;
    private final double maxCurvature;
    private final int minStrokeLength;
    private final int maxStrokeLength;
    private final boolean useRandomColors;
    private final boolean useInterpolatedColors;

    AutoPaintSettings(AbstractBrushTool tool, int numStrokes, int baseStrokeLength, int strokeDirection,
                      boolean useRandomColors, boolean useInterpolatedColors,
                      double lengthVariation, double maxCurvature) {
        this.tool = tool;
        this.numStrokes = numStrokes;
        this.maxCurvature = maxCurvature;
        this.strokeDirection = strokeDirection;
        this.useRandomColors = useRandomColors;
        this.useInterpolatedColors = useInterpolatedColors;

        double variation = lengthVariation * baseStrokeLength;
        this.minStrokeLength = (int) (baseStrokeLength - variation);
        this.maxStrokeLength = (int) (baseStrokeLength + variation);
    }

    /**
     * Generates a random end point for a stroke starting at the given point.
     */
    public PPoint genRandomEndPoint(PPoint start, Composition comp, SplittableRandom rand) {
        Canvas canvas = comp.getCanvas();
        double angle = switch (strokeDirection) {
            case DIRECTION_RANDOM -> rand.nextDouble() * 2 * Math.PI;
            case DIRECTION_RADIAL -> getRadialAngle(start, canvas);
            case DIRECTION_CIRCULAR -> getRadialAngle(start, canvas) + Math.PI / 2.0;
            case DIRECTION_NOISE -> {
                float nx = (float) (start.getImX() * canvas.getAspectRatio() / canvas.getWidth());
                float ny = (float) (start.getImY() / canvas.getHeight());
                yield Noise.noise2(nx * NOISE_SCALE, ny * NOISE_SCALE) * Math.PI;
            }
            default -> throw new IllegalStateException("Unexpected value: " + strokeDirection);
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

    public double getMaxCurvature() {
        return maxCurvature;
    }

    public AbstractBrushTool getTool() {
        return tool;
    }

    public int getNumStrokes() {
        return numStrokes;
    }

    private int genStrokeLength(SplittableRandom rand) {
        return rand.nextInt(minStrokeLength, maxStrokeLength + 1);
    }

    public boolean useRandomColors() {
        return useRandomColors;
    }

    public boolean useInterpolatedColors() {
        return useInterpolatedColors;
    }

    public boolean changeColors() {
        return useRandomColors || useInterpolatedColors;
    }
}
