/*
 * @(#)AbstractTransition.java
 *
 * $Date: 2014-11-27 07:50:51 +0100 (Cs, 27 nov. 2014) $
 *
 * Copyright (c) 2014 by Jeremy Wood.
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

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** This abstract class handles bounds checking. */
public abstract class AbstractTransition implements Transition {

	/** Create an image with the text provided and a gradient background. This
	 * is intended to facilitate simple demos of transitions.
	 * 
	 * @param text the text to render, probably just 1 or 2 letters will do.
	 * @param light whether to use a light (red/yellow) background or a dark (green/blue)
	 * background.
	 * @return an image to test transitions with.
	 */
	public static BufferedImage createImage(String text,boolean light) {
		return createImage(400,text,light,true);
	}
	/** Create an image with the text provided. This
	 * is intended to facilitate simple demos of transitions.
	 * 
	 * @param size the width and height of this image.
	 * @param text the text to render, probably just 1 or 2 letters will do.
	 * @param light whether to use a light (red/yellow) background or a dark (green/blue)
	 * background.
	 * @param useGradients whether to use gradients as a background or not.
	 * @return an image to test transitions with.
	 */
	public static BufferedImage createImage(int size,String text,boolean light,boolean useGradients) {
		BufferedImage bi = new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
		Font font = new Font("Default",0,size*150/200);
		FontRenderContext frc = new FontRenderContext(new AffineTransform(),true,true);
		
		Graphics2D g = bi.createGraphics();
		if(useGradients) {
			if(light) {
				g.setPaint(new GradientPaint(0,bi.getHeight(),Color.red,bi.getWidth(),0,Color.yellow,true));
			} else {
				g.setPaint(new GradientPaint(0,0,Color.blue,bi.getWidth(),bi.getHeight(),Color.green,true));
			}
		} else {
			if(light) {
				g.setPaint(new Color(0xE19839));
			} else {
				g.setPaint(new Color(0x3B4E92));
			}
		}
		g.fillRect(0,0,bi.getWidth(),bi.getHeight());
		g.setColor(Color.black);
		g.setFont(font);
		float width = (float)font.getStringBounds(text,frc).getWidth();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.drawString(text, bi.getWidth()/2-width/2,size*160/200);
		g.dispose();
		
		return bi;
	}

	public final void paint(Graphics2D g, BufferedImage frameA, BufferedImage frameB,
			float progress) {
		if(g==null) throw new NullPointerException();
		if(frameA==null) throw new NullPointerException();
		if(frameB==null) throw new NullPointerException();
		if(!(frameA.getHeight()==frameB.getHeight() && frameA.getWidth()==frameB.getWidth()))
			throw new IllegalArgumentException(
				"the two images must be the same dimensions ("+frameA.getWidth()+"x"+frameA.getHeight() +" != "+frameB.getWidth()+"x"+frameB.getHeight());
		if(progress<0 || progress>1) throw new IllegalArgumentException("progress ("+progress+") should be between [0,1]");
		doPaint(g, frameA, frameB, progress);
	}

	/** Paint this transition. This method is invoked after bounds checking for
	 * all the arguments.
	 */
	protected abstract void doPaint(Graphics2D g, BufferedImage frameA, BufferedImage frameB,
			float progress);
	
	/** Return a rectangle including all the points in the argument. */
	protected static Rectangle2D getBounds(Point2D... p) {
		Rectangle2D r = new Rectangle2D.Double(p[0].getX(), p[0].getY(), 0, 0);
		for(int a = 1; a<p.length; a++) {
			r.add(p[a]);
		}
		return r;
	}
}
