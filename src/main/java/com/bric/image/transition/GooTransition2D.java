/*
 * @(#)GooTransition2D.java
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
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Random;

/** This resembles a bucket of paint being thrown at a wall.  The
 * clipping of the incoming shape is this liquid-dripping outline. Here are playback samples:
 * <p><table summary="Sample Animations of GooTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/GooTransition2D/Goo(10).gif" alt="Goo (10)">
 * <p>Goo (10)
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/GooTransition2D/Goo(20).gif" alt="Goo (10)">
 * <p>Goo (20)
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/GooTransition2D/Goo(50).gif" alt="Goo (10)">
 * <p>Goo (50)
 * </td>
 * </tr></table>
 *
 */
public class GooTransition2D extends AbstractClippedTransition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new GooTransition2D(10),
				new GooTransition2D(20),
				new GooTransition2D(50)
		};
	}

	float[] offset;
	float[] accel;
	
	/** Creates a new goo transtion with 20 columns. */
	public GooTransition2D() {
		this(20);
	}
	
	/** Creates a new goo transition.
	 * 
	 * @param columns the number of columns to use.
	 */
	public GooTransition2D(int columns) {
		Random r = new Random();
		offset = new float[columns];
		accel = new float[columns];
		boolean ok = false;
		long seed = System.currentTimeMillis();
		while(!ok) {
			seed++;
			r.setSeed(seed);
			ok = true;
			for(int a = 0; a<columns && ok; a++) {
				offset[a] = -r.nextFloat();
				accel[a] = 4*r.nextFloat()+.2f;
				if(accel[a]+1+offset[a]<1.2)
					ok = false;
			}
			if(ok) {
				//make sure at least one strand takes up the whole time t=[0,1]
				boolean atLeastOneSlowOne = false;
				for(int a = 0; a<columns && atLeastOneSlowOne==false; a++) {
					atLeastOneSlowOne = accel[a]+1+offset[a]<1.3;
				}
				ok = atLeastOneSlowOne;
			}
		}
	}
	
	@Override
	public Shape[] getShapes(float progress, Dimension size) {
		float[] f = new float[offset.length];
		for(int a = 0; a<f.length; a++) {
			f[a] = size.height*(offset[a]+progress+progress*progress*accel[a]);
		}
		float w = ((float)size.width)/((float)f.length);
		
        int k = 4; //padding to make the stroke doesn't show
        
		GeneralPath path = new GeneralPath();
		path.moveTo(-k, -k);
		
		path.lineTo(-k, f[0]);
		path.lineTo(w/2f, f[0]);
		
		for(int a = 1; a<f.length; a++) {
			float x1 = (a-1)*w+w/2f;
			float x2 = (a)*w+w/2f;
			path.curveTo(x1+w/2, f[a-1], x2-w/2, f[a], x2, f[a]);
		}
		path.lineTo(size.width+k, f[f.length-1]);
		
		path.lineTo(size.width+k, -k);
		path.lineTo(-k, -k);
		path.closePath();
		
		return new Shape[] {path};
	}

	@Override
	public float getStrokeWidth(float progress,Dimension size) {
		return 1;
	}
	
	@Override
	public String toString() {
		return "Goo ("+offset.length+")";
	}

}
