/*
 * @(#)CharcoalEffect.java
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */

package com.bric.awt;

import com.bric.geom.GeneralPathWriter;
import com.bric.geom.MeasuredShape;
import com.bric.geom.PathWriter;
import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;

import static java.lang.Math.PI;

/**
 * This applies a charcoal effect to a shape.
 * <p>
 * This takes a shape and applies several "cracks" of varying depth at a fixed angle.
 */
public class CharcoalEffect {
    private static final float BASE_GAP = 20f;
    private static final float GAP_MIN_FRACTION = 0.05f;
    private static final float GAP_RANGE_FRACTION = 0.95f;
    private static final float GAP_SIZE_FACTOR = 0.9f;
    private static final float MIN_TAIL_LENGTH = 2f;
    private static final float CRACK_WIDTH_DIVISOR = 150f;

    private static final float PROBE_OFFSET = 0.05f;
    private static final float INITIAL_STEP = 1.0f;
    private static final int BINARY_SEARCH_STEPS = 4;
    private static final float DEPTH_SCALE_SLOPE = -0.05f;
    private static final float DEPTH_SCALE_INTERCEPT = 1.25f;
    private static final float MIN_DEPTH_SCALE = 0.4f;
    private static final float MAX_DEPTH_SCALE = 1.0f;

    private final PathWriter writer;
    private final int seed;
    private final float size;
    private final float maxDepth;
    private final float gapSizeScale;
    private final double dirX;
    private final double dirY;

    /**
     * Creates a new <code>CharcoalEffect</code>.
     *
     * @param dest       the destination to write the new shape to.
     * @param size       the size of the cracks.  This is float from [0,1], where
     *                   "0" means "no crack depth" and "1" means "high depth".  The depth is
     *                   always relative to the <i>possible</i> depth.
     * @param angle      the angle of the cracks.
     * @param randomSeed the random seed.
     * @param maxDepth   this is the maximum crack depth.  If this is zero, then no
     *                   cracks will be added.  If this is 5, then cracks will be at most 5 pixels.
     *                   If you aren't sure what to make this value, use <code>Float.MAX_VALUE</code>.
     */
    public CharcoalEffect(PathWriter dest, float size, float angle, int randomSeed, float maxDepth) {
        if (size < 0 || size > 1) {
            throw new IllegalArgumentException("size (" + size + ") must be between 0 and 1.");
        }
        writer = dest;
        seed = randomSeed;
        this.size = size;
        this.maxDepth = maxDepth;
        gapSizeScale = BASE_GAP * (GAP_MIN_FRACTION + GAP_RANGE_FRACTION * (1 - GAP_SIZE_FACTOR * size));
        dirX = FastMath.cosQuick(angle - PI / 2);
        dirY = FastMath.sinQuick(angle - PI / 2);
    }

    /**
     * Applies the <code>CharcoalEffect</code> to a shape.
     *
     * @param shape      the shape to apply the effect to.
     * @param size       the size of the cracks.  This is float from [0,1], where
     *                   "0" means "no crack depth" and "1" means "high depth".  The depth is
     *                   always relative to the <i>possible</i> depth.
     * @param angle      the angle of the cracks.
     * @param randomSeed the random seed.
     * @param maxDepth   this is the maximum crack depth.  If this is zero, then no
     *                   cracks will be added.  If this is 5, then cracks will be at most 5 pixels.
     *                   If you aren't sure what to make this value, use <code>Float.MAX_VALUE</code>.
     * @return a new filtered path.
     */
    public static GeneralPath filter(Shape shape, float size, float angle, int randomSeed, float maxDepth) {
        GeneralPath path = new GeneralPath(shape.getPathIterator(null).getWindingRule());
        GeneralPathWriter writer = new GeneralPathWriter(path);
        CharcoalEffect effect = new CharcoalEffect(writer, size, angle, randomSeed, maxDepth);
        effect.write(shape);
        return path;
    }

    /**
     * Applies the <code>CharcoalEffect</code> to a shape.
     *
     * @param shape      the shape to apply the effect to.
     * @param size       the size of the cracks.  This is float from [0,1], where
     *                   "0" means "no crack depth" and "1" means "high depth".  The depth is
     *                   always relative to the <i>possible</i> depth.
     * @param angle      the angle of the cracks.
     * @param randomSeed the random seed.
     * @return a new filtered path.
     */
    public static GeneralPath filter(Shape shape, float size, float angle, int randomSeed) {
        return filter(shape, size, angle, randomSeed, Float.MAX_VALUE);
    }

    /**
     * Applies this effect to the shape provided.
     *
     * @param s the shape to write.
     */
    public void write(Shape s) {
        Random random = new Random(seed);

        Point2D center = new Point2D.Float();
        Point2D rightSide = new Point2D.Float();

        MeasuredShape[] m = MeasuredShape.getSubpaths(s);
        if (m.length == 0) {
            return;
        }

        // Shape.contains() is O(number of segments) for a Path2D, and it is called
        // several times per crack attempt, so the shape is indexed once up front.
        PointInShapeIndex inside = new PointInShapeIndex(s);

        for (MeasuredShape measuredShape : m) {
            float orig = measuredShape.getOriginalDistance();
            float total = measuredShape.getClosedDistance();

            writer.moveTo(measuredShape.getMoveToX(), measuredShape.getMoveToY());

            float distance = 0;
            float pendingGap = 0;
            int[] segCursor = new int[1];

            while (distance < orig) {
                pendingGap += (GAP_MIN_FRACTION + GAP_RANGE_FRACTION * random.nextFloat()) * gapSizeScale;

                if (distance + pendingGap >= orig) {
                    float remaining = orig - distance;
                    if (remaining > MIN_TAIL_LENGTH) {
                        measuredShape.writeShape(distance / total, remaining / total, writer, false);
                    }
                    break;
                }

                float halfWidth = tryAddCrack(inside, measuredShape, orig, total,
                    distance, pendingGap, segCursor, center, rightSide, random);
                if (halfWidth > 0) {
                    distance += pendingGap + halfWidth;
                    pendingGap = 0;
                }
            }
            writer.closePath();
        }
    }

    /**
     * Attempts to add a crack at the current pending position.
     *
     * @return the crack half-width if a crack was written, or 0 if no crack could be written.
     */
    private float tryAddCrack(PointInShapeIndex inside, MeasuredShape measuredShape,
                              float orig, float total, float distance, float pendingGap,
                              int[] segCursor, Point2D center, Point2D rightSide, Random random) {
        measuredShape.getPoint(distance + pendingGap, segCursor, center);

        if (isFullyInside(inside, center)) {
            return 0f;
        }

        for (int side = 1; side >= -1; side -= 2) {
            double cos = side * dirX;
            double sin = side * dirY;

            float availableDepth = findAvailableDepth(inside, center, cos, sin);

            if (availableDepth > 1) {
                // availableDepth will be > 1 when we're on the correct side AND we have material to cut into
                float depthScale = Math.clamp(DEPTH_SCALE_SLOPE * availableDepth + DEPTH_SCALE_INTERCEPT,
                    MIN_DEPTH_SCALE, MAX_DEPTH_SCALE);
                float depth = availableDepth * depthScale * (0.5f + 0.5f * size) * (0.25f + 0.75f * random.nextFloat());
                float crackWidth = Math.clamp(depth * depth / CRACK_WIDTH_DIVISOR, 1.0f, 2.0f);

                float halfWidth = crackWidth / 2;
                if (pendingGap > halfWidth && distance + pendingGap + halfWidth < orig) {
                    measuredShape.getPoint(distance + pendingGap + halfWidth, segCursor, rightSide);
                    measuredShape.writeShape(distance / total,
                        (pendingGap - halfWidth) / total, writer, false);
                    writer.lineTo(
                        (float) (center.getX() + depth * cos),
                        (float) (center.getY() + depth * sin)
                    );
                    writer.lineTo((float) rightSide.getX(), (float) rightSide.getY());

                    return halfWidth;
                }
                break;
            }
        }
        return 0f;
    }

    /**
     * Checks if a point is completely inside the guiding shape by testing the 4 corners
     * of a 1x1 pixel neighborhood centered at (x, y).
     * <p>
     * {@link Shape#contains(double, double, double, double)} does not always return correct results
     * for arbitrary shapes, so this 4-corner test is used instead.
     */
    private static boolean isFullyInside(PointInShapeIndex inside, double x, double y) {
        return inside.contains(x - 0.5, y - 0.5)
            && inside.contains(x + 0.5, y - 0.5)
            && inside.contains(x - 0.5, y + 0.5)
            && inside.contains(x + 0.5, y + 0.5);
    }

    private static boolean isFullyInside(PointInShapeIndex inside, Point2D center) {
        return isFullyInside(inside, center.getX(), center.getY());
    }

    private float findAvailableDepth(PointInShapeIndex inside, Point2D center, double cos, double sin) {
        return findAvailableDepth(inside, center.getX(), center.getY(), cos, sin, maxDepth);
    }

    private static float findAvailableDepth(PointInShapeIndex inside, double cx, double cy, double cos, double sin, float maxDepth) {
        float availableDepth = PROBE_OFFSET;
        double startX = cx + availableDepth * cos;
        double startY = cy + availableDepth * sin;

        if (inside.contains(startX, startY)) {
            // Exponential bracket search [low, high]
            float low = PROBE_OFFSET;
            float step = INITIAL_STEP;
            float high = low + step;
            while (high < maxDepth) {
                double px = cx + high * cos;
                double py = cy + high * sin;
                if (!inside.contains(px, py)) {
                    break;
                }
                low = high;
                step *= 2.0f;
                high = Math.min(maxDepth, high + step);
            }

            // Binary search between low (inside) and high (outside)
            for (int iter = 0; iter < BINARY_SEARCH_STEPS; iter++) {
                float mid = (low + high) * 0.5f;
                double px = cx + mid * cos;
                double py = cy + mid * sin;
                if (inside.contains(px, py)) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            availableDepth = low;
        }
        return availableDepth;
    }
}
