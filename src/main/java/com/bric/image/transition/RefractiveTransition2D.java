/*
 * @(#)RefractiveTransition2D.java
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;

/**
 * This resembles a lens with several faces, showing several fractured
 * copies of the same image.  As the transition progresses the incoming image
 * becomes more focused and opaque. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/RefractiveTransition2D/Refractive.gif" alt="RefractiveTransition2D Demo">
 */
public class RefractiveTransition2D extends Transition2D {
    @Override
    public Transition2DInstruction[] getInstructions(float progress,
                                                     Dimension size) {
        List<Rectangle2D> v1 = new ArrayList<>();
        List<Transition2DInstruction> v2 = new ArrayList<>();

        float factor = 0.05f;

        float ySize = size.height * factor;
        float xSize = size.width * factor;
        for (float y = 0; y < size.height; y += ySize) {
            for (float x = 0; x < size.width; x += xSize) {
                v1.add(new Rectangle2D.Float(x, y, xSize, ySize));
            }
        }


        Point2D p1, p2;
        // 1 -> 0, 0 -> PI,
        float angleProgress = (float) (1 - Math.pow(progress, 0.2));
        v2.add(new ImageInstruction(true));
        for (Rectangle2D rectangle : v1) {
            try {
                p1 = new Point2D.Double(rectangle.getCenterX(), rectangle.getCenterY());
                AffineTransform transform = new AffineTransform();

                double anchorX = size.width / 2.0;
                double anchorY = size.height / 2.0;
                transform.setToRotation(-2 * PI * angleProgress, anchorX, anchorY);
                transform.translate(anchorX, anchorY);
                transform.scale(progress, progress);
                transform.translate(-anchorX, -anchorY);

                p2 = new Point2D.Double();
                transform.transform(p1, p2);
                transform.setToTranslation(p2.getX() - p1.getX(), p2.getY() - p1.getY());
                v2.add(new ImageInstruction(false, (float) Math.pow(progress, 0.4), transform.createInverse(), rectangle));

                transform.setToRotation(2 * PI * angleProgress, anchorX, anchorY);

                p2 = new Point2D.Double();
                transform.transform(p1, p2);
                transform.setToTranslation(p2.getX() - p1.getX(), p2.getY() - p1.getY());
                v2.add(new ImageInstruction(false, progress * progress, transform.createInverse(), rectangle));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return v2.toArray(new Transition2DInstruction[v2.size()]);
    }

    @Override
    public String toString() {
        return "Refractive";
    }

}
