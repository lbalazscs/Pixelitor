/*
 * @(#)BristleStroke.java
 *
 * $Date: 2014-03-13 09:15:48 +0100 (Cs, 13 márc. 2014) $
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
import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * This <code>Stroke</code> that resembles a bristle.
 * <P>More specifically: this stroke splatters tiny triangles and dots over a path.
 */
public class BristleStroke implements Stroke {
    private static final int SHAPE_TRIANGLE = 0;
    private static final int SHAPE_SQUARE = 1;
    private static final int SHAPE_STAR = 2;
    private static final int SHAPE_TRIANGLE_OR_SQUARE = 3;

    /**
     * (I experimented with a few different shapes, but
     * decided in the end that a simple mix of squares
     * and triangles is sufficient.)
     */
    private final int shape = SHAPE_TRIANGLE_OR_SQUARE;

    public final float width;
    public final float thickness;
    private final int layers;
    private final long randomSeed;
    private final float grain;
    private final float spacing;


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
        if (width <= 0) {
            throw new IllegalArgumentException("the width (" + width + ") must be positive");
        }
        if (thickness < 0) {
            throw new IllegalArgumentException("the thickness (" + thickness + ") must be greater than 0");
        }
        this.width = width;
        this.thickness = thickness;
        this.grain = getGrain(width, thickness);
        this.spacing = .5f + .5f * thickness;
        this.randomSeed = randomSeed;
        int l = (int) ((1 + 2 * thickness) * width) + 10;
        if (l > 20) {
            l = 20;
        }
        this.layers = 20;
    }

    private static float getGrain(float width, float thickness) {
        double k = width;
        if (width > 1) {
            k = Math.pow(width, .5f);
            if (k > 4) {
                k = 4;
            }
            return (float) (k * (.75 + .25 * thickness));
        } else {
            return
                    Math.max(width, .1f);
        }
    }

    /**
     * @return the random seed used in this stroke
     */
    public long getRandomSeed() {
        return randomSeed;
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

    public Shape createStrokedShape(Shape p) {
        GeneralPath path = new GeneralPath();
        Random r = new Random(randomSeed);

        MeasuredShape[] paths = MeasuredShape.getSubpaths(p);

        for (int a = 0; a < layers; a++) {
            float k1 = ((float) a) / ((float) (layers - 1));
            float k2 = (k1 - .5f) * 2; //range from [-1,1]


            float k3 = thickness;
            float minGapDistance = (4 + 10 * k3) / (1 + 9 * spacing);
            float maxGapDistance = (40 + 10 * k3) / (1 + 9 * spacing);

            Point2D p2 = new Point2D.Float();
            float x, y;

            for (int b = 0; b < paths.length; b++) {
                r.setSeed(randomSeed + 1000 * a + 10000 * b);

                float d = r.nextFloat() * (maxGapDistance - minGapDistance) + minGapDistance * (1 + 20 * (1 - thickness) * Math.abs(k2 * k2));
                while (d < paths[b].getOriginalDistance()) {
                    float gapDistance = r.nextFloat() * (maxGapDistance - minGapDistance) + minGapDistance * (1 + 20 * (1 - thickness) * Math.abs(k2 * k2));

                    paths[b].getPoint(d, p2);
                    float angle = paths[b].getTangentSlope(d);
                    float dx = (float) (k2 * width * FastMath.cos(angle + Math.PI / 2) / 2.0);
                    float dy = (float) (k2 * width * FastMath.sin(angle + Math.PI / 2) / 2.0);

                    p2.setLocation(p2.getX() + dx, p2.getY() + dy);

                    x = (float) p2.getX();
                    y = (float) p2.getY();

                    float rotation = r.nextFloat() * 2 * 3.145f;

                    int thisShape = shape;
                    if (thisShape == SHAPE_TRIANGLE_OR_SQUARE) {
                        thisShape = r.nextInt(2);
                    }

                    if (thisShape == SHAPE_TRIANGLE) {
                        path.moveTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 2 * Math.PI / 3)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 2 * Math.PI / 3)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 4 * Math.PI / 3)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 4 * Math.PI / 3)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation)));
                        path.closePath();
                    } else if (thisShape == SHAPE_SQUARE) {
                        path.moveTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 2 * Math.PI / 4)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 2 * Math.PI / 4)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 4 * Math.PI / 4)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 4 * Math.PI / 4)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 6 * Math.PI / 4)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 6 * Math.PI / 4)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation)));
                        path.closePath();
                    } else if (thisShape == SHAPE_STAR) {
                        path.moveTo((float) (x + grain / (6.0 + 2 - 2 * thickness) * FastMath.cos(rotation)),
                                (float) (y + grain / (6.0 + 2 - 2 * thickness) * FastMath.sin(rotation)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 2 * Math.PI / 8.0)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 2 * Math.PI / 8.0)));

                        path.lineTo((float) (x + grain / (6.0 + 2 - 2 * thickness) * FastMath.cos(rotation + Math.PI / 2)),
                                (float) (y + grain / (6.0 + 2 - 2 * thickness) * FastMath.sin(rotation + Math.PI / 2)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + Math.PI / 2 + 2 * Math.PI / 8.0)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + Math.PI / 2 + 2 * Math.PI / 8.0)));

                        path.lineTo((float) (x + grain / (6.0 + 2 - 2 * thickness) * FastMath.cos(rotation + Math.PI)),
                                (float) (y + grain / (6.0 + 2 - 2 * thickness) * FastMath.sin(rotation + Math.PI)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + Math.PI + 2 * Math.PI / 8.0)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + Math.PI + 2 * Math.PI / 8.0)));

                        path.lineTo((float) (x + grain / (6.0 + 2 - 2 * thickness) * FastMath.cos(rotation + 3 * Math.PI / 2)),
                                (float) (y + grain / (6.0 + 2 - 2 * thickness) * FastMath.sin(rotation + 3 * Math.PI / 2)));
                        path.lineTo((float) (x + grain / 2.0 * FastMath.cos(rotation + 3 * Math.PI / 2 + 2 * Math.PI / 8.0)),
                                (float) (y + grain / 2.0 * FastMath.sin(rotation + 3 * Math.PI / 2 + 2 * Math.PI / 8.0)));
                    }

                    d = d + gapDistance;
                }
            }
        }
        return path;
    }
}
