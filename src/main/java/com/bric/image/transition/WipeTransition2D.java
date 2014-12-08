/*
 * @(#)WipeTransition2D.java
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
import java.awt.geom.Rectangle2D;

/** This is the standard "wipe" transition. Here are playback samples:
 * <p><table summary="Sample Animations of WipeTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WipeTransition2D/WipeLeft.gif" alt="Wipe Left">
 * <p>Wipe Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WipeTransition2D/WipeRight.gif" alt="Wipe Right">
 * <p>Wipe Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WipeTransition2D/WipeUp.gif" alt="Wipe Up">
 * <p>Wipe Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/WipeTransition2D/WipeDown.gif" alt="Wipe Down">
 * <p>Wipe Down
 * </td>
 * </tr></table>
 */
public class WipeTransition2D extends Transition2D {

	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new WipeTransition2D(UP),
				new WipeTransition2D(LEFT),
				new WipeTransition2D(DOWN), 
				new WipeTransition2D(RIGHT)
		};
	}
	
	
	int direction;

	/** Creates a wipe transition that wipes to the right.
	 * 
	 */
	public WipeTransition2D() {
		this(RIGHT);
	}

	/** Creates a wipe transition
	 * 
	 * @param direction must be LEFT, UP, DOWN or RIGHT
	 */
	public WipeTransition2D(int direction) {
		this.direction = direction;
		if(!(direction==LEFT || direction==UP ||
				direction==RIGHT || direction==DOWN))
			throw new IllegalArgumentException();
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		Rectangle2D clipping = null;
		if(direction==RIGHT) {
			clipping = new Rectangle2D.Double(0,0,progress*size.width,size.height);
		} else if(direction==LEFT) {
			double x = (1-progress)*size.width;
			clipping = new Rectangle2D.Double(x,0,size.width-x,size.height);
		} else if(direction==DOWN) {
			clipping = new Rectangle2D.Double(0,0,size.width,progress*size.width);
		} else if(direction==UP) {
			double y = (1-progress)*size.height;
			clipping = new Rectangle2D.Double(0,y,size.width,size.height-y);
		}
		return new Transition2DInstruction[] {
				new ImageInstruction(true),
				new ImageInstruction(false,null,clipping)
		};
	}
	
	@Override
	public String toString() {
		if(direction==RIGHT) {
			return "Wipe Right";
		} else if(direction==LEFT) {
			return "Wipe Left";
		} else if(direction==DOWN) {
			return "Wipe Down";
		} else {
			return "Wipe Up";
		}
	}
}
