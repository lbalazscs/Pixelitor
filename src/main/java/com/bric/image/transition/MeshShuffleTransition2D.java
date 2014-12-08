/*
 * @(#)MeshShuffleTransition2D.java
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

/** The concept here was to resemble a deck of cards being shuffled. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/MeshShuffleTransition2D/MeshShuffle.gif" alt="MeshShuffleTransition2D Demo">
 *
 */
public class MeshShuffleTransition2D extends Transition2D {
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		progress = (float)Math.pow(progress,.45);
		float stripHeight = size.height*10/200;
		
		Vector<Rectangle2D> v = new Vector<Rectangle2D>();
		for(int y = size.height; y>-stripHeight; y-=stripHeight) {
			v.add(new Rectangle2D.Float(0,y,size.width,stripHeight));
		}
		Transition2DInstruction[] instr = new Transition2DInstruction[v.size()];
		instr[0] = new ImageInstruction(true);
		for(int a = 1; a<v.size(); a++) {
			Rectangle2D r = v.get(a);
			AffineTransform transform = new AffineTransform();
			float k = (1-progress)*(a)/(v.size());
			float theta = (float)(Math.PI*k/2+(1-progress)*Math.PI/2);
			if(theta>Math.PI/2) theta = (float)(Math.PI/2);
			if(a%2==0) {
				transform.rotate(-theta,-size.width*(1-progress)/2,size.height*progress);
			} else {
				transform.rotate(theta,size.width+(1-progress)*size.width/2,size.height*progress);
			}
			instr[a] = new ImageInstruction(false,transform,transform.createTransformedShape(r));
		}
		return instr;
	}
	
	@Override
	public String toString() {
		return "Mesh Shuffle";
	}
	
}
