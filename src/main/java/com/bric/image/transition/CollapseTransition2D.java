/*
 * @(#)CollapseTransition2D.java
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

/** In this transition the original image is split into 6 horizontal strips,
 * and they collapse downward to reveal the next image underneath. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/CollapseTransition2D/Collapse.gif" alt="CollapseTransition2D Demo">
 *
 */
public class CollapseTransition2D extends Transition2D {
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		progress = (float)Math.pow(progress,2);
		float stripHeight = size.height/6;
		
		Vector<Rectangle2D> v = new Vector<Rectangle2D>();
		for(int y = 0; y<size.height; y+=stripHeight) {
			v.add(new Rectangle2D.Float(0,y,size.width,stripHeight));
		}
		ImageInstruction[] instr = new ImageInstruction[v.size()+1];
		instr[0] = new ImageInstruction(false);
		for(int a = 0; a<v.size(); a++) {
			Rectangle2D r = v.get(a);
			AffineTransform transform = new AffineTransform();
			float angleProgress = (float)Math.pow(progress, .6);
			float xProgress = 1.0f/(1.0f+progress);
			float k = (angleProgress)*(a)/(v.size());
			float theta = (float)(Math.PI*k/2+(progress)*Math.PI/2);
			if(theta>Math.PI/2) theta = (float)(Math.PI/2);
			float k2;
			theta = theta/(1+progress);
			k2 = 1*progress;
			if(a%2==0) {
				transform.rotate(theta,-size.width*(1-xProgress*xProgress*xProgress)/2,size.height*k2);
			} else {
				transform.rotate(-theta,size.width+(1-xProgress*xProgress*xProgress)*size.width/2,size.height*k2);
			}
			transform.translate(0,progress*progress*size.height*1.5);
			instr[a+1] = new ImageInstruction(true,transform,transform.createTransformedShape(r));
		}
		return instr;
	}
	
	@Override
	public String toString() {
		return "Collapse";
	}
	
}
