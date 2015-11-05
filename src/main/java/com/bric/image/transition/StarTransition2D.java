/*
 * @(#)StarTransition2D.java
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

import java.awt.Shape;
import java.awt.geom.GeneralPath;

/** This clips to the shape of a star zooming in/out. Here are playback samples:
 * <p><table summary="Sample Animations of StarTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/StarTransition2D/StarIn.gif" alt="Star In">
 * <p>Star In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/StarTransition2D/StarOut.gif" alt="Star Out">
 * <p>Star Out
 * </td>
 * </tr></table>
 */
public class StarTransition2D extends AbstractShapeTransition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new StarTransition2D(IN), 
				new StarTransition2D(OUT)
		};
	}
	
	/** Creates a new StarTransition2D that zooms out.
	 * 
	 */
	public StarTransition2D() {
		super();
	}

	/** Creates a new StarTransition2D.
	 * 
	 * @param type must be IN or OUT
	 */
	public StarTransition2D(int type) {
		super(type);
	}

	@Override
	public Shape getShape() {
		GeneralPath p = new GeneralPath();
		double angle = Math.PI/10;
		float r2 = 2.5f;
		double k = Math.PI*2/10;
		p.moveTo((float) (FastMath.cos(angle)), (float) (FastMath.sin(angle)));
		for(int a = 0; a<5; a++) {
			p.lineTo((float) (r2 * FastMath.cos(angle + k)), (float) (r2 * FastMath.sin(angle + k)));
			angle+= Math.PI*2.0/5.0;
			p.lineTo((float) (FastMath.cos(angle)), (float) (FastMath.sin(angle)));
		}
		p.closePath();
		return p;
	}
	
	@Override
	public String getShapeName() {
		return "Star";
	}

}
