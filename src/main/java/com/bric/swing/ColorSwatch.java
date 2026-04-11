/*
 * @(#)ColorSwatch.java
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
package com.bric.swing;

import com.bric.plaf.PlafPaintUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * This is a square, opaque panel used to indicate
 * a certain color.
 * <P>The color is assigned with the <code>setForeground()</code> method.
 * <P>Also the user can right-click this panel and select 'Copy' to send
 * a 100x100 image of this color to the clipboard.  (This feature was
 * added at the request of a friend who paints; she wanted to select a
 * color and then quickly print it off, and then mix her paints to match
 * that shade.)
 */
public class ColorSwatch extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int size;

    public ColorSwatch(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
    }

    public ColorSwatch(Color color, int size) {
        this(size);
        setForeground(color);
    }

    private static TexturePaint checkerPaint = null;

    private static TexturePaint getCheckerPaint() {
        if (checkerPaint == null) {
            int t = 8;
            BufferedImage bi = new BufferedImage(t * 2, t * 2, TYPE_INT_RGB);
            Graphics g = bi.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 2 * t, 2 * t);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, t, t);
            g.fillRect(t, t, t, t);
            checkerPaint = new TexturePaint(bi, new Rectangle(0, 0, bi.getWidth(), bi.getHeight()));
        }
        return checkerPaint;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g); //may be necessary for some look-and-feels?

        Graphics2D g2 = (Graphics2D) g;

        Color c = getForeground();
        int w2 = Math.min(getWidth(), size);
        int h2 = Math.min(getHeight(), size);
        Rectangle r = new Rectangle(getWidth() / 2 - w2 / 2, getHeight() / 2 - h2 / 2, w2, h2);

        if (!isEnabled()) {
            // lbalazscs: respecting the enabled setting
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            PlafPaintUtils.drawBevel(g2, r);
            return;
        }

        if (c.getAlpha() < 255) {
            TexturePaint checkers = getCheckerPaint();
            g2.setPaint(checkers);
            g2.fillRect(r.x, r.y, r.width, r.height);
        }
        g2.setColor(c);
        g2.fillRect(r.x, r.y, r.width, r.height);
        PlafPaintUtils.drawBevel(g2, r);
    }
}
