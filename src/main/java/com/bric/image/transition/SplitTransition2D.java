/*
 * @(#)SplitTransition2D.java
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

/** This splits the frame either horizontally or vertically.
 * This is also known as the "barn-door effect". Here are playback samples:
 * <p><table summary="Sample Animations of SplitTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SplitTransition2D/SplitHorizontalIn.gif" alt="Split Horizontal In">
 * <p>Split Horizontal In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SplitTransition2D/SplitHorizontalOut.gif" alt="Split Horizontal Out">
 * <p>Split Horizontal Out
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SplitTransition2D/SplitVerticalIn.gif" alt="Split Vertical In">
 * <p>Split Vertical In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SplitTransition2D/SplitVerticalOut.gif" alt="Split Vertical Out">
 * <p>Split Vertical Out
 * </td>
 * </tr></table>
 */
public class SplitTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new SplitTransition2D(HORIZONTAL, false),
				new SplitTransition2D(HORIZONTAL, true),
				new SplitTransition2D(VERTICAL, false),
				new SplitTransition2D(VERTICAL, true)
		};
	}
	
	int type;
	boolean in;
	
	/** Creates a new SplitTransition2D that uses horizontal
	 * strips that grow outwards from the middle.
	 */
	public SplitTransition2D() {
		this(HORIZONTAL,false);
	}
	
	/** Creates a new SplitTransition2D
	 * 
	 * @param type must be HORIZONTAL or VERTICAL
	 * @param in whether the halves in this transition grow in (true) or out (false)
	 */
	public SplitTransition2D(int type,boolean in) {
		if(!(type==HORIZONTAL || type==VERTICAL))
			throw new IllegalArgumentException("The type must be HORIZONTAL or VERTICAL.");
		this.type = type;
		this.in = in;
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		Rectangle2D rect;
		if(in)
			progress = 1-progress;
		if(type==HORIZONTAL) {
			float k = size.height/2f*progress;
			rect = new Rectangle2D.Float(
					0, size.height/2f-k,
					size.width, 2*k );
		} else {
			float k = size.width/2f*progress;
			rect = new Rectangle2D.Float(
					size.width/2f-k, 0,
					2*k, size.height );
		}
		return new ImageInstruction[] {
				new ImageInstruction(!in),
				new ImageInstruction(in,null,rect)
		};
	}
	
	@Override
	public String toString() {
		if(in && type==HORIZONTAL) {
			return "Split Horizontal In";
		} else if(type==HORIZONTAL) {
			return "Split Horizontal Out";
		} else if(in && type==VERTICAL) {
			return "Split Vertical In";
		} else {
			return "Split Vertical Out";
		}
	}

}
