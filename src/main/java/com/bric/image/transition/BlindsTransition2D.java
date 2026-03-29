/*
 * @(#)BlindsTransition2D.java
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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Also known as "Venetian Blinds", this creates several horizontal/vertical
 * strips that grow in width/height respectively to reveal the new frame.
 */
public class BlindsTransition2D extends Transition2D {
    private final int type;
    private final int blinds;

    /**
     * Creates a new BlindsTransition2D with 10 blinds.
     *
     * @param type must be LEFT, RIGHT, UP or DOWN.
     */
    public BlindsTransition2D(int type) {
        this(type, 10);
    }

    /**
     * Creates a BlindsTransition2D.
     *
     * @param type           must be LEFT, RIGHT, UP or DOWN
     * @param numberOfBlinds the number of blinds.  Must be 4 or greater.
     */
    public BlindsTransition2D(int type, int numberOfBlinds) {
        if (!(type == LEFT || type == RIGHT || type == UP || type == DOWN)) {
            throw new IllegalArgumentException("The type must be LEFT, RIGHT, UP or DOWN");
        }
        if (numberOfBlinds < 1) {
            throw new IllegalArgumentException("The number of blinds (" + numberOfBlinds + ") must be greater than 0.");
        }
        this.type = type;
        blinds = numberOfBlinds;
    }

    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {
        List<Transition2DInstruction> v = new ArrayList<>();
        v.add(new ImageInstruction(type == RIGHT || type == DOWN));
        float k;
        if (type == LEFT || type == RIGHT) {
            k = ((float) size.width) / ((float) blinds);
        } else {
            k = ((float) size.height) / ((float) blinds);
        }
        for (int a = 0; a < blinds; a++) {
            Rectangle2D r = switch (type) {
                case DOWN -> new Rectangle2D.Float(0, a * k, size.width, progress * k);
                case UP -> new Rectangle2D.Float(0, a * k, size.width, k - progress * k);
                case RIGHT -> new Rectangle2D.Float(a * k, 0, progress * k, size.height);
                default -> new Rectangle2D.Float(a * k, 0, k - progress * k, size.height);
            };
            v.add(new ImageInstruction(type == UP || type == LEFT, null, r));
        }
        return v.toArray(new Transition2DInstruction[v.size()]);
    }

    @Override
    public String toString() {
        return switch (type) {
            case LEFT -> "Blinds Left (" + blinds + ")";
            case RIGHT -> "Blinds Right (" + blinds + ")";
            case UP -> "Blinds Up (" + blinds + ")";
            default -> "Blinds Down (" + blinds + ")";
        };
    }
}
