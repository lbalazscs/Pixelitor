/*
 * @(#)TossTransition2D.java
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

import com.bric.geom.TransformUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static java.lang.Math.PI;

/**
 * In this transition the incoming frame is tossed into place,
 * and after a little bit of wobbling it settles down. Here are playback samples:
 * <p><table summary="Sample Animations of TossTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/TossTransition2D/TossLeft.gif" alt="Toss Left">
 * <p>Toss Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/TossTransition2D/TossRight.gif" alt="Toss Right">
 * <p>Toss Right
 * </td>
 * </tr></table>
 */
public class TossTransition2D extends Transition2D {
    /**
     * This public static method is used by the
     * {@link com.bric.image.transition.Transition2DDemoHelper}
     * class to create sample animations of this transition.
     *
     * @return the transitions that should be used to demonstrate this
     * transition.
     */
    public static Transition[] getDemoTransitions() {
        return new Transition[]{
                new TossTransition2D(LEFT),
                new TossTransition2D(RIGHT)
        };
    }

    private int type = RIGHT;

    /**
     * Creates a new toss transition that throws to the right
     */
    public TossTransition2D() {
        this(RIGHT);
    }

    /**
     * Creates a new toss transition.
     *
     * @param type must be LEFT or RIGHT.
     */
    public TossTransition2D(int type) {
        if (!(type == LEFT || type == RIGHT)) {
            throw new IllegalArgumentException("The transition must use RIGHT or LEFT");
        }
        this.type = type;
    }

    /**
     * Transitions from one AffineTransform to another.
     *
     * @param a               the initial AffineTransform
     * @param b               the final AffineTransform
     * @param progress        a float between zero and one, where zero
     *                        represents <code>a</code> and one represents <code>b</code>.
     *                        Values outside this range will not throw an exception, but they will
     *                        make some funky results.
     * @param createNewObject indicates whether a new AffineTransform
     *                        should be constructed, or if one of the arguments can be
     *                        used to store the results
     * @return a transform that is somehow between <code>a</code> and <code>b</code>.
     */


    /**
     *Move method refactoring, tween(): from TransformUtils to TossTransition2D
     */
    public static AffineTransform tween(AffineTransform a, AffineTransform b, float progress, boolean createNewObject) {
        AffineTransform dest = createNewObject ? new AffineTransform() : a;
        dest.setTransform(
                a.getScaleX() * (1 - progress) + b.getScaleX() * progress,
                a.getShearY() * (1 - progress) + b.getShearY() * progress,
                a.getShearX() * (1 - progress) + b.getShearX() * progress,
                a.getScaleY() * (1 - progress) + b.getScaleY() * progress,
                a.getTranslateX() * (1 - progress) + b.getTranslateX() * progress,
                a.getTranslateY() * (1 - progress) + b.getTranslateY() * progress);
        return dest;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {
        double angle1 = -15.0f * PI / 180.0f;
        double angle2 = 5.0f * PI / 180.0f;
        double angle3 = -2.0f * PI / 180.0f;
        AffineTransform flipped = TransformUtils.createAffineTransform(
                0, 0,
                size.width, 0,
                0, size.height,

                size.width, 0,
                0, 0,
                size.width, size.height
        );


        AffineTransform untouched = TransformUtils.createAffineTransform(
                0, size.height,
                size.width, size.height,
                0, 0,

                size.width * 0.8, size.height - 5 * size.height / 4.0,
                size.width * 0.8 + size.width, size.height - 5 * size.height / 4.0,
                size.width * 0.8, 0 - 5 * size.height / 4.0
        );
        if (type == RIGHT) {
            untouched.preConcatenate(flipped);
            untouched.concatenate(flipped);
        }

        Point2D p1 = new Point2D.Double(size.width, size.height);
        Point2D p2 = new Point2D.Double(0, 0);
        Point2D p3 = new Point2D.Double();
        Point2D p4 = new Point2D.Double();
        AffineTransform t1 = new AffineTransform();
        t1.setToRotation(angle1, 0, size.height);
        t1.transform(p1, p3);
        t1.transform(p2, p4);

        AffineTransform transform1 = TransformUtils.createAffineTransform(
                0, size.height,
                0 + size.width, size.height,
                0, 0,

                0, size.height,
                p3.getX(), p3.getY(),
                p4.getX(), p4.getY()
        );
        if (type == RIGHT) {
            transform1.preConcatenate(flipped);
            transform1.concatenate(flipped);
        }

        p1.setLocation(0, size.height);
        p2.setLocation(0, 0);
        t1.setToRotation(angle2, size.width, size.height);
        t1.transform(p1, p3);
        t1.transform(p2, p4);

        AffineTransform transform2 = TransformUtils.createAffineTransform(
                0, size.height,
                size.width, size.height,
                0, 0,

                p3.getX(), p3.getY(),
                size.width, size.height,
                p4.getX(), p4.getY()
        );
        if (type == RIGHT) {
            transform2.preConcatenate(flipped);
            transform2.concatenate(flipped);
        }

        p1.setLocation(size.width, size.height);
        p2.setLocation(0, 0);
        t1.setToRotation(angle3, 0, size.height);
        t1.transform(p1, p3);
        t1.transform(p2, p4);

        AffineTransform transform3 = TransformUtils.createAffineTransform(
                0, size.height,
                size.width, size.height,
                0, 0,

                0, size.height,
                p3.getX(), p3.getY(),
                p4.getX(), p4.getY()
        );
        if (type == RIGHT) {
            transform3.preConcatenate(flipped);
            transform3.concatenate(flipped);
        }

        AffineTransform transform;
        float cut1 = 0.35f;
        float cut2 = 0.65f;
        float cut3 = 0.85f;
        if (progress < cut1) {
            progress = progress / cut1;
            transform = tween(untouched, transform1, progress, true);
        } else if (progress < cut2) {
            AffineTransform identity = new AffineTransform();
            progress = (progress - cut1) / (cut2 - cut1);
            progress = 3.125f * progress * progress - 2.125f * progress;

            transform = tween(transform1, identity, progress, true);
        } else if (progress < cut3) {
            AffineTransform identity = new AffineTransform();
            progress = (progress - cut2) / (cut3 - cut2);
            progress = -4.8f * progress * progress + 4.8f * progress;

            transform = tween(identity, transform2, progress, true);
        } else {
            AffineTransform identity = new AffineTransform();
            progress = (progress - cut3) / (1 - cut3);

            progress = -4.8f * progress * progress + 4.8f * progress;

            transform = tween(identity, transform3, progress, true);
        }

        return new Transition2DInstruction[]{
                new ImageInstruction(true),
                new ImageInstruction(false, transform, null)
        };
    }

    @Override
    public String toString() {
        if (type == RIGHT) {
            return "Toss Right";
        }
        return "Toss Left";
    }

}
