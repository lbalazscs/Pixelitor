/*
 * @(#)AbstractShapeTransition2D.java
 *
 * $Date: 2014-03-14 07:36:22 +0100 (P, 14 márc. 2014) $
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
package com.bric.image.transition;

import com.bric.geom.ShapeBounds;

import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * This takes any abstract silhouette and will zoom it in/out to reveal
 * the new frame.
 *
 * <P>This class is actually a little bit clever, because it will study
 * exactly to what proportion it has to zoom the silhouette to completely
 * cover the frame size.
 */
public abstract class AbstractShapeTransition2D extends Transition2D {
    private final int type;

    // the box transition can look nicer without anti-aliasing
    private final boolean antiAliasing;

    /**
     * Creates a new AbstractShapeTransition2D that zooms out
     */
    protected AbstractShapeTransition2D(boolean antiAliasing) {
        this(OUT, antiAliasing);
    }

    /**
     * Creates a new AbstractShapeTransition2D
     *
     * @param type         must be IN or OUT.
     *                     <P>This indicates whether the shape grows or shrinks as this
     *                     transition progresses.
     * @param antiAliasing
     */
    protected AbstractShapeTransition2D(int type, boolean antiAliasing) {
        this.antiAliasing = antiAliasing;
        if (!(type == IN || type == OUT)) {
            throw new IllegalArgumentException("Type must be IN or OUT.");
        }
        this.type = type;
    }

    protected abstract Shape getShape();

    private final Map<Dimension, Number> multipliers = new HashMap<>();

    /**
     * Calculating the scaling ratio for the shape to fit the dimensions provided.
     */
    private float calculateMultiplier(Dimension size) {
        Shape shape = getShape();
        Area base = new Area(shape);
        AffineTransform transform = new AffineTransform();
        Rectangle2D r = ShapeBounds.getBounds(base);
        transform.translate(size.width / 2.0f - r.getCenterX(), size.height / 2.0f - r.getCenterY());
        base.transform(transform);
//        r = ShapeBounds.getBounds(base, r);
        float min = 0;
        float max = 1;
        Rectangle2D boundsRect = new Rectangle2D.Float(0, 0, size.width, size.height);
        while (!isOK(base, boundsRect, max)) {
            min = max;
            max *= 1.2f;
        }
        float f = calculateMultiplier(base, boundsRect, min, max);
        isOK(base, boundsRect, f);
        return f;
    }

    /**
     * Perform a binary search for the best-fitting multiplier to use
     */
    private static float calculateMultiplier(Area shape, Rectangle2D bounds, float min, float max) {
        while (true) {
            if (max - min < 0.5) {
                return max;
            }

            float middle = (min + max) / 2.0f;
            if (isOK(shape, bounds, middle)) {
                max = middle;
            } else {
                min = middle;
            }
        }
    }

    /**
     * Determine if a particular scaling ratio works
     */
    private static boolean isOK(Area shape, Rectangle2D bounds, float ratio) {
        Area area = new Area(shape);
        area.transform(AffineTransform.getScaleInstance(ratio, ratio));
        Rectangle2D r = ShapeBounds.getBounds(area);
        area.transform(AffineTransform.getTranslateInstance(
                -r.getCenterX() + bounds.getCenterX(),
                -r.getCenterY() + bounds.getCenterY()));

        Area boundsArea = new Area(bounds);
        boundsArea.subtract(area);
        return boundsArea.isEmpty();
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress, Dimension size) {
        Number multiplier = multipliers.get(size);
        if (multiplier == null) {
            multiplier = calculateMultiplier(size);
            multipliers.put(size, multiplier);
        }

        if (type == IN) {
            progress = 1 - progress;
        }

        Shape clipping = getShape();
        Rectangle2D r = ShapeBounds.getBounds(clipping);

        AffineTransform transform = new AffineTransform();

        transform.setToIdentity();


        transform.translate(size.width / 2.0, size.height / 2.0);
        transform.scale(
            progress * multiplier.floatValue(),
            progress * multiplier.floatValue());
        transform.translate(-size.width / 2.0, -size.height / 2.0);

        transform.translate(
            -r.getCenterX() + size.width / 2.0f,
            -r.getCenterY() + size.height / 2.0f);

        clipping = transform.createTransformedShape(clipping);

        if (type == IN) {
            Area area = new Area(new Rectangle2D.Double(0, 0, size.width, size.height));
            area.subtract(new Area(clipping));
            clipping = area;
        }

        return new Transition2DInstruction[]{
            new ImageInstruction(true),
            new ImageInstruction(false, null, clipping, antiAliasing)
        };
    }

    protected abstract String getShapeName();

    @Override
    public String toString() {
        if (type == IN) {
            return getShapeName() + " In";
        }
        return getShapeName() + " Out";
    }
}
