/*
 * @(#)SpiralTransition2D.java
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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import com.bric.geom.RectangularTransform;
import com.bric.geom.ShapeStringUtils;
import com.bric.geom.ShapeUtils;

/** In this transition a wobbling spiral grows to fill the entire
 * frame; inside the shape is the incoming image. Here are playback samples:
 * <p><table summary="Sample Animations of SpiralTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SpiralTransition2D/SpiralSprawl.gif" alt="Spiral Sprawl">
 * <p>Spiral Sprawl
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SpiralTransition2D/Spiral.gif" alt="Spiral">
 * <p>Spiral
 * </td>
 * </tr></table>
 *
 */
public class SpiralTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new SpiralTransition2D(false), 
				new SpiralTransition2D(true)
		};
	}
	
	Shape spiral;
	boolean sprawl = true;
	Rectangle2D shapeBounds;
	
	/** Creates a new spiral transition.
	 * 
	 * @param sprawl whether the spiral rotates as the transition progresses
	 * 
	 */
	public SpiralTransition2D(boolean sprawl) {
		spiral = ShapeStringUtils.createGeneralPath("m 32.574 32.527 c 21.77 23.645 42.863 21.455 42.537 32.494 c 42.212 43.533 34.209 45.303 27.629 42.58 c 21.049 39.857 17.374 35.943 18.708 27.331 c 20.043 18.72 27.036 7.229 39.603 12.433 c 52.17 17.636 56.668 23.651 53.935 37.469 c 51.202 51.287 43.916 57.222 28.139 53.074 c 12.361 48.927 0.062 39.761 7.31 20.954 c 14.558 2.147 23.188 -2.412 40.942 0.083 c 58.696 2.579 69.57 20.663 64.804 38.565 c 60.038 56.468 53.063 66.173 28.941 64.198 c 4.82 62.224 -7.552 41.196 -6.927 32.645 c -6.303 24.094 -1.187 8.315 6.772 -1.593 z");
		this.sprawl = sprawl;
		shapeBounds = new Rectangle2D.Float(0,0,60,60);
	}
	
	@Override
	public String toString() {
        if(sprawl)
            return "Spiral Sprawl";
		return "Spiral";
	}
	
	public Shape getShape(float progress) {
		if(sprawl) {
			double theta = (1-progress)*3*Math.PI;
			AffineTransform rotate = AffineTransform.getRotateInstance(theta,30,30);
			return rotate.createTransformedShape(spiral);
		}
		return spiral;
	}
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,Dimension size) {
		Shape subShape = ShapeUtils.traceShape(getShape(progress),progress);
		BasicStroke stroke = new BasicStroke(16.23f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		subShape = stroke.createStrokedShape(subShape);
		Rectangle2D bigRect = new Rectangle2D.Float(0,0,size.width,size.height);
		AffineTransform t = RectangularTransform.create(shapeBounds,bigRect);
		
		subShape = t.createTransformedShape(subShape);
		
		return new Transition2DInstruction[] {
				new ImageInstruction(true),
				new ImageInstruction(false,null,subShape)
		};
	}

}
