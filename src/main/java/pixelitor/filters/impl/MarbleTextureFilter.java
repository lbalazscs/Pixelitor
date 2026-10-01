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

package pixelitor.filters.impl;

import com.jhlabs.image.Colormap;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.Marble;

import java.awt.geom.Point2D;
import java.util.function.DoubleBinaryOperator;

import static com.jhlabs.image.WaveType.wave01;
import static com.jhlabs.math.Noise.*;
import static net.jafama.FastMath.*;

/**
 * Generates a procedural marble-like texture: a periodic wave (lines, grid,
 * rings, spiral or star) is distorted by Perlin noise and mapped through
 * a gradient. The source pixels are ignored; only the image size is used.
 */
public class MarbleTextureFilter extends PointFilter {
    public static final int TYPE_LINES = 1;
    public static final int TYPE_GRID = 2;
    public static final int TYPE_RINGS = 3;
    public static final int TYPE_SPIRAL = 4;
    public static final int TYPE_STAR = 5;

    // cos and sin divided by zoom to rotate and zoom in one step without per-pixel division
    private final double cos;
    private final double sin;

    private final double cx, cy;
    private final double strength;
    private final double detailsStrength;
    private final int octaves;
    private final double phase;
    private final int type;

    private final DoubleBinaryOperator distFunc;
    private final double distScale;

    private final int waveType;
    private final boolean smoothDetails;
    private final Colormap colormap;

    public MarbleTextureFilter(int type, PolarMetric metric, Point2D center, int waveType,
                               double angle, double zoom, double strength,
                               int octaves, double detailsStrength,
                               Colormap colormap, boolean smoothDetails, double phase) {
        super(Marble.NAME);
        this.type = type;
        this.distFunc = metric.getDistFunction();
        this.distScale = 1.0 / metric.getCircleFitFactor();

        this.cx = center.getX();
        this.cy = center.getY();
        this.waveType = waveType;

        double invZoom = 1.0 / zoom;
        this.cos = cos(angle) * invZoom;
        this.sin = sin(angle) * invZoom;

        this.strength = strength;
        this.octaves = octaves;
        this.detailsStrength = detailsStrength;
        this.colormap = colormap;
        this.smoothDetails = smoothDetails;
        this.phase = phase;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        double dx = x - cx;
        double dy = y - cy;

        // rotate around the center, then zoom
        double px = cos * dx + sin * dy;
        double py = cos * dy - sin * dx;

        double phaseOffset = distortionAt(px, py) + phase;
        float value = switch (type) {
            case TYPE_LINES -> linesValue(px, phaseOffset);
            case TYPE_GRID -> gridValue(px, py, phaseOffset);
            case TYPE_RINGS -> ringsValue(px, py, phaseOffset);
            case TYPE_SPIRAL -> spiralValue(px, py, phaseOffset);
            case TYPE_STAR -> starValue(px, py, phaseOffset);
            default -> throw new IllegalStateException("type = " + type);
        };

        return colormap.getColor(value);
    }

    private double dist(double px, double py) {
        return distScale * distFunc.applyAsDouble(px, py);
    }

    private double distortionAt(double x, double y) {
        double base = strength * noise2((float) (x * 0.1), (float) (y * 0.1));
        double details = smoothDetails
            ? turbulence2Smooth(x * 0.2, y * 0.2, octaves)
            : turbulence2(x * 0.2, y * 0.2, octaves);
        return base + detailsStrength * details;
    }

    private float linesValue(double px, double phaseOffset) {
        return (float) wave01(px + phaseOffset, waveType);
    }

    private float gridValue(double px, double py, double phaseOffset) {
        double phaseOffset2 = distortionAt(-py, -px) + phase;
        return (float) (wave01(px + phaseOffset, waveType) + wave01(py + phaseOffset2, waveType)) / 2.0f;
    }

    private float ringsValue(double px, double py, double phaseOffset) {
        return (float) wave01(phaseOffset + dist(px, py), waveType);
    }

    private float spiralValue(double px, double py, double phaseOffset) {
        double dist = dist(px, py);
        double pixelAngle = atan2(py, px);
        return (float) wave01(phaseOffset + (dist + pixelAngle), waveType);
    }

    private float starValue(double px, double py, double phaseOffset) {
        double pixelAngle = atan2(py, px);
        return (float) wave01(phaseOffset + pixelAngle * 10.0, waveType);
    }
}
