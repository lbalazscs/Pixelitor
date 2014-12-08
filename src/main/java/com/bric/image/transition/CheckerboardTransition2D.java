/*
 * @(#)CheckerboardTransition2D.java
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
import java.awt.geom.GeneralPath;

import com.bric.geom.TransformUtils;

/** This creates a checkerboard pattern that grows to reveal the new frame. Here are playback samples:
 * <p><table summary="Sample Animations of CheckerboardTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CheckerboardTransition2D/CheckerboardLeft.gif" alt="Checkerboard Left">
 * <p>Checkerboard Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CheckerboardTransition2D/CheckerboardRight.gif" alt="Checkerboard Right">
 * <p>Checkerboard Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CheckerboardTransition2D/CheckerboardUp.gif" alt="Checkerboard Up">
 * <p>Checkerboard Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CheckerboardTransition2D/CheckerboardDown.gif" alt="Checkerboard Down">
 * <p>Checkerboard Down
 * </td>
 * </tr></table>
 *
 */
public class CheckerboardTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new CheckerboardTransition2D(LEFT),
				new CheckerboardTransition2D(RIGHT),
				new CheckerboardTransition2D(UP),
				new CheckerboardTransition2D(DOWN)
		};
	}
	
	int type;
	
	int rowCount = 20;
	int columnCount = 20;
	
	/** Creates a new CheckerboardTransition2D that moves right.
	 * 
	 */
	public CheckerboardTransition2D() {
		this(RIGHT);
	}
	
	/** Creates a new CheckerboardTransition2D.
	 * 
	 * @param type must be LEFT, RIGHT, UP or DOWN
	 */
	public CheckerboardTransition2D(int type) {
		if(!(type==RIGHT || type==LEFT || type==UP || type==DOWN)) {
			throw new IllegalArgumentException("The type must be RIGHT, LEFT, UP or DOWN.");
		}
		this.type = type;
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		
		GeneralPath clipping = new GeneralPath();
		
		if(type==RIGHT || type==LEFT) {
			float k = ((float)size.width)/columnCount*2;
			float k2 = ((float)size.height)/rowCount;
			
			for(int row = 0; row<rowCount; row++) {
				float dx = 0;
				if(row%2==0)
					dx = k/2;
				
				for(int column = -1; column<columnCount; column++) {
					clipping.moveTo(column*k+dx, row*k2);
					clipping.lineTo(column*k+dx, row*k2+k2);
					clipping.lineTo(column*k+k*progress+dx, row*k2+k2);
					clipping.lineTo(column*k+k*progress+dx, row*k2);
					clipping.lineTo(column*k+dx, row*k2);
					clipping.closePath();
				}
			}
			
			if(type==LEFT) {
				AffineTransform flip = TransformUtils.createAffineTransform(
						0,0,
						size.width,0,
						0,size.height,
						size.width,0,
						0,0,
						size.width,size.height
				);
				clipping.transform(flip);
			}
		} else {
			float k = ((float)size.height)/rowCount*2;
			float k2 = ((float)size.width)/columnCount;

			for(int column = 0; column<columnCount; column++) {
				float dy = 0;
				if(column%2==0)
					dy = k/2;
				
				for(int row = -1; row<rowCount; row++) {
					clipping.moveTo(column*k2, row*k+dy);
					clipping.lineTo(column*k2+k2, row*k+dy);
					clipping.lineTo(column*k2+k2, row*k+k*progress+dy);
					clipping.lineTo(column*k2, row*k+k*progress+dy);
					clipping.lineTo(column*k2, row*k+dy);
					clipping.closePath();
				}
			}
			
			if(type==UP) {
				AffineTransform flip = TransformUtils.createAffineTransform(
						0,0,
						size.width,0,
						0,size.height,
						0,size.height,
						size.width, size.height,
						0,0
				);
				clipping.transform(flip);
			}
		}
		
		return new Transition2DInstruction[] {
			new ImageInstruction(true),
			new ImageInstruction(false,null,clipping)
				
		};
	}
	
	@Override
	public String toString() {
		if(type==RIGHT) {
			return "Checkerboard Right";
		} else if(type==LEFT) {
			return "Checkerboard Left";
		} else if(type==UP) {
			return "Checkerboard Up";
		} else {
			return "Checkerboard Down";
		}
	}

}
