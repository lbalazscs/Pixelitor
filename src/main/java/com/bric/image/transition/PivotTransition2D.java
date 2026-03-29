/*
 * @(#)PivotTransition2D.java
 *
 * $Date: 2014-06-06 20:04:49 +0200 (P, 06 jún. 2014) $
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

import java.awt.Dimension;
import java.awt.geom.AffineTransform;

import static java.lang.Math.PI;

/**
 * This pivots a frame in/out from a specific corner, as if there
 * is a hinge involved.
 */
public class PivotTransition2D extends Transition2D {
    private final boolean in;
    private final int type;

    /**
     * Creates a new PivotTransition2D.
     *
     * @param type must be TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT or BOTTOM_RIGHT
     * @param in   whether the incoming frame is pivoting in on top of the old frame, or
     *             whether the old frame is pivoting out revealing the new frame.
     */
    public PivotTransition2D(int type, boolean in) {
        if (!(type == TOP_LEFT || type == TOP_RIGHT || type == BOTTOM_LEFT || type == BOTTOM_RIGHT)) {
            throw new IllegalArgumentException("Type must be TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT or BOTTOM_RIGHT");
        }
        this.type = type;
        this.in = in;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {
        AffineTransform transform;
        if (in) {
            transform = switch (type) {
                case TOP_LEFT -> AffineTransform.getRotateInstance(
                    (float) (-(1 - progress) * PI / 2.0f), 0, 0);
                case TOP_RIGHT -> AffineTransform.getRotateInstance(
                    (float) ((1 - progress) * PI / 2.0f), size.width, 0);
                case BOTTOM_LEFT -> AffineTransform.getRotateInstance(
                    (float) ((1 - progress) * PI / 2.0f), 0, size.height);
                default -> AffineTransform.getRotateInstance(
                    (float) ((1 - progress) * PI / 2.0f), size.width, size.height);
            };
            return new Transition2DInstruction[]{
                    new ImageInstruction(true),
                    new ImageInstruction(false, transform, null)
            };
        }

        //pivot out:
        transform = switch (type) {
            case TOP_LEFT -> AffineTransform.getRotateInstance(
                (float) (progress * PI / 2.0f), 0, 0);
            case TOP_RIGHT -> AffineTransform.getRotateInstance(
                (float) (-progress * PI / 2.0f), size.width, 0);
            case BOTTOM_LEFT -> AffineTransform.getRotateInstance(
                (float) (-progress * PI / 2.0f), 0, size.height);
            default -> AffineTransform.getRotateInstance(
                (float) (-progress * PI / 2.0f), size.width, size.height);
        };
        return new Transition2DInstruction[]{
                new ImageInstruction(false),
                new ImageInstruction(true, transform, null)
        };
    }

    @Override
    public String toString() {
        String s;
        if (in) {
            s = "Pivot In ";
        } else {
            s = "Pivot Out ";
        }
        if (type == TOP_LEFT) {
            return s + "Top Left";
        } else if (type == TOP_RIGHT) {
            return s + "Top Right";
        } else if (type == BOTTOM_LEFT) {
            return s + "Bottom Left";
        } else {
            return s + "Bottom Right";
        }
    }

}
