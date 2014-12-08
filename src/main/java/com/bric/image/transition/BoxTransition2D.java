/*
 * @(#)BoxTransition2D.java
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

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/** This clips to the shape of a square zooming in/out. Here are playback samples:
 * <p><table summary="Sample Animations of BoxTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/BoxTransition2D/BoxIn.gif" alt="Box In">
 * <p>Box In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/BoxTransition2D/BoxOut.gif" alt="Box Out">
 * <p>Box Out
 * </td>
 * </tr></table>
 */
public class BoxTransition2D extends AbstractShapeTransition2D {

	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new BoxTransition2D(IN),
				new BoxTransition2D(OUT)
		};
	}

	/** Creates a new BoxTransition2D that zooms out
	 */
	public BoxTransition2D() {}

	/** Creates a new BoxTransition2D
	 * 
	 * @param type must be IN or OUT
	 */
	public BoxTransition2D(int type) {
		super(type);
	}

	@Override
	public Shape getShape() {
		return new Rectangle2D.Float(0,0,100,100);
	}

	@Override
	public String getShapeName() {
		return "Box";
	}

}
