/*
 * @(#)CheckerboardTransition2D.java
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

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

/**
 * This creates a checkerboard pattern that grows to reveal the new frame.
 */
public class CheckerboardTransition2D extends Transition2D {
    private final int type;
    private final int rowCount;
    private final int columnCount;

    /**
     * Creates a new CheckerboardTransition2D that moves right.
     */
    public CheckerboardTransition2D() {
        this(RIGHT, 20, 20);
    }

    /**
     * Creates a new CheckerboardTransition2D.
     *
     * @param type must be LEFT, RIGHT, UP or DOWN
     */
    public CheckerboardTransition2D(int type, int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        if (!(type == RIGHT || type == LEFT || type == UP || type == DOWN)) {
            throw new IllegalArgumentException("The type must be RIGHT, LEFT, UP or DOWN.");
        }
        this.type = type;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {

        GeneralPath clipping = new GeneralPath();

        if (type == RIGHT || type == LEFT) {
            float k = ((float) size.width) / columnCount * 2;
            float k2 = ((float) size.height) / rowCount;

            for (int row = 0; row < rowCount; row++) {
                float dx = 0;
                if (row % 2 == 0) {
                    dx = k / 2;
                }

                float startY = row * k2;
                float endY = startY + k2;
                for (int column = -1; column < columnCount; column++) {
                    float startX = column * k + dx;
                    float endX = column * k + k * progress + dx;
                    clipping.moveTo(startX, startY);
                    clipping.lineTo(startX, endY);
                    clipping.lineTo(endX, endY);
                    clipping.lineTo(endX, startY);
                    clipping.lineTo(startX, startY);
                    clipping.closePath();
                }
            }

            if (type == LEFT) {
                AffineTransform flip = TransformUtils.createAffineTransform(
                        0, 0,
                        size.width, 0,
                        0, size.height,
                        size.width, 0,
                        0, 0,
                        size.width, size.height
                );
                clipping.transform(flip);
            }
        } else { // up or down
            float k = ((float) size.height) / rowCount * 2;
            float k2 = ((float) size.width) / columnCount;

            for (int column = 0; column < columnCount; column++) {
                float dy = 0;
                if (column % 2 == 0) {
                    dy = k / 2;
                }

                float startX = column * k2;
                float endX = startX + k2;
                for (int row = -1; row < rowCount; row++) {
                    float startY = row * k + dy;
                    float endY = row * k + k * progress + dy;
                    clipping.moveTo(startX, startY);
                    clipping.lineTo(endX, startY);
                    clipping.lineTo(endX, endY);
                    clipping.lineTo(startX, endY);
                    clipping.lineTo(startX, startY);
                    clipping.closePath();
                }
            }

            if (type == UP) {
                AffineTransform flip = TransformUtils.createAffineTransform(
                        0, 0,
                        size.width, 0,
                        0, size.height,
                        0, size.height,
                        size.width, size.height,
                        0, 0
                );
                clipping.transform(flip);
            }
        }

        return new Transition2DInstruction[]{
                new ImageInstruction(true),
                new ImageInstruction(false, null, clipping)

        };
    }

    @Override
    public String toString() {
        return switch (type) {
            case RIGHT -> "Checkerboard Right";
            case LEFT -> "Checkerboard Left";
            case UP -> "Checkerboard Up";
            default -> "Checkerboard Down";
        };
    }
}
