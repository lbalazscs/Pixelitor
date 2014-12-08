/*
 * @(#)CurtainTransition2D.java
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
import java.awt.geom.Rectangle2D;

import com.bric.geom.RectangularTransform;

/** This splits a frame down the middle and squishes the two left and
 * right halves, as if a curtain is being opened.
 * 
 * <P>This is basically a "Split Vertical", except the two halves are squished. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/CurtainTransition2D/Curtain.gif" alt="CurtainTransition2D Demo">
 *
 */
public class CurtainTransition2D extends Transition2D {

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		
		progress = 1-progress;
		
		Rectangle2D rect1 = new Rectangle2D.Double(
			0,0,
			size.width/2f*progress, size.height
		);
		
		Rectangle2D rect2 = new Rectangle2D.Double(
				size.width-rect1.getWidth(), 0,
				rect1.getWidth(), rect1.getHeight()
			);
		
		AffineTransform transform1 = RectangularTransform.create(
			new Rectangle2D.Float(0,0, size.width/2f, size.height),
			rect1
		);

		AffineTransform transform2 = RectangularTransform.create(
			new Rectangle2D.Float(size.width/2f,0, size.width/2f, size.height),
			rect2
		);
		
		return new Transition2DInstruction[] {
				new ImageInstruction(false),
				new ImageInstruction(true,transform1,rect1),
				new ImageInstruction(true,transform2,rect2)
		};
	}
	
	@Override
	public String toString() {
		return "Curtain";
	}

}
