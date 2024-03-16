/*
 * @(#)SimplestTransitionDemo.java
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

import pixelitor.utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 * This class was put together at the request of Thierry to help
 * experiment with new Transition classes.
 * <P>To test your transition, just redefine the "transition" field
 * in this object.
 */
public class SimplestTransitionDemo extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The first image to use in this transition.
     */
    private static BufferedImage bi1 = AbstractTransition.createImage("A", true);

    /**
     * The second image to use in this transition.
     * It's assumed this is the same dimension as bi1.
     **/
//    public static final BufferedImage bi2 = AbstractTransition.createImage("B", false);
    private static BufferedImage bi2 = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);

    static {
        bi1 = ImageUtils.toSysCompatibleImage(bi1);
        bi2 = ImageUtils.toSysCompatibleImage(bi2);
    }

    /**
     * How long the transition should last.
     */
    private static final float DURATION = 2000;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimplestTransitionDemo::buildGUI);
    }

    private static void buildGUI() {
        JFrame frame = new JFrame("SimplestDemo");
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.add(new SimplestTransitionDemo());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private final Transition transition = new BlendTransition2D();

    public SimplestTransitionDemo() {
        setPreferredSize(new Dimension(bi1.getWidth(), bi1.getHeight()));
        Timer timer = new Timer(50, e -> repaint());
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        long t = System.currentTimeMillis();
        float progress = (t % ((long) (DURATION * 2)));
        if (progress > DURATION) {
            progress = (progress - DURATION) / DURATION;
            transition.paint((Graphics2D) g, bi2, bi1, progress);
        } else {
            progress = progress / DURATION;
            transition.paint((Graphics2D) g, bi1, bi2, progress);
        }
    }
}

