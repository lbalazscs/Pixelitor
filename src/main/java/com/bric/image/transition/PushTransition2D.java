/*
 * @(#)PushTransition2D.java
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

/** This is the standard "push" transition. Here are playback samples:
 * <p><table summary="Sample Animations of PushTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/PushTransition2D/PushLeft.gif" alt="Push Left">
 * <p>Push Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/PushTransition2D/PushRight.gif" alt="Push Right">
 * <p>Push Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/PushTransition2D/PushUp.gif" alt="Push Up">
 * <p>Push Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/PushTransition2D/PushDown.gif" alt="Push Down">
 * <p>Push Down
 * </td>
 * </tr></table>
 */
public class PushTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new PushTransition2D(LEFT), 
				new PushTransition2D(DOWN), 
				new PushTransition2D(UP), 
				new PushTransition2D(RIGHT)
		};
	}
	
	int type;
	
	/** Creates a push-right transition.
	 */
	public PushTransition2D() {
		this(RIGHT);
	}
	
	/** Creates a new PushTransition2D
	 * 
	 * @param type must be LEFT, RIGHT, UP or DOWN
	 */
	public PushTransition2D(int type) {
		if(!(type==RIGHT || type==LEFT || type==UP || type==DOWN)) {
			throw new IllegalArgumentException("The type must be LEFT, RIGHT, UP or DOWN");
		}
		this.type = type;
	}
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,Dimension size) {
		AffineTransform transform1 = new AffineTransform();
		AffineTransform transform2 = new AffineTransform();
		
		if(type==LEFT) {
			transform2.translate(size.width*(1-progress),0);
			transform1.translate(size.width*(1-progress)-size.width,0);
		} else if(type==RIGHT) {
			transform2.translate(size.width*(progress-1),0);
			transform1.translate(size.width*(progress-1)+size.width,0);
		} else if(type==UP) {
			transform2.translate(0,size.height*(1-progress));
			transform1.translate(0,size.height*(1-progress)-size.height);
		} else {
			transform2.translate(0,size.height*(progress-1));
			transform1.translate(0,size.height*(progress-1)+size.height);
		}
		
		return new Transition2DInstruction[] {
				new ImageInstruction(true,transform1,null),
				new ImageInstruction(false,transform2,null)
		};
	}

	@Override
	public String toString() {
		if(type==RIGHT) {
			return "Push Right";
		} else if(type==LEFT) {
			return "Push Left";
		} else if(type==DOWN) {
			return "Push Down";
		} else {
			return "Push Up";
		}
	}
}
