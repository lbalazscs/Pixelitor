/*
 * @(#)RotateTransition2D.java
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

/** This spins one frame in/out from the center. Here are playback samples:
 * <p><table summary="Sample Animations of RotateTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/RotateTransition2D/RotateIn.gif" alt="Rotate In">
 * <p>Rotate In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/RotateTransition2D/RotateOut.gif" alt="Rotate Out">
 * <p>Rotate Out
 * </td>
 * </tr></table>
 */
public class RotateTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new RotateTransition2D(IN), 
				new RotateTransition2D(OUT)
		};
	}
	
	int type;
	
	/** Creates a new RotateTransition2D that rotates in.
	 * 
	 */
	public RotateTransition2D() {
		this(IN);
	}
	
	/** Creates a new RotateTransition2D
	 * 
	 * @param type must be IN or OUT
	 */
	public RotateTransition2D(int type) {
		if(!(type==IN || type==OUT)) {
			throw new IllegalArgumentException("type must be IN or OUT");
		}
		this.type = type;
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		if(type==OUT) {
			progress = 1-progress;
		}
		AffineTransform transform = new AffineTransform();
		transform.translate(size.width/2,size.height/2);
		transform.scale(progress, progress);
		transform.rotate((1-progress)*6);
		transform.translate(-size.width/2,-size.height/2);
		
		return new ImageInstruction[] {
				new ImageInstruction(type==IN),
				new ImageInstruction(type!=IN,transform,null)
		};
	}

	@Override
	public String toString() {
		if(type==IN) {
			return "Rotate In";
		} else {
			return "Rotate Out";
		}
	}
}
