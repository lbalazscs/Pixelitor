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

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.TransformFilter;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A filter that creates a kaleidoscope effect using rectangular grid coordinates.
 */
public class GridKaleidoscopeFilter extends TransformFilter {
    public static final int STYLE_MIRROR = 1;
    public static final int STYLE_REPEAT = 2;

    private final int style;

    private final double gridSizeX;
    private final double gridSizeY;
    private final double angle;
    private final double distortionX;
    private final double distortionY;

    private final double relCx;
    private final double relCy;

    private final double cos;
    private final double sin;

    private double cx;
    private double cy;

    /**
     * Constructs a GridKaleidoscopeFilter with all required parameters.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param gridSizeX     the horizontal size of the grid cells in pixels.
     * @param gridSizeY     the vertical size of the grid cells in pixels.
     * @param angle         the rotation angle of the grid in radians.
     * @param distortionX   the horizontal sine-wave distortion amount.
     * @param distortionY   the vertical sine-wave distortion amount.
     * @param center        the relative center point of the effect (values between 0 and 1).
     * @param style         the grid style (STYLE_MIRROR or STYLE_REPEAT).
     */
    public GridKaleidoscopeFilter(String filterName,
                                  int edgeAction, int interpolation,
                                  double gridSizeX, double gridSizeY,
                                  double angle,
                                  double distortionX, double distortionY,
                                  Point2D center, int style) {
        super(filterName, edgeAction, interpolation);

        this.gridSizeX = gridSizeX;
        this.gridSizeY = gridSizeY;
        this.angle = angle;
        this.distortionX = distortionX;
        this.distortionY = distortionY;
        this.relCx = center.getX();
        this.relCy = center.getY();
        this.style = style;

        this.cos = Math.cos(angle);
        this.sin = Math.sin(angle);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        cx = src.getWidth() * relCx;
        cy = src.getHeight() * relCy;

        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // translate to make (cx, cy) the origin
        double dx = x - cx;
        double dy = y - cy;

        // rotate around center
        double rx, ry;
        if (angle != 0) {
            rx = dx * cos + dy * sin;
            ry = -dx * sin + dy * cos;
        } else {
            rx = dx;
            ry = dy;
        }

        // add sine wave distortion
        double undistortedRx = rx;
        if (distortionX != 0) {
            rx += distortionX * Math.sin(ry / gridSizeY * Math.PI);
        }
        if (distortionY != 0) {
            ry += distortionY * Math.sin(undistortedRx / gridSizeX * Math.PI);
        }

        // map the coordinates into a single grid cell of size [0,1] x [0,1]
        double gridX = mapToGrid(rx / gridSizeX);
        double gridY = mapToGrid(ry / gridSizeY);

        // map the normalized grid coordinates back to image coordinates
        double imgX = gridX * gridSizeX;
        double imgY = gridY * gridSizeY;

        // rotate back if needed and translate back from origin
        if (angle != 0) {
            out[0] = (float) (cx + (imgX * cos - imgY * sin));
            out[1] = (float) (cy + (imgX * sin + imgY * cos));
        } else {
            out[0] = (float) (cx + imgX);
            out[1] = (float) (cy + imgY);
        }
    }

    private double mapToGrid(double value) {
        return switch (style) {
            case STYLE_MIRROR -> ImageMath.triangle(value);
            case STYLE_REPEAT -> ImageMath.mod(2 * value, 1);
            default -> throw new IllegalStateException("Unexpected value: " + style);
        };
    }
}
