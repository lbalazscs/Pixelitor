/*
 * @(#)BlendTransition2D.java
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

/** Also know as the "Fade" transition, this simply fades in the opacity of the incoming
 * frame. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/BlendTransition2D/Blend.gif" alt="BlendTransition2D demo">
 *
 */
public class BlendTransition2D extends Transition2D {

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		return new Transition2DInstruction[] {
				new ImageInstruction(true),
				new ImageInstruction(false,progress,null,null)
		};
	}
	
	@Override
	public String toString() {
		return "Blend";
	}

}
