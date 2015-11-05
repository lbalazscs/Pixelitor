/*
 * @(#)SwivelTransition2D.java
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

import net.jafama.FastMath;

import java.awt.Color;
import java.awt.geom.Point2D;

/** This transition resembles two still images on a turntable.  The table spins
 * clockwise or counter-clockwise, and the foremost image rotates to the
 * background and the new image rotates forward. Here are playback samples:
 * <p><table summary="Sample Animations of SwivelTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SwivelTransition2D/SwivelCounterclockwise.gif" alt="Swivel Counterclockwise">
 * <p>Swivel Counterclockwise
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SwivelTransition2D/SwivelClockwise.gif" alt="Swivel Clockwise">
 * <p>Swivel Clockwise
 * </td>
 * </tr></table>
 *
 */
public class SwivelTransition2D extends AbstractPlanarTransition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new SwivelTransition2D(COUNTER_CLOCKWISE), 
				new SwivelTransition2D(CLOCKWISE)
		};
	}
	
	int multiplier;

	/** Creates a new swivel transition that moves clockwise.
	 * 
	 */
	public SwivelTransition2D() {
		this(CLOCKWISE);
	}

	/** Creates a new swivel transition against a black background.
	 * 
	 * @param direction must be CLOCKWISE or MOVE_COUNTERCLOCKWISE.
	 */
	public SwivelTransition2D(int direction) {
		this(Color.black, direction);
	}
	
	/** Creates a new swivel transition.
	 * 
	 * @param direction must be CLOCKWISE or MOVE_COUNTERCLOCKWISE.
	 */
	public SwivelTransition2D(Color background,int direction) {
		super(background);
		if(direction==CLOCKWISE) {
			multiplier = 1;
		} else if(direction==COUNTER_CLOCKWISE) {
			multiplier = -1;
		} else {
			throw new IllegalArgumentException("The direction must be CLOCKWISE or COUNTER_CLOCKWISE");
		}
	}

	
	@Override
	public String toString() {
        if(multiplier==-1) {
		return "Swivel Counterclockwise";
        }
        return "Swivel Clockwise";
	}
	
	@Override
	public float getFrameAOpacity(float p) {
		if(p<.5f) {
			return 1f;
		}
		p = 1-(p-.5f)/.5f;
		p = (float)Math.sqrt(p);
		return p;
	}
	
	@Override
	public float getFrameBOpacity(float p) {
		if(p>.5f)
			return 1f;
		p = p/.5f;
		p = (float)Math.pow(p, .5);
		return p;
	}

	@Override
	public Point2D getFrameALocation(float p) {
		p = multiplier*p;
		return new Point2D.Double(.5 * FastMath.cos(Math.PI * p + Math.PI / 2) + .5,
				.5 * FastMath.sin(Math.PI * p + Math.PI / 2) + .5);
	}
	
	@Override
	public Point2D getFrameBLocation(float p) {
		p = multiplier*p;
		return new Point2D.Double(.5 * FastMath.cos(Math.PI * p + 3 * Math.PI / 2) + .5,
				.5 * FastMath.sin(Math.PI * p + 3 * Math.PI / 2) + .5);
	}
}
