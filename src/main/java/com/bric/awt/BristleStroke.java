/*
 * @(#)BristleStroke.java
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

import com.bric.geom.MeasuredShape;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;

import static net.jafama.FastMath.cosQuick;
import static net.jafama.FastMath.sinQuick;

/**
 * This <code>Stroke</code> resembles a bristle.
 * <P>More specifically: this stroke splatters tiny triangles and dots over a path.
 */
public class BristleStroke implements Stroke {
    private final static int LAYERS = 20;

    private final float width;
    private final float thickness;
    private final long randomSeed;
    private final float grain;
    private final float density;

    /**
     * Creates a new BristleStroke.
     * <P>This constructor always uses a random seed of zero.
     *
     * @param width     the width (in pixels) of this stroke.
     * @param thickness a float between zero and one indicating how
     *                  "thick" this stroke should be.  (1 = "very thick", and 0 = "very thin")
     */
    public BristleStroke(float width, float thickness) {
        this(width, thickness, 0);
    }

    /**
     * Creates a new BristleStroke.
     *
     * @param width      the width (in pixels) of this stroke.
     * @param thickness  a float between zero and one indicating how
     *                   "thick" this stroke should be.  (1 = "very thick", and 0 = "very thin")
     * @param randomSeed the random seed for this stroke.
     */
    public BristleStroke(float width, float thickness, long randomSeed) {
        if (!(width > 0) || !Float.isFinite(width)) {
            throw new IllegalArgumentException("the width (" + width + ") must be positive and finite");
        }
        if (thickness < 0 || thickness > 1) {
            throw new IllegalArgumentException("the thickness (" + thickness + ") must be between 0 and 1");
        }
        this.width = width;
        this.thickness = thickness;
        this.grain = calcGrain(width, thickness);
        this.density = 0.5f + 0.5f * thickness;
        this.randomSeed = randomSeed;
    }

    private static float calcGrain(float width, float thickness) {
        if (width > 1) {
            double k = Math.sqrt(width);
            if (k > 4) {
                k = 4;
            }
            return (float) (k * (0.75 + 0.25 * thickness));
        } else {
            return Math.max(width, 0.1f);
        }
    }

    /**
     * @return the thickness of this stroke (a float between zero and one).
     */
    public float getThickness() {
        return thickness;
    }

    /**
     * @return the width (in pixels) of this stroke.
     */
    public float getWidth() {
        return width;
    }

    @Override
    public Shape createStrokedShape(Shape p) {
        GeneralPath path = new GeneralPath();
        Random r = new Random(randomSeed);

        MeasuredShape[] paths = MeasuredShape.getSubpaths(p);

        float minGapDistance = (4 + 10 * thickness) / (1 + 9 * density);
        float maxGapDistance = (40 + 10 * thickness) / (1 + 9 * density);

        Point2D p2 = new Point2D.Float();
        Point2D normal = new Point2D.Float();
        int[] segCursor = new int[1];

        for (int layer = 0; layer < LAYERS; layer++) {
            float k1 = ((float) layer) / ((float) (LAYERS - 1));
            float k2 = (k1 - 0.5f) * 2; // range from [-1, 1]
            float layerMinGap = minGapDistance * (1 + 20 * (1 - thickness) * (k2 * k2));
            float offsetScale = k2 * width * 0.5f;

            for (int subpathIndex = 0; subpathIndex < paths.length; subpathIndex++) {
                r.setSeed(randomSeed + 1000L * layer + 1000L * LAYERS * subpathIndex); // 1000*LAYERS exceeds the largest 1000*layer
                segCursor[0] = 0;

                float d = r.nextFloat() * (maxGapDistance - minGapDistance) + layerMinGap;
                float origDist = paths[subpathIndex].getOriginalDistance();
                while (d < origDist) {
                    float gapDistance = r.nextFloat() * (maxGapDistance - minGapDistance) + layerMinGap;
                    paths[subpathIndex].getPointAndNormal(d, segCursor, p2, normal);

                    float x = (float) (p2.getX() + normal.getX() * offsetScale);
                    float y = (float) (p2.getY() + normal.getY() * offsetScale);

                    float rotation = (float) (r.nextFloat() * Math.TAU);

                    float rHalf = grain * 0.5f;
                    float ux = (float) (rHalf * cosQuick(rotation));
                    float uy = (float) (rHalf * sinQuick(rotation));

                    if (r.nextInt(2) == 0) {
                        appendTriangle(path, x, y, ux, uy);
                    } else {
                        appendDiamond(path, x, y, ux, uy);
                    }

                    d = d + gapDistance;
                }
            }
        }
        return path;
    }

    /**
     * Appends an equilateral triangle centered at {@code (x, y)}.
     * The orientation is counter-clockwise so no holes appear under {@code WIND_NON_ZERO}.
     */
    private static void appendTriangle(GeneralPath path, float x, float y, float ux, float uy) {
        // Triangle: cos(120°) = -0.5, sin(120°) = √3/2 ≈ 0.8660254
        float hx = 0.8660254f * ux;
        float hy = 0.8660254f * uy;

        path.moveTo(x - 0.5f * ux - hy, y - 0.5f * uy + hx); // rotation + TAU/3
        path.lineTo(x - 0.5f * ux + hy, y - 0.5f * uy - hx); // rotation + 4*PI/3
        path.lineTo(x + ux, y + uy);                         // rotation
        path.closePath();
    }

    /**
     * Appends a diamond (rotated square) centered at {@code (x, y)}.
     * The orientation is counter-clockwise so no holes appear under {@code WIND_NON_ZERO}.
     */
    private static void appendDiamond(GeneralPath path, float x, float y, float ux, float uy) {
        // Square: 90° rotations are simply (-uy, ux)
        path.moveTo(x - uy, y + ux); // rotation + TAU/4
        path.lineTo(x - ux, y - uy); // rotation + PI
        path.lineTo(x + uy, y - ux); // rotation + 3*PI/2
        path.lineTo(x + ux, y + uy); // rotation
        path.closePath();
    }
}
