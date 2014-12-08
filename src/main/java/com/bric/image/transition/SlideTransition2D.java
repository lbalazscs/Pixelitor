/*
 * @(#)SlideTransition2D.java
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

/** This is the standard "slide" transition. Here are playback samples:
 * <p><table summary="Sample Animations of SlideTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SlideTransition2D/SlideLeft.gif" alt="Slide Left">
 * <p>Slide Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SlideTransition2D/SlideRight.gif" alt="Slide Right">
 * <p>Slide Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SlideTransition2D/SlideUp.gif" alt="Slide Up">
 * <p>Slide Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SlideTransition2D/SlideDown.gif" alt="Slide Down">
 * <p>Slide Down
 * </td>
 * </tr></table>
 */
public class SlideTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new SlideTransition2D(LEFT),
				new SlideTransition2D(RIGHT),
				new SlideTransition2D(UP), 
				new SlideTransition2D(LEFT)
		};
	}
	
	int type;
	
	/** Creates a slide-right transition.
	 */
	public SlideTransition2D() {
		this(RIGHT);
	}
	
	/** Creates a new SlideTransition2D.
	 * 
	 * @param type must be RIGHT, LEFT, UP or DOWN
	 */
	public SlideTransition2D(int type) {
		if(!(type==RIGHT || type==LEFT || type==UP || type==DOWN)) {
			throw new IllegalArgumentException("The type must be LEFT, RIGHT, UP or DOWN");
		}
		this.type = type;
	}
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,Dimension size) {
		AffineTransform transform = new AffineTransform();
		
		if(type==LEFT) {
			transform.translate(size.width*(1-progress),0);
		} else if(type==RIGHT) {
			transform.translate(size.width*(progress-1),0);
		} else if(type==UP) {
			transform.translate(0,size.height*(1-progress));
		} else {
			transform.translate(0,size.height*progress-1);
		}
		
		return new Transition2DInstruction[] {
				new ImageInstruction(type!=DOWN),
				new ImageInstruction(type==DOWN,transform,null)
		};
	}
	
	@Override
	public String toString() {
		if(type==RIGHT) {
			return "Slide Right";
		} else if(type==LEFT) {
			return "Slide Left";
		} else if(type==DOWN) {
			return "Slide Down";
		} else {
			return "Slide Up";
		}
	}
}
