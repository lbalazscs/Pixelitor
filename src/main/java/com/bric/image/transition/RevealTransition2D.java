/*
 * @(#)RevealTransition2D.java
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

/** This takes the current frame and slides it away to reveal the new frame
 * underneath. Here are playback samples:
 * <p><table summary="Sample Animations of RevealTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/RevealTransition2D/RevealLeft.gif" alt="Reveal Left">
 * <p>Reveal Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/RevealTransition2D/RevealRight.gif" alt="Reveal Right">
 * <p>Reveal Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/RevealTransition2D/RevealUp.gif" alt="Reveal Up">
 * <p>Reveal Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/RevealTransition2D/RevealDown.gif" alt="Reveal Down">
 * <p>Reveal Down
 * </td>
 * </tr></table>
 */
public class RevealTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new RevealTransition2D(LEFT),
				new RevealTransition2D(RIGHT),
				new RevealTransition2D(UP), 
				new RevealTransition2D(DOWN)
		};
	}
	
	int direction;
	
	/** Creates a new RevealTransition2D that slides to the left.
	 * 
	 */
	public RevealTransition2D() {
		this(LEFT);
	}
	
	/** Creates a new RevealTransition2D
	 * 
	 * @param direction must be LEFT, RIGHT, UP or DOWN
	 */
	public RevealTransition2D(int direction) {
		if(!(direction==LEFT ||direction==RIGHT || direction==UP || direction==DOWN))
			throw new IllegalArgumentException("Direction must be LEFT, UP, RIGHT or DOWN");
		this.direction = direction;
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		AffineTransform transform;
		
		if(direction==LEFT) {
			transform = AffineTransform.getTranslateInstance( -progress*size.width, 0);
		} else if(direction==RIGHT) {
			transform = AffineTransform.getTranslateInstance( progress*size.width, 0);
		} else if(direction==UP) {
			transform = AffineTransform.getTranslateInstance( 0, -progress*size.height);
		} else {
			transform = AffineTransform.getTranslateInstance( 0, progress*size.height);
		}
		
		return new ImageInstruction[] {
				new ImageInstruction(false),
				new ImageInstruction(true,transform,null)
		};
	}

	@Override
	public String toString() {
		if(direction==UP) {
			return "Reveal Up";
		} else if(direction==LEFT) {
			return "Reveal Left";
		} else if(direction==RIGHT) {
			return "Reveal Right";
		} else {
			return "Reveal Down";
		}
	}
}
