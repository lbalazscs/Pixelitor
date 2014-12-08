/*
 * @(#)MotionBlendTransition2D.java
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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import com.bric.geom.RectangularTransform;

/** This is a blend transition with a subtle zoom in/out added. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/MotionBlendTransition2D/MotionBlend.gif" alt="MotionBlendTransition2D Demo">
*/
public class MotionBlendTransition2D extends Transition2D {

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		//the logic here is copied & pasted from Tech4Learning code
		//(used with permission):
		Vector<ImageInstruction> v = new Vector<ImageInstruction>();
		
		int max_wchange = size.width/4;
		int max_hchange = size.height/4;

		int x2 = (int)(-(max_wchange*(1.0f-progress)));
		int y2 = (int)(-(max_hchange*(1.0f-progress)));
		int w2 = (int)(size.width+(max_wchange*(1.0f-progress)));
		int h2 = (int)(size.height+(max_hchange*(1.0f-progress)));
		AffineTransform transform = RectangularTransform.create(
				new Rectangle(0,0,size.width,size.height),
				new Rectangle(x2,y2,w2-x2,h2-y2)
		);
		v.add(new ImageInstruction(true,1,transform,null));

		x2 = (int)(-(max_wchange*progress));
		y2 = (int)(-(max_hchange*progress));
		w2 = (int)(size.width+(max_wchange*progress));
		h2 = (int)(size.height+(max_hchange*progress));
		transform = RectangularTransform.create(
				new Rectangle(0,0,size.width,size.height),
				new Rectangle(x2,y2,w2-x2,h2-y2)
		);
		v.add(new ImageInstruction(false,1-progress,transform,null));
		
		return v.toArray(new ImageInstruction[v.size()]);
	}
	
	@Override
	public String toString() {
		return "Motion Blend";
	}
}
