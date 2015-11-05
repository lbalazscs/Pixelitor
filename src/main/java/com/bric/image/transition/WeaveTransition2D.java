/*
 * @(#)WeaveTransition2D.java
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

/** This paints the incoming frame in thin horizontal strips that slide
 * into place. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/WeaveTransition2D/Weave.gif" alt="WeaveTransition2D Demo">
 * 
 */
public class WeaveTransition2D extends Transition2D {
	
	/** Creates a new weave transition */
	public WeaveTransition2D() {
	}
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		float stripHeight = 5;
		progress = (float)(-1.6666666666666186*progress*progress+2.6666666666666203*progress);
		
		
		float progress2 = (float)Math.pow(1-progress,3)*.5f+(1-progress)*.5f;
		if(progress>1)
			progress2 = (float)Math.pow(1-progress,2);
		float dip = -(2*progress-1)*(2*progress-1)+1;
		Vector<Rectangle2D> v = new Vector<Rectangle2D>();
		for(int y = size.height; y>-stripHeight; y-=stripHeight) {
			v.add(new Rectangle2D.Float(0,y,size.width,stripHeight));
		}
		Transition2DInstruction[] instr = new Transition2DInstruction[v.size()+1];
		instr[0] = new ImageInstruction(true);
		for(int a = 0; a<v.size(); a++) {
			Rectangle2D r = v.get(a);
			AffineTransform transform = new AffineTransform();
			float dx = (float) (FastMath.sin(.5 * Math.PI * (1 - progress)) * size.width);
			float k = (progress2)*(1000*dip)*(a)/(v.size());
			dx = dx+k;
			if(a%2==0) {
				transform.translate(dx,0);
			} else {
				transform.translate(-dx,0);
			}
			instr[a+1] = new ImageInstruction(false,transform,transform.createTransformedShape(r));
		}
		return instr;
	}
	
	@Override
	public String toString() {
		return "Weave";
	}
	
}
