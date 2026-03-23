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

package pixelitor.filters;

import pixelitor.filters.gui.*;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a concentric Voronoi diagram with optional distortion
 * (randomness, spiral, pinch/bulge) and renders it using image colors.
 */
public class RadialMosaic extends ParametrizedFilter {
    public static final String NAME = "Radial Mosaic";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int ARRANGEMENT_ALIGNED = 0;
    private static final int ARRANGEMENT_OFFSET = 1;

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam size = new RangeParam("Tile Size", 10, 40, 200);
    private final IntChoiceParam arrangementParam = new IntChoiceParam("Arrangement", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Aligned", ARRANGEMENT_ALIGNED),
        new IntChoiceParam.Item("Offset", ARRANGEMENT_OFFSET)
    });
    private final RangeParam randomnessParam = new RangeParam("Randomness", 0, 0, 100);
    private final RangeParam pinchBulgeParam = new RangeParam("Pinch-Bulge", -100, 0, 100, true, SliderSpinner.LabelPosition.NONE_WITH_TICKS);
    private final RangeParam spiralParam = new RangeParam("Spiral", -100, 0, 100, true, SliderSpinner.LabelPosition.NONE_WITH_TICKS);
    private final RangeParam edgeWidth = new RangeParam("Width", 0, 1, 10, true, SliderSpinner.LabelPosition.NONE_WITH_TICKS);
    private final ColorParam edgeColor = new ColorParam("Color", Color.BLACK);

    public RadialMosaic() {
        super(true);

        FilterButtonModel reseedAction = paramSet.createReseedAction("", "Reseed Randomness");

        // enable the reseed randomness button only if randomness > 0
        randomnessParam.setupEnableOtherIfNotZero(reseedAction);

        // enable edge color selection only if edge width > 0
        edgeWidth.setupEnableOtherIfNotZero(edgeColor);

        initParams(
            center,
            size,
            arrangementParam,
            randomnessParam.withSideButton(reseedAction),
            CompositeParam.border("Distortion", pinchBulgeParam, spiralParam),
            CompositeParam.border("Edge", edgeWidth, edgeColor)
        );
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int width = src.getWidth();
        int height = src.getHeight();

        // pre-compute the seeds and their neighbors
        List<SeedPoint> seeds = generateConcentricSeeds(width, height);
        ProgressTracker pt = new StatusBarProgressTracker(NAME, seeds.size());

        Graphics2D g = dest.createGraphics();
        try {
            VoronoiProcessor.render(seeds, g, src, pt,
                edgeWidth.getValue(), edgeColor.getColor());
        } finally {
            g.dispose();
            pt.finished();
        }

        return dest;
    }

    private List<SeedPoint> generateConcentricSeeds(int width, int height) {
        int distBetweenRings = size.getValue();
        double cx = center.getRelativeX() * width;
        double cy = center.getRelativeY() * height;
        int arrangement = arrangementParam.getValue();

        Random random = paramSet.getLastSeedRandom();
        double randomness = randomnessParam.getPercentage();
        double pinchBulge = pinchBulgeParam.getPercentage();
        double spiral = spiralParam.getPercentage();

        // calculate how many rings are needed to fully cover the canvas
        double maxDist = Math.hypot(Math.max(cx, width - cx), Math.max(cy, height - cy));
        int numRings = (int) Math.ceil(maxDist / distBetweenRings) + 1;

        int totalPoints = 1 + 3 * numRings * (numRings + 1);
        List<SeedPoint> allSeeds = new ArrayList<>(totalPoints);

        // jagged array because each ring contains a different number of points
        SeedPoint[][] rings = new SeedPoint[numRings][];

        // ensure the maximum offset scales with the distance to neighboring rings
        double centerMaxOffset = getWarpedRadius(0, distBetweenRings, maxDist, pinchBulge) * 0.4 * randomness;
        SeedPoint centerSeed = genCenterPoint(cx, cy, random, centerMaxOffset);
        allSeeds.add(centerSeed);

        genRingPoints(rings, distBetweenRings, arrangement, random, randomness, cx, cy, allSeeds, maxDist, pinchBulge, spiral, numRings);
        linkNeighbors(centerSeed, rings, numRings, arrangement, spiral);

        return allSeeds;
    }

    private static double getWarpedRadius(int r, int distBetweenRings, double maxDist, double pinchBulge) {
        double radius = (r + 1) * distBetweenRings;
        if (pinchBulge == 0) {
            return radius;
        }

        // use a power curve interpolation ensuring total canvas coverage
        double t = radius / maxDist;
        double exponent = Math.pow(2.0, -pinchBulge * 2.0);
        return Math.pow(t, exponent) * maxDist;
    }

    private static double getSpiralAngle(int r, int numRings, double spiral) {
        if (spiral == 0) {
            return 0;
        }
        // at ±1.0, the outermost ring completes a full 360-degree rotation
        return spiral * Math.PI * 2.0 * ((r + 1.0) / numRings);
    }

    private static SeedPoint genCenterPoint(double cx, double cy, Random random, double maxOffset) {
        double centerOffsetX = (random.nextDouble() * 2 - 1) * maxOffset;
        double centerOffsetY = (random.nextDouble() * 2 - 1) * maxOffset;
        return new SeedPoint(cx + centerOffsetX, cy + centerOffsetY);
    }

    private static void genRingPoints(SeedPoint[][] rings, int distBetweenRings, int arrangement,
                                      Random random, double randomness, double cx, double cy,
                                      List<SeedPoint> allSeeds, double maxDist, double pinchBulge,
                                      double spiral, int numRings) {
        for (int r = 0; r < rings.length; r++) {
            int pointsInRing = 6 * (r + 1);
            rings[r] = new SeedPoint[pointsInRing];

            double radius = getWarpedRadius(r, distBetweenRings, maxDist, pinchBulge);

            // compute local distances to prevent randomness crossing inner/outer ring bounds
            double prevRadius = r == 0 ? 0 : getWarpedRadius(r - 1, distBetweenRings, maxDist, pinchBulge);
            double nextRadius = getWarpedRadius(r + 1, distBetweenRings, maxDist, pinchBulge);

            double distIn = radius - prevRadius;
            double distOut = nextRadius - radius;
            double safeLocalDist = Math.min(distIn, distOut);
            double localMaxOffset = safeLocalDist * 0.4 * randomness;

            // offset every second ring by a half angle if requested
            double ringOffset = (arrangement == ARRANGEMENT_OFFSET && r % 2 != 0) ? Math.PI / pointsInRing : 0;
            double spiralAngle = getSpiralAngle(r, numRings, spiral);
            double angleBase = ringOffset + spiralAngle;

            for (int i = 0; i < pointsInRing; i++) {
                double angle = 2 * Math.PI * i / pointsInRing + angleBase;

                // add random displacement bounded to local distances
                double offsetX = (random.nextDouble() * 2 - 1) * localMaxOffset;
                double offsetY = (random.nextDouble() * 2 - 1) * localMaxOffset;

                double px = cx + radius * Math.cos(angle) + offsetX;
                double py = cy + radius * Math.sin(angle) + offsetY;

                SeedPoint p = new SeedPoint(px, py);
                rings[r][i] = p;
                allSeeds.add(p);
            }
        }
    }

    private static void linkNeighbors(SeedPoint centerSeed, SeedPoint[][] rings, int numRings, int arrangement, double spiral) {
        // center point cares about all innermost seeds
        for (int i = 0; i < 6; i++) {
            centerSeed.neighbors.add(rings[0][i]);
            rings[0][i].neighbors.add(centerSeed);
        }

        for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
            int pointsInRing = 6 * (ringIndex + 1);
            double currentOffset = (arrangement == ARRANGEMENT_OFFSET && ringIndex % 2 != 0) ? Math.PI / pointsInRing : 0;
            double currentSpiral = getSpiralAngle(ringIndex, numRings, spiral);
            double currentAngleBase = currentOffset + currentSpiral;

            // precompute outer ring alignment
            boolean hasOuter = (ringIndex < numRings - 1);
            int outerPoints = hasOuter ? 6 * (ringIndex + 2) : 0;
            double outerTotalOffset = 0;
            if (hasOuter) {
                double outerOffset = (arrangement == ARRANGEMENT_OFFSET && (ringIndex + 1) % 2 != 0) ? Math.PI / outerPoints : 0;
                double outerSpiral = getSpiralAngle(ringIndex + 1, numRings, spiral);
                outerTotalOffset = outerOffset + outerSpiral;
            }

            // precompute inner ring alignment
            boolean hasInner = (ringIndex > 0);
            int innerPoints = hasInner ? 6 * ringIndex : 0;
            double innerTotalOffset = 0;
            if (hasInner) {
                double innerOffset = (arrangement == ARRANGEMENT_OFFSET && (ringIndex - 1) % 2 != 0) ? Math.PI / innerPoints : 0;
                double innerSpiral = getSpiralAngle(ringIndex - 1, numRings, spiral);
                innerTotalOffset = innerOffset + innerSpiral;
            }

            for (int i = 0; i < pointsInRing; i++) {
                SeedPoint current = rings[ringIndex][i];
                double currentAngle = 2 * Math.PI * i / pointsInRing + currentAngleBase;

                // left and right on the same ring
                int leftIndex = Math.floorMod(i - 1, pointsInRing);
                int rightIndex = Math.floorMod(i + 1, pointsInRing);

                current.neighbors.add(rings[ringIndex][leftIndex]);
                current.neighbors.add(rings[ringIndex][rightIndex]);

                // ring above (outer)
                if (hasOuter) {
                    addNeighborsFromRing(current, rings[ringIndex + 1], currentAngle, outerPoints, outerTotalOffset);
                }

                // ring below (inner)
                if (hasInner) {
                    addNeighborsFromRing(current, rings[ringIndex - 1], currentAngle, innerPoints, innerTotalOffset);
                }
            }
        }
    }

    /**
     * Links the 3 closest points in an adjacent ring as neighbors
     * to guarantee proper Voronoi containment clipping.
     */
    private static void addNeighborsFromRing(SeedPoint current, SeedPoint[] targetRing,
                                             double currentAngle, int targetPoints, double targetOffset) {
        // Assumes angular ordering between rings is preserved despite distortion.
        // Large distortions reduce neighbor accuracy.
        double exactIndex = (currentAngle - targetOffset) * targetPoints / (2 * Math.PI);
        int baseIdx = Math.floorMod(Math.round(exactIndex), targetPoints);

        int n1 = Math.floorMod(baseIdx - 1, targetPoints);
        int n2 = baseIdx;
        int n3 = Math.floorMod(baseIdx + 1, targetPoints);

        current.neighbors.add(targetRing[n1]);
        current.neighbors.add(targetRing[n2]);
        current.neighbors.add(targetRing[n3]);
    }
}
