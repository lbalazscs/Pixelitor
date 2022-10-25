/*
 * $Id: InnerGlowPathEffect.java 1837 2007-03-15 22:44:22Z joshy $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


package org.jdesktop.swingx.painter.effects;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * An effect which draws a glow inside the painter's shape
 *
 * @author joshy
 */
public class InnerGlowPathEffect extends AbstractAreaEffect {
    @Serial
    private static final long serialVersionUID = 1887188174881815174L;

    public InnerGlowPathEffect() {
        setBrushColor(Color.WHITE);
        setBrushSteps(10);
        setEffectWidth(10);
        setShouldFillShape(false);
        setOffset(new Point(0, 0));
        setRenderInsideShape(true);
    }

    public InnerGlowPathEffect(float opacity) {
        this();
        setOpacity(opacity); // opacity support added by lbalazscs
    }

    // copied the entire method from the superclass in order to safely fix issue #63
    @Override
    public void apply(Graphics2D g, Shape clipShape, int width, int height) {
        // opacity support added by lbalazscs
        Composite savedComposite = g.getComposite();
        if (opacity < 1.0f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }

        // create a rect to hold the bounds
        Rectangle2D clipShapeBounds = clipShape.getBounds2D();

        if (clipShapeBounds.isEmpty()) {
            // check added by lbalazscs
            return;
        }

        width = (int) (clipShapeBounds.getWidth() + clipShapeBounds.getX());
        height = (int) (clipShapeBounds.getHeight() + clipShapeBounds.getY());
        Rectangle effectBounds = new Rectangle(0, 0, width + 2, height + 2);

        if (effectBounds.isEmpty()) {
            // check added by lbalazscs
            // this can be empty even if the clip shape bounds is not
            // when the clip shape starts at large negative coordinates
            return;
        }

        // Apply the border glow effect
        BufferedImage clipImage = getClipImage(effectBounds);
        Graphics2D g2 = clipImage.createGraphics();

        // lbalazscs: moved here from getClipImage
        // in order to avoid two createGraphics calls
        g2.clearRect(0, 0, clipImage.getWidth(), clipImage.getHeight());

        try {
            // clear the buffer
            g2.setPaint(Color.BLACK);
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, effectBounds.width, effectBounds.height);

            // turn on smoothing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            paintBorderGlow(g2, clipShape, width, height);

            // clip out the parts we don't want
            g2.setComposite(AlphaComposite.Clear);
            g2.setColor(Color.WHITE);

            // clip the outside
            Area area = new Area(effectBounds);
            area.subtract(new Area(clipShape));
            g2.fill(area);
        } finally {
            // draw the final image
            g2.dispose();
        }

        g.drawImage(clipImage, 0, 0, null);


        //g.setColor(Color.MAGENTA);
        //g.draw(clipShape.getBounds2D());
        //g.drawRect(0,0,width,height);

        g.setComposite(savedComposite);
    }

    // copied from superclass in order to simplify
    @Override
    protected void paintBorderGlow(Graphics2D g2,
                                   Shape clipShape, int width, int height) {
        g2.setPaint(getBrushColor());

        int steps = getBrushSteps();
        float brushAlpha = 1.0f / steps;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, brushAlpha));

        // draw the effect
        for (float i = 0; i < steps; i = i + 1.0f) {
            float brushWidth = (float) (i * effectWidthDouble / steps);
            g2.setStroke(new BasicStroke(brushWidth,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(clipShape);
        }
    }
}
