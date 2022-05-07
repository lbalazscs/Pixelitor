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

import net.jafama.FastMath;

import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.*;

/**
 * This creates a pattern of growing shapes.  (The new frame is clipped
 * to these shapes.)
 */
public class DiamondsTransition2D extends Transition2D {
    public static final int TYPE_DIAMOND = 0;
    public static final int TYPE_CIRCLE = 1;
    public static final int TYPE_SQUARE = 2;
    public static final int TYPE_TRIANGLE = 3;

    private final int shapeSize;
    private final int type;
    private double angle;

    /**
     * Creates a new DiamondsTransition2D with a diamond size of 50.
     */
    public DiamondsTransition2D() {
        this(50, TYPE_DIAMOND, 0);
    }

    /**
     * Creates a new DiamondsTransition2D.
     *
     * @param size the width of the diamonds.
     *             It is not recommended that this value is less than 40, as that
     *             can really hurt performance in some situations.
     */
    public DiamondsTransition2D(int size, int type, double angle) {
        this.type = type;
        this.angle = angle;
        if (size <= 0) {
            throw new IllegalArgumentException("size (" + size + ") must be greater than 4");
        }
        this.shapeSize = size;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress, Dimension size) {
        GeneralPath path = new GeneralPath(Path2D.WIND_NON_ZERO);

        float dx = size.width / 2.0f;
        float dy = size.height / 2.0f;
        while (dx > shapeSize) {
            dx -= shapeSize;
        }
        while (dy > shapeSize) {
            dy -= shapeSize;
        }

        // if the angle is not 0, then extra shapes have to be generated
        float extraX = 0.0f;
        float extraY = 0.0f;
        if (angle != 0) {
            double sin = Math.abs(FastMath.sin(angle));
            double cos = Math.abs(FastMath.cos(angle));

            double rotatedWidth = size.width * cos + size.height * sin;
            double rotatedHeight = size.height * cos + size.width * sin;

            extraX = (float)((rotatedWidth - size.width) / 2.0);
            extraY = (float)((rotatedHeight - size.height) / 2.0);

            // Make sure that the extras are multiples of the shape size,
            // so that it rotates around the center when the angle is increased.
            extraX -= (extraX % shapeSize);
            extraX += shapeSize;
            extraY -= (extraY % shapeSize);
            extraY += shapeSize;
        }

        int ctr = 0;
        float currentSize = shapeSize * progress;
        float currentHalfSize = currentSize / 2.0f;
        for (float y = -dy - extraY; y < size.height + shapeSize + extraY; y += shapeSize / 2.0f) {
            float polkaX = 0;
            if (ctr % 2 == 0) {
                polkaX = shapeSize / 2.0f;
            }

            for (float x = -dx - extraX; x < size.width + shapeSize + extraX; x += shapeSize) {
                float adjX = x + polkaX;
                switch (type) {
                    case TYPE_DIAMOND -> {
                        path.moveTo(adjX, y - currentHalfSize);
                        path.lineTo(adjX + currentHalfSize, y);
                        path.lineTo(adjX, y + currentHalfSize);
                        path.lineTo(adjX - currentHalfSize, y);
                        path.lineTo(adjX, y - currentHalfSize);
                    }
                    case TYPE_CIRCLE -> path.append(new Ellipse2D.Float(
                        adjX - currentHalfSize, y - currentHalfSize, currentSize, currentSize), false);
                    case TYPE_SQUARE -> path.append(new Rectangle2D.Float(
                        adjX - currentHalfSize, y - currentHalfSize, currentSize, currentSize), false);
                    case TYPE_TRIANGLE -> {
                        path.moveTo(adjX, y - currentHalfSize);
                        path.lineTo(adjX + currentSize, y + currentHalfSize);
                        path.lineTo(adjX - currentSize, y + currentHalfSize);
                    }
                    default -> throw new IllegalStateException("type = " + type);
                }
            }
            ctr++;
        }

        Shape clipShape = path;
        if (angle != 0) {
            double cx = size.width / 2.0;
            double cy = size.height / 2.0;
            AffineTransform rotation = AffineTransform.getRotateInstance(angle, cx, cy);
            clipShape = rotation.createTransformedShape(path);
        }

        return new Transition2DInstruction[]{
            new ImageInstruction(true),
            new ImageInstruction(false, null, clipShape)
        };
    }

    @Override
    public String toString() {
        return "Diamonds (" + shapeSize + ")";
    }
}
