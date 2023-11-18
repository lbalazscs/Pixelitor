/*
 * @(#)CharcoalEffect.java
 *
 * $Date: 2014-04-06 05:02:15 +0200 (V, 06 ápr. 2014) $
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

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;

import static java.lang.Math.PI;

/**
 * This applies a charcoal effect to a shape.
 * <p>
 * This basically takes a shape and applies several "cracks" of varying
 * depth at a fixed angle.
 * <p>
 * (The implementation is pretty simple, and there are a few interesting
 * code snippets commented out that change how this renders.)
 */
public class CharcoalEffect {
    private final PathWriter writer;
    private final int seed;
    private final float size;
    private final float angle;
    private final float maxDepth;

    /**
     * Creates a new <code>CharcoalEffect</code>.
     *
     * @param dest       the destination to write the new shape to.
     * @param size       the size of the cracks.  This is float from [0,1], where
     *                   "0" means "no crack depth" and "1" means "high depth".  The depth is
     *                   always relative to the <i>possible</i> depth.
     * @param angle      the angle of the cracks.
     * @param randomSeed the random seed.
     */
    public CharcoalEffect(PathWriter dest, float size, float angle, int randomSeed) {
        this(dest, size, angle, randomSeed, Float.MAX_VALUE);
    }

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
        this.angle = angle;
        this.maxDepth = maxDepth;
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
        GeneralPath path = new GeneralPath();
        GeneralPathWriter writer = new GeneralPathWriter(path);
        CharcoalEffect effect = new CharcoalEffect(writer, size, angle, randomSeed, maxDepth);
        effect.applyCharcoalEffect(shape);
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

    /**
     * Applied Rename Method Refactoring
     * Write to applyCharcoalEffect
     */
    public void applyCharcoalEffect(Shape s) {
        Random random = new Random(seed);

        Point2D center = new Point2D.Float();
        Point2D rightSide = new Point2D.Float();

        MeasuredShape[] m = MeasuredShape.getSubpaths(s, 0.05f);

        // Don't add a crack if this point is completely inside the guiding shape.
        // And don't trust shape.contains(x,y,width,height).  Although that is in
        // theory what we want, it doesn't always return correct results!
        // This test isn't quite the same, but gets us what we need.  And accurately:
        subpathIterator:
        for (MeasuredShape measuredShape : m) {
            float orig = measuredShape.getOriginalDistance();
            float total = measuredShape.getClosedDistance();

            writer.moveTo(measuredShape.getMoveToX(), measuredShape.getMoveToY());

            float distance = 0;
            float pendingGap = 0;
            while (distance < orig) {
                pendingGap += (0.05f + 0.95f * random.nextFloat()) * 20 * (0.05f + 0.95f * (1 - 0.9f * size));

                if (distance + pendingGap >= orig) {
                    //we're overflowing:
                    float remaining = orig - distance;
                    if (remaining > 2) {
                        measuredShape.writeShape(distance / total, remaining / total, writer, false);
                    } else {
                        writer.closePath();
                    }
                    continue subpathIterator;
                } else if (distance + pendingGap < orig) {
                    //see if we can add a crack here:

                    measuredShape.getPoint(distance + pendingGap, center);

                    // Don't add a crack if this point is completely inside the guiding shape.
                    // And don't trust shape.contains(x,y,width,height).  Although that is in
                    // theory what we want, it doesn't always return correct results!
                    // This test isn't quite the same, but gets us what we need.  And accurately:
                    boolean addCrack = !(s.contains(center.getX() - 0.5, center.getY() - 0.5) &&
                            s.contains(center.getX() + 0.5, center.getY() - 0.5) &&
                            s.contains(center.getX() - 0.5, center.getY() + 0.5) &&
                            s.contains(center.getX() + 0.5, center.getY() + 0.5));

                    if (addCrack) {
                        for (int mult = -1; mult <= 1; mult += 2) { //try both the clockwise and the counterclockwise side
                            float width = 0.05f;
                            //yes, you can also try this:
                            //float angle = m[a].getTangentSlope(distance+pendingGap);
                            //or
                            //float angle = m[a].getTangentSlope(distance+pendingGap)-(float)Math.PI/4;
                            double cos = FastMath.cosQuick(angle + mult * PI / 2);
                            double sin = FastMath.sinQuick(angle + mult * PI / 2);
                            while (s.contains(center.getX() + width * cos,
                                    center.getY() + width * sin) && width < maxDepth) {
                                width++;
                            }
                            //or to make something spikey, try:
                            //if(guide.contains(center.getX()+.1*FastMath.cos(angle-mult*Math.PI/2),
                            //		center.getY()+.1*FastMath.sin(angle-mult*Math.PI/2))) {
                            //	width = random.nextFloat()*8+1;
                            //}
                            if (width > 1) {
                                //width will be > 1 when we're on the correct side AND we have
                                //a fleshy material to cut into
                                float crackWidth, depth;

                                //now define a constant to multiply the depth of the crack by
                                //when width is 5, multiply by 1.  when width is 15, multiply by .5
                                float k = -0.05f * width + 1.25f;
                                if (k > 1) {
                                    k = 1; //cap at these values
                                }
                                if (k < 0.4f) {
                                    k = 0.4f;
                                }
                                depth = width * k * (0.5f + 0.5f * size) * (0.25f + 0.75f * random.nextFloat());

                                crackWidth = depth * depth / 150;
                                if (crackWidth < 1.0f) {
                                    crackWidth = 1.0f;
                                }
                                if (crackWidth > 2) {
                                    crackWidth = 2;
                                }

                                if (distance + pendingGap - crackWidth / 2 > 0 && distance + pendingGap + crackWidth / 2 < orig) {
                                    measuredShape.getPoint(distance + pendingGap + crackWidth / 2, rightSide);
                                    measuredShape
                                            .writeShape(distance / total, (pendingGap - crackWidth / 2) / total, writer, false);
                                    writer.lineTo(
                                            (float) (center.getX() + depth * cos),
                                            (float) (center.getY() + depth * sin)
                                    );
                                    writer.lineTo((float) rightSide.getX(), (float) rightSide.getY());

                                    distance += pendingGap + crackWidth / 2;
                                    pendingGap = 0;
                                }
                                break;
                            }
                        }
                    }
                }
            }
            writer.closePath();
        }
    }
}
