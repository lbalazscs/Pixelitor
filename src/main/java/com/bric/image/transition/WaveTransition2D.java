/*
 * @(#)WaveTransition2D.java
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

import com.bric.geom.TransformUtils;
import net.jafama.FastMath;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/** This slides the incoming frame in a wavy motion, similar to a flag
 * whipping in the wind. Here are playback samples:
 * <p><table summary="Sample Animations of WaveTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WaveTransition2D/WaveLeft.gif" alt="Wave Left">
 * <p>Wave Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WaveTransition2D/WaveRight.gif" alt="Wave Right">
 * <p>Wave Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WaveTransition2D/WaveUp.gif" alt="Wave Up">
 * <p>Wave Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WaveTransition2D/WaveDown.gif" alt="Wave Down">
 * <p>Wave Down
 * </td>
 * </tr></table>
 * 
 */
public class WaveTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new WaveTransition2D(LEFT), 
				new WaveTransition2D(RIGHT), 
				new WaveTransition2D(UP), 
				new WaveTransition2D(DOWN)
		};
	}
	
	int type = RIGHT;
	
	/** Creates a new wave transition that moves to the right
	 * 
	 */
	public WaveTransition2D() {}

	/** Creates a new wave transition.
	 * 
	 * @param type must be LEFT, RIGHT, UP or DOWN
	 */
	public WaveTransition2D(int type) {
		if(!(type==LEFT || type==RIGHT || type==DOWN || type==UP))
			throw new IllegalArgumentException("Type must be LEFT, UP, RIGHT or DOWN");
		this.type = type;
	}
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,Dimension size) {
		int k = size.height/5;
		Transition2DInstruction[] i = new Transition2DInstruction[k+1];
		float progress2 = (float)Math.sqrt(progress);
		i[0] = new ImageInstruction(true);
		
		int measurement = size.width;
		if(type==UP || type==DOWN) {
			measurement = size.height;
		}
		
		double lastD = 0;
		
		for(int a = 0; a<i.length; a++) {
			float z1 = ((float)(a-1))/((float)(i.length-1));
			float z2 = ((float)(a))/((float)(i.length-1));
			AffineTransform transform;
			float wave = (float)(.3* FastMath.sin(1 + 3 * z1 + 8 * progress));
			float dip = 1-.5f*(2*progress-1)*(2*progress-1)*(2*progress-1)-.5f;
			
			
			
			double d = (1-progress2)*measurement+(1-z2)*dip*(z2+wave+.4)*measurement;
			
			Rectangle2D clipping;
			if(a>0) {
				if(type==LEFT || type==RIGHT) {

					clipping = new Rectangle2D.Float( (0),z1*size.height,size.width,z2*size.height-z1*size.height+1);
					if(type==LEFT) {
						transform = TransformUtils.createAffineTransform(
								0, z2*size.height,
								0, z1*size.height,
								size.width, z2*size.height,
								d, z2*size.height,
								lastD, z1*size.height,
								d+size.width, z2*size.height
						);
					} else {
						transform = TransformUtils.createAffineTransform(
								size.width, z2*size.height,
								size.width, z1*size.height,
								0, z2*size.height,
								size.width-d, z2*size.height,
								size.width-lastD, z1*size.height,
								size.width-d-size.width, z2*size.height
						);
					}
				} else {

					clipping = new Rectangle2D.Float( z1*size.width, (0),z2*size.width-z1*size.width+1,size.height);
					if(type==UP) {
						transform = TransformUtils.createAffineTransform(
								z2*size.width, 0,
								z1*size.width, 0,
								z2*size.width, size.height,
								z2*size.width, d,
								z1*size.width, lastD,
								z2*size.width, d+size.height
						);
					} else {
						transform = TransformUtils.createAffineTransform(
								z2*size.width, size.height,
								z1*size.width, size.height, 
								z2*size.width, 0,
								z2*size.width, size.height-d,
								z1*size.width, size.height-lastD, 
								z2*size.width, size.height-d-size.height
						);
					}
				}
				i[a] = new ImageInstruction(false,transform,transform.createTransformedShape(clipping));
			}
			lastD = d;
		}
		return i;
	}
	
	@Override
	public String toString() {
        if(type==UP) {
            return "Wave Up";
        } else if(type==DOWN) {
            return "Wave Down";
        } else if(type==RIGHT) {
            return "Wave Right";
        } else {
            return "Wave Left";
        }
	}

}
