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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.HalftoneFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.utils.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.DoubleBinaryOperator;

import static com.jhlabs.image.ImageMath.SQRT_2;
import static com.jhlabs.image.ImageMath.SQRT_3;

/**
 * A halftone filter that applies a clustered-dot halftoning effect.
 * It procedurally generates a small, shaped dot matrix mask which is
 * then tiled across the image using the chosen grid layout.
 */
public class JHDotsHalftone extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "Dots Halftone";

    public enum DotShape {
        CIRCLE("Circle", Math::hypot),
        SQUARE("Square", (dx, dy) -> Math.max(Math.abs(dx), Math.abs(dy))),
        DIAMOND("Diamond", (dx, dy) -> Math.abs(dx) + Math.abs(dy)),
        CROSS("Cross", (dx, dy) -> Math.min(Math.abs(dx), Math.abs(dy))),
        X("X", (dx, dy) -> {
            double distanceToFirstDiagonal = Math.abs(dx - dy);  // distance to y = x
            double distanceToSecondDiagonal = Math.abs(dx + dy); // distance to y = -x
            return Math.min(distanceToFirstDiagonal, distanceToSecondDiagonal);
        }),
        TRIANGLE("Triangle", (dx, dy) -> {
            // equilateral triangle pointing upwards
            // distances to the triangle’s edges
            double dist1 = (SQRT_3 * dx - dy) / 2;
            double dist2 = (-SQRT_3 * dx - dy) / 2;
            double dist3 = dy;

            return Math.max(Math.max(dist1, dist2), dist3);
        }),
        HEXAGON("Hexagon", (dx, dy) -> {
            // uses axial coordinates to calculate distance from the center of a regular hexagon
            double q = (SQRT_3 / 3 * dx - 1.0 / 3 * dy);
            double r = (2.0 / 3 * dy);
            double s = -q - r;
            return Math.abs(q) + Math.abs(r) + Math.abs(s);
        }),
        OCTAGON("Octagon", (dx, dy) -> {
            double absX = Math.abs(dx);
            double absY = Math.abs(dy);
            return Math.max(absX, absY) + (SQRT_2 - 1) * Math.min(absX, absY);
        }),
        STAR("Star", (dx, dy) -> {
            // uses polar coordinates to create a 5-pointed star
            double angle = Math.atan2(dy, dx) + 3 * Math.PI / 2; // orient the star to point up
            double radius = Math.hypot(dx, dy);
            // modulates the radius based on the angle to form the star's points
            return radius * (1 + 0.25 * Math.cos(5 * angle));
        });

        private final String displayName;
        private final DoubleBinaryOperator distFunc;

        DotShape(String displayName, DoubleBinaryOperator distFunc) {
            this.displayName = displayName;
            this.distFunc = distFunc;
        }

        public double distance(double dx, double dy) {
            return distFunc.applyAsDouble(dx, dy);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final boolean DEBUG_MASK = false;

    private final RangeParam dotRadius = new RangeParam("Dot Radius", 1, 10, 100);
    private final EnumParam<DotShape> dotShape = new EnumParam<>("Dot Shape", DotShape.class);
    private final BooleanParam hollowDots = new BooleanParam("Hollow Dots");

    private final IntChoiceParam dotGrid = new IntChoiceParam("Dot Grid", new Item[]{
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
        dotGrid.enableOtherWhen(center, item -> item.hasValue(HalftoneFilter.GRID_RINGS));

        initParams(
            dotRadius,
            dotShape,
            hollowDots,
            dotGrid,
            center,
            softness,
            monochrome,
            invert
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        BufferedImage thresholdMask = createMaskImage();

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
        filter.setGridType(dotGrid.getValue());
        filter.setCenter(center.getAbsolutePoint(src));

        return filter.filter(src, dest);
    }

    /**
     * Creates a mask image where dots are clustered together
     * to represent different intensity thresholds (priority orders).
     * As brightness increases, dots will appear in that order,
     * expanding outward (or inward and outward if hollow),
     * and growing into recognizable shapes.
     */
    private BufferedImage createMaskImage() {
        int maskSize = 2 * dotRadius.getValue();

        int total = maskSize * maskSize;
        DotShape shape = dotShape.getValue();
        double center = (maskSize - 1) / 2.0;
        boolean hollow = hollowDots.isChecked();
        double targetRadius = dotRadius.getValue() / 2.0;

        // binds a pixel's index to its distance metric
        record PointDist(int pixelIndex, double dist) {
        }

        PointDist[] points = new PointDist[total];

        int idx = 0;
        for (int y = 0; y < maskSize; y++) {
            double dy = y - center;
            for (int x = 0; x < maskSize; x++) {
                double dx = x - center;
                double dist = shape.distance(dx, dy);
                if (hollow) {
                    dist = Math.abs(dist - targetRadius);
                }
                points[idx] = new PointDist(idx, dist);
                idx++;
            }
        }

        // sort the points by priority order
        Arrays.sort(points, Comparator.comparingDouble(PointDist::dist));

        BufferedImage maskImage = new BufferedImage(maskSize, maskSize, BufferedImage.TYPE_INT_ARGB);
        int[] maskPixels = ImageUtils.getPixels(maskImage);

        // assign 0-255 threshold values
        for (int i = 0; i < total; i++) {
            int threshold = (int) Math.round((double) i / total * 255);
            int rgb = 0xFF_00_00_00 | (threshold << 16) | (threshold << 8) | threshold;
            maskPixels[points[i].pixelIndex] = rgb;
        }

        return maskImage;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
