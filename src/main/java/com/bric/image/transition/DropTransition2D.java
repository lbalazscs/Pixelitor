/*
 * @(#)DropTransition2D.java
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

/** This is basically a "slide down" transition, but with a bounce at the bottom. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/DropTransition2D/Drop.gif" alt="DropTransition2D Demo">
 */
public class DropTransition2D extends Transition2D {

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		float dy;
		if(progress<.8) {
			progress = progress/.8f;
			dy = -progress*progress+1;
			dy = 1-dy;
		} else {
			progress = (progress-.8f)/.2f;
			dy = -4*(progress-.5f)*(progress-.5f)+1;
			dy = 1-dy*.1f;
		}
		AffineTransform transform = AffineTransform.getTranslateInstance(0,dy*size.height-size.height);
		
		return new ImageInstruction[] {
				new ImageInstruction(true),
				new ImageInstruction(false,transform,null)
		};
	}
	
	@Override
	public String toString() {
		return "Drop";
	}
}
