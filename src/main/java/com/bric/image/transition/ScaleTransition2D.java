/*
 * @(#)ScaleTransition2D.java
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

import com.bric.geom.RectangularTransform;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This zooms one frame in/out from the center. Here are playback samples:
 * <p><table summary="Sample Animations of ScaleTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/ScaleTransition2D/ScaleIn.gif" alt="Scale In">
 * <p>Scale In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/ScaleTransition2D/ScaleOut.gif" alt="Scale Out">
 * <p>Scale Out
 * </td>
 * </tr></table>
 */
public class ScaleTransition2D extends Transition2D {
    private final int type;

    /**
     * Creates a new ScaleTransition2D
     *
     * @param type must be IN or OUT
     */
    public ScaleTransition2D(int type) {
        if (!(type == IN || type == OUT)) {
            throw new IllegalArgumentException("type must be IN or OUT");
        }
        this.type = type;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {
        Point2D center = new Point2D.Float(size.width / 2.0f, size.height / 2.0f);

        AffineTransform transform;
        if (type == OUT) {
            progress = 1 - progress;
        }

        float w = size.width * progress;
        float h = size.height * progress;
        transform = RectangularTransform.create(
                new Rectangle2D.Float(0, 0, size.width, size.height),
                new Rectangle2D.Double(center.getX() - w / 2, center.getY() - h / 2, w, h));

        return new ImageInstruction[]{
                new ImageInstruction(type == IN),
                new ImageInstruction(type != IN, transform, null)
        };
    }

    @Override
    public String toString() {
        if (type == IN) {
            return "Scale In";
        } else {
            return "Scale Out";
        }
    }
}
