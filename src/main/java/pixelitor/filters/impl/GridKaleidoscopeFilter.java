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
    private int style;

    private double gridSizeX = 50;
    private double gridSizeY = 50;
    private double angle = 0;
    private double distortionX = 0;
    private double distortionY = 0;

    private double relCx = 0.5;
    private double relCy = 0.5;

    private double cx;
    private double cy;

    private double cos;
    private double sin;

    public GridKaleidoscopeFilter(String filterName) {
        super(filterName);
    }

    public void setGridSizeX(double size) {
        this.gridSizeX = size;
    }

    public void setGridSizeY(double size) {
        this.gridSizeY = size;
    }

    public void setGridAngle(double angle) {
        this.angle = angle;

        cos = Math.cos(this.angle);
        sin = Math.sin(this.angle);
    }

    public void setDistortionX(double distortion) {
        this.distortionX = distortion;
    }

    public void setDistortionY(double distortion) {
        this.distortionY = distortion;
    }

    public void setCenter(Point2D center) {
        this.relCx = center.getX();
        this.relCy = center.getY();
    }

    public void setStyle(int style) {
        this.style = style;
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
        double rxCopy = rx;
        if (distortionX != 0) {
            rx += distortionX * Math.sin(ry / gridSizeY * Math.PI);
        }
        if (distortionY != 0) {
            ry += distortionY * Math.sin(rxCopy / gridSizeX * Math.PI);
        }

        double gridX = switch (style) {
            case STYLE_MIRROR -> ImageMath.triangle(rx / gridSizeX);
            case STYLE_REPEAT -> ImageMath.mod(2 * rx / gridSizeX, 1);
            default -> throw new IllegalStateException("Unexpected value: " + style);
        };

        double gridY = switch (style) {
            case STYLE_MIRROR -> ImageMath.triangle(ry / gridSizeY);
            case STYLE_REPEAT -> ImageMath.mod(2 * ry / gridSizeY, 1);
            default -> throw new IllegalStateException("Unexpected value: " + style);
        };

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
}