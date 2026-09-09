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
 * A filter that creates a kaleidoscope effect using rectangular, brick,
 * 8-fold square, triangular, hexagonal, or octagonal truncated grid coordinates.
 */
public class GridKaleidoscopeFilter extends TransformFilter {
    public static final int GRID_SQUARE = 1;

    // 8-fold group order, but visually 4-fold rotational symmetry
    public static final int GRID_SQUARE_8_FOLD = 2;

    // regular octagons with corner squares
    public static final int GRID_OCTAGONAL_TRUNCATED = 3;

    public static final int GRID_BRICK = 4;
    public static final int GRID_TRIANGULAR = 5;
    public static final int GRID_HEXAGONAL = 6;

    private static final double TRIANGLE_ANGLE_OFFSET = Math.PI / 6.0; // 30 degrees
    private static final double TRIANGLE_SIZE_FACTOR = 0.5; // matches 0.5 period of ImageMath.triangle()

    // normalization scale for corner squares: 2.0 - sqrt(2)
    private static final double OCTAGON_CORNER_SCALE = 2.0 - ImageMath.SQRT_2;

    private final int gridType;

    private final double gridSizeX;
    private final double gridSizeY;
    private final double distortionX;
    private final double distortionY;

    private final double relCx;
    private final double relCy;

    private final double cos;
    private final double sin;
    private final boolean hasRotation;

    private double cx;
    private double cy;

    /**
     * Constructs a GridKaleidoscopeFilter with all required parameters.
     *
     * @param filterName    the name of the filter.
     * @param edgeAction    the edge handling strategy (TRANSPARENT, REPEAT_EDGE, WRAP_AROUND, REFLECT).
     * @param interpolation the interpolation method (NEAREST_NEIGHBOR, BILINEAR, BICUBIC).
     * @param gridType      the grid type (GRID_SQUARE, GRID_OCTAGONAL_TRUNCATED, GRID_BRICK, GRID_SQUARE_8_FOLD, GRID_TRIANGULAR, or GRID_HEXAGONAL).
     * @param gridSize      the vertical size of the grid cells in pixels (the horizontal size is derived from this).
     * @param angle         the rotation angle of the grid in radians.
     * @param distortionX   the horizontal sine-wave distortion amount.
     * @param distortionY   the vertical sine-wave distortion amount.
     * @param center        the relative center point of the effect (values between 0 and 1).
     */
    public GridKaleidoscopeFilter(String filterName,
                                  int edgeAction, int interpolation,
                                  int gridType, double gridSize,
                                  double angle,
                                  double distortionX, double distortionY,
                                  Point2D center) {
        super(filterName, edgeAction, interpolation);

        this.gridType = gridType;

        // scale down triangular grid to match square/brick tile size
        double effectiveGridSize = (gridType == GRID_TRIANGULAR)
            ? gridSize * TRIANGLE_SIZE_FACTOR
            : gridSize;
        this.gridSizeX = (gridType == GRID_TRIANGULAR || gridType == GRID_HEXAGONAL)
            ? effectiveGridSize / ImageMath.COS_30
            : effectiveGridSize;
        this.gridSizeY = effectiveGridSize;

        this.distortionX = distortionX;
        this.distortionY = distortionY;
        this.relCx = center.getX();
        this.relCy = center.getY();

        // add 30° offset so triangles are vertical (flat bottom) at angle = 0
        double effectiveAngle = (gridType == GRID_TRIANGULAR)
            ? angle + TRIANGLE_ANGLE_OFFSET
            : angle;

        this.cos = Math.cos(effectiveAngle);
        this.sin = Math.sin(effectiveAngle);
        this.hasRotation = effectiveAngle != 0.0;
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
        if (hasRotation) {
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

        double imgX;
        double imgY;

        if (gridType == GRID_TRIANGULAR || gridType == GRID_HEXAGONAL) {
            // isometric coordinates in an equilateral triangular lattice
            double v = ry / gridSizeY;
            double u = (rx / gridSizeX) - 0.5 * v;

            int m = (int) Math.floor(u);
            int n = (int) Math.floor(v);
            double fu = u - m;
            double fv = v - n;

            // determine weights for vertices based on whether the point
            // is in the upward-pointing or downward-pointing triangle
            double w0, w1, w2;
            if (fu + fv < 1.0) {
                w0 = 1.0 - fu - fv;
                w1 = fu;
                w2 = fv;
            } else {
                w0 = fu + fv - 1.0;
                w1 = 1.0 - fv;
                w2 = 1.0 - fu;
            }

            // in the p3m1 reflection group, vertex colors in
            // {0, 1, 2} are invariant under mirror reflections
            int c00 = Math.floorMod(m - n, 3);
            double lambda1;
            double lambda2;
            switch (c00) {
                case 0 -> {
                    lambda1 = w1;
                    lambda2 = w2;
                }
                case 1 -> {
                    lambda1 = w0;
                    lambda2 = w1;
                }
                case 2 -> {
                    lambda1 = w2;
                    lambda2 = w0;
                }
                default -> throw new IllegalStateException("Unexpected c00: " + c00);
            }

            // fold along the altitudes for hexagonal D6 symmetry / wallpaper group p6m
            if (gridType == GRID_HEXAGONAL) {
                double lambda0 = Math.max(0.0, 1.0 - lambda1 - lambda2);

                // sort lambda0, lambda1, lambda2 in descending order (a >= b >= c)
                // to map into the 30°-60°-90° fundamental domain
                double a = lambda0;
                double b = lambda1;
                double c = lambda2;

                if (a < b) {
                    double t = a;
                    a = b;
                    b = t;
                }
                if (b < c) {
                    double t = b;
                    b = c;
                    c = t;
                    if (a < b) {
                        t = a;
                        a = b;
                        b = t;
                    }
                }

                lambda1 = b;
                lambda2 = c;
            }

            // map barycentric coordinates of the equilateral triangle
            // V0=(0, 0), V1=(s, 0), V2=(s/2, h) to image coordinates
            imgX = (lambda1 + 0.5 * lambda2) * gridSizeX;
            imgY = lambda2 * gridSizeY;
        } else {
            // apply grid type offset
            switch (gridType) {
                case GRID_SQUARE, GRID_SQUARE_8_FOLD, GRID_OCTAGONAL_TRUNCATED -> {
                    // regular square lattice: no offset
                }
                case GRID_BRICK -> {
                    int row = (int) Math.floor(ry / gridSizeY);
                    if ((row & 1) != 0) {
                        rx += 0.5 * gridSizeX;
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + gridType);
            }

            // map the coordinates into a single grid cell of size [0,1] x [0,1]
            double gridX = ImageMath.triangle(rx / gridSizeX);
            double gridY = ImageMath.triangle(ry / gridSizeY);

            // fold along the diagonal x = y for 8-fold (D4 / p4m) symmetry
            if ((gridType == GRID_SQUARE_8_FOLD || gridType == GRID_OCTAGONAL_TRUNCATED) && gridX < gridY) {
                double temp = gridX;
                gridX = gridY;
                gridY = temp;
            }

            // partition into regular octagon and corner square for Archimedean 4.8^2 tiling
            if (gridType == GRID_OCTAGONAL_TRUNCATED && gridX + gridY > ImageMath.SQRT_2) {
                double deltaX = 1.0 - gridX;
                double deltaY = 1.0 - gridY;

                // rotate 45° around corner (1, 1) and scale to [0, 1]
                gridX = (deltaX + deltaY) / OCTAGON_CORNER_SCALE;
                gridY = (deltaY - deltaX) / OCTAGON_CORNER_SCALE;
            }

            // map the normalized grid coordinates back to image coordinates
            imgX = gridX * gridSizeX;
            imgY = gridY * gridSizeY;
        }

        // rotate back if needed and translate back from origin
        if (hasRotation) {
            out[0] = (float) (cx + (imgX * cos - imgY * sin));
            out[1] = (float) (cy + (imgX * sin + imgY * cos));
        } else {
            out[0] = (float) (cx + imgX);
            out[1] = (float) (cy + imgY);
        }
    }
}
