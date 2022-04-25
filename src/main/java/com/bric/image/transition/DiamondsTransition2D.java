/*
 * @(#)DiamondsTransition2D.java
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * This creates a pattern of growing shapes.  (The new frame is clipped
 * to these shapes.)
 */
public class DiamondsTransition2D extends Transition2D {
    public static int TYPE_DIAMOND = 0;
    public static int TYPE_CIRCLE = 1;
    public static int TYPE_SQUARE = 2;

    private final int diamondSize;
    private final int type;

    /**
     * Creates a new DiamondsTransition2D with a diamond size of 50.
     */
    public DiamondsTransition2D() {
        this(50, TYPE_DIAMOND);
    }

    /**
     * Creates a new DiamondsTransition2D.
     *
     * @param size the width of the diamonds.
     *             It is not recommended that this value is less than 40, as that
     *             can really hurt performance in some situations.
     */
    public DiamondsTransition2D(int size, int type) {
        this.type = type;
        if (size <= 0) {
            throw new IllegalArgumentException("size (" + size + ") must be greater than 4");
        }
        this.diamondSize = size;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {

        GeneralPath clipping = new GeneralPath(Path2D.WIND_NON_ZERO);

        float dx = size.width / 2.0f;
        float dy = size.height / 2.0f;
        while (dx > diamondSize) {
            dx -= diamondSize;
        }
        while (dy > diamondSize) {
            dy -= diamondSize;
        }

        int ctr = 0;
        float currentSize = diamondSize * progress;
        float currentHalfSize = currentSize / 2.0f;
        for (float y = -dy; y < size.height + diamondSize; y += diamondSize / 2.0f) {
            float polkaX = 0;
            if (ctr % 2 == 0) {
                polkaX = diamondSize / 2.0f;
            }

            for (float x = -dx; x < size.width + diamondSize; x += diamondSize) {
                if (type == TYPE_DIAMOND) {
                    clipping.moveTo(x + polkaX, y - currentHalfSize);
                    clipping.lineTo(x + currentHalfSize + polkaX, y);
                    clipping.lineTo(x + polkaX, y + currentHalfSize);
                    clipping.lineTo(x - currentHalfSize + polkaX, y);
                    clipping.lineTo(x + polkaX, y - currentHalfSize);
                    clipping.closePath();
                } else if (type == TYPE_CIRCLE) {
                    clipping.append(new Ellipse2D.Float(x - currentHalfSize + polkaX, y - currentHalfSize, currentSize, currentSize), false);
                } else { // square
                    clipping.append(new Rectangle2D.Float(x - currentHalfSize + polkaX, y - currentHalfSize, currentSize, currentSize), false);
                }
            }
            ctr++;
        }

        return new Transition2DInstruction[]{
            new ImageInstruction(true),
            new ImageInstruction(false, null, clipping)
        };
    }

    @Override
    public String toString() {
        return "Diamonds (" + diamondSize + ")";
    }
}
