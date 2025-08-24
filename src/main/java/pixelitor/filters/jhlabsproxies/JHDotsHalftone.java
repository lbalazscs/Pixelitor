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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.HalftoneFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JHDotsHalftone extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Dots Halftone";

    private static final boolean DEBUG_MASK = false;

    private static final int SHAPE_CIRCLE = 0;
    private static final int SHAPE_SQUARE = 1;
    private static final int SHAPE_DIAMOND = 2;
    private static final int SHAPE_CROSS = 3;
    private static final int SHAPE_X = 4;
    private static final int SHAPE_TRIANGLE = 5;
    private static final int SHAPE_HEXAGON = 6;
    private static final int SHAPE_OCTAGON = 7;
    private static final int SHAPE_STAR = 8;

    private static final double SQRT_2 = 1.4142135623730951;
    private static final double SQRT_3 = 1.7320508075688772;

    private final RangeParam dotRadius = new RangeParam("Dot Radius", 1, 10, 100);
    private final IntChoiceParam shapeParam = new IntChoiceParam("Dot Shape", new Item[]{
        new Item("Circle", SHAPE_CIRCLE),
        new Item("Square", SHAPE_SQUARE),
        new Item("Diamond", SHAPE_DIAMOND),
        new Item("Cross", SHAPE_CROSS),
        new Item("X", SHAPE_X),
        new Item("Triangle", SHAPE_TRIANGLE),
        new Item("Hexagon", SHAPE_HEXAGON),
        new Item("Octagon", SHAPE_OCTAGON),
        new Item("Star", SHAPE_STAR),
    });

    private final IntChoiceParam gridParam = new IntChoiceParam("Dot Grid", new Item[]{
        new Item("Triangle", HalftoneFilter.GRID_TRIANGLE),
        new Item("Square", HalftoneFilter.GRID_SQUARE),
        new Item("Rings", HalftoneFilter.GRID_RINGS),
    });

    private final ImagePositionParam center = new ImagePositionParam("Rings Center");

    private final BooleanParam monochrome = new BooleanParam("Monochrome", true);
    private final BooleanParam invert = new BooleanParam("Invert Pattern");
    private final RangeParam softness = new RangeParam("Softness", 0, 10, 100);

    public JHDotsHalftone() {
        super(true);

        // enable the center selector only if the rings grid is selected
        gridParam.setupEnableOtherIf(center, item -> item.valueIs(HalftoneFilter.GRID_RINGS));

        initParams(
            dotRadius,
            shapeParam,
            gridParam,
            center,
            softness,
            monochrome,
            invert
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        BufferedImage thresholdMask = createMaskImage(src);

        if (DEBUG_MASK) {
            Graphics2D g = dest.createGraphics();
            g.drawImage(thresholdMask, 0, 0, null);
            g.dispose();
            return dest;
        }

        var filter = new HalftoneFilter(getName());
        filter.setMask(thresholdMask);
        filter.setMonochrome(monochrome.isChecked());
        filter.setSoftness((float) softness.getPercentage());
        filter.setInvert(invert.isChecked());
        filter.setGridType(gridParam.getValue());
        filter.setCenter(center.getAbsolutePoint(src));

        return filter.filter(src, dest);
    }

    /**
     * Creates a mask image from the clustered dot matrix.
     */
    private BufferedImage createMaskImage(BufferedImage src) {
        int maskSize = 2 * dotRadius.getValue();
        int[][] matrix = genClusteredDotMatrix(maskSize, shapeParam.getValue());
        BufferedImage maskImage = ImageUtils.createImageWithSameCM(src, maskSize, maskSize);

        int[] maskPixels = ImageUtils.getPixels(maskImage);
        for (int y = 0; y < maskSize; y++) {
            for (int x = 0; x < maskSize; x++) {
                int threshold = matrix[x][y];
                maskPixels[x + y * maskSize] = 0xFF_00_00_00 | threshold << 16 | threshold << 8 | threshold;
            }
        }

        return maskImage;
    }

    /**
     * Creates a square matrix where dots are clustered together
     * to represent different intensity thresholds (priority orders).
     * As brightness increases, dots will appear in that order,
     * expanding outward, and growing into recognizable shapes.
     */
    private static int[][] genClusteredDotMatrix(int matrixSize, int shape) {
        assert matrixSize % 2 == 0 : "matrixSize = " + matrixSize;

        // stores a pixel's coordinates and its distance from the center
        record MPoint(int x, int y, double dist) {
        }

        int[][] matrix = new int[matrixSize][matrixSize];
        List<MPoint> points = new ArrayList<>();

        int centerX = matrixSize / 2;
        int centerY = matrixSize / 2;

        for (int y = 0; y < matrixSize; y++) {
            for (int x = 0; x < matrixSize; x++) {
                double d = distanceToCenter(shape, x - centerX, y - centerY);
                points.add(new MPoint(x, y, d));
            }
        }

        // sort the points by the distance to the center of the shape
        points.sort(Comparator.comparingDouble(p -> p.dist));

        // assign 0-255 threshold values
        int total = matrixSize * matrixSize;
        for (int i = 0; i < total; i++) {
            MPoint p = points.get(i);
            matrix[p.x][p.y] = (int) Math.round((double) i / total * 255);
        }

        return matrix;
    }

    private static double distanceToCenter(int shape, double dx, double dy) {
        return switch (shape) {
            case SHAPE_CIRCLE -> Math.hypot(dx, dy);
            case SHAPE_SQUARE -> Math.max(Math.abs(dx), Math.abs(dy));
            case SHAPE_DIAMOND -> Math.abs(dx) + Math.abs(dy);
            case SHAPE_CROSS -> Math.min(Math.abs(dx), Math.abs(dy));
            case SHAPE_X -> {
                double distanceToFirstDiagonal = Math.abs(dx - dy) / SQRT_2;  // distance to y = x
                double distanceToSecondDiagonal = Math.abs(dx + dy) / SQRT_2; // distance to y = -x
                yield Math.min(distanceToFirstDiagonal, distanceToSecondDiagonal);
            }
            case SHAPE_TRIANGLE -> { // equilateral triangle pointing upwards
                // distances to the triangle’s edges
                double dist1 = (SQRT_3 * dx - dy) / 2;
                double dist2 = (-SQRT_3 * dx - dy) / 2;
                double dist3 = dy;

                double scale = 2.0 / SQRT_3; // scaling factor for unit triangle
                yield Math.max(Math.max(dist1, dist2), dist3) / scale;
            }
            case SHAPE_HEXAGON -> {
                // uses axial coordinates to calculate distance from the center of a regular hexagon
                double q = (SQRT_3 / 3 * dx - 1.0 / 3 * dy);
                double r = (2.0 / 3 * dy);
                double s = -q - r;
                yield (Math.abs(q) + Math.abs(r) + Math.abs(s)) / 2;
            }
            case SHAPE_OCTAGON -> {
                double absX = Math.abs(dx);
                double absY = Math.abs(dy);
                yield Math.max(absX, absY) + (SQRT_2 - 1) * Math.min(absX, absY);
            }
            case SHAPE_STAR -> {
                // uses polar coordinates to create a 5-pointed star
                double angle = Math.atan2(dy, dx) + 3 * Math.PI / 2; // orient the star to point up
                double radius = Math.hypot(dx, dy);
                // modulates the radius based on the angle to form the star's points
                double modifiedRadius = radius * (1 + 0.25 * Math.cos(5 * angle));
                yield modifiedRadius;
            }
            default -> throw new IllegalArgumentException("Invalid shape: " + shape);
        };
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
