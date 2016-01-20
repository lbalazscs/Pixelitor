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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/** This class was put together at the request of Thierry to help
 * experiment with new Transition classes.
 * <P>To test your transition, just redefine the "transition" field
 * in this object.
 *
 */
public class SimplestTransitionDemo extends JPanel {
    private static final long serialVersionUID = 1L;

    /** The first image to use in this transition. */
    public static  BufferedImage bi1 = AbstractTransition.createImage("A", true);

    /** The second image to use in this transition.
     * It's assumed this is the same dimension as bi1.
     **/
//    public static final BufferedImage bi2 = AbstractTransition.createImage("B", false);
	public static  BufferedImage bi2 = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);

    static {
        bi1 = ImageUtils.toSysCompatibleImage(bi1);
        bi2 = ImageUtils.toSysCompatibleImage(bi2);
    }

    /** How long the transition should last. */
    public static final float DURATION = 2000;

    /** Create a frame with a simple transition demo.
     * @param args the application's arguments. (This is unused.)
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("SimplestDemo");
        SimplestTransitionDemo d = new SimplestTransitionDemo();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(d);
        frame.pack();
        frame.setVisible(true);
    }

    //	Transition transition = new GooTransition2D();
//	Transition transition = new BlindsTransition2D();
//    Transition transition = new BoxTransition2D();
    Transition transition = new BlendTransition2D();

    ActionListener repainter = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            repaint();
        }
    };

    public SimplestTransitionDemo() {
        setPreferredSize(new Dimension(bi1.getWidth(),bi1.getHeight()));
        Timer timer = new Timer(50,repainter);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        long t = System.currentTimeMillis();
        float progress = ( t%((long)(DURATION*2)) );
        if(progress>DURATION) {
            progress = (progress-DURATION)/DURATION;
            transition.paint( (Graphics2D)g, bi2, bi1, progress);
        } else {
            progress = progress/DURATION;
            transition.paint( (Graphics2D)g, bi1, bi2, progress);
        }
    }
}

