/*
 * @(#)MicroscopeTransition2D.java
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

/** This transition "zooms in" on the existing image into the incoming image
 * comes into view.  (The incoming image is overlayed on the pixels of the
 * original image with increasing opacity.) Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/MicroscopeTransition2D/Microscope.gif" alt="MicroscopeTransition2D Demo">
 * 
 */
public class MicroscopeTransition2D extends Transition2D {
	float min = .05f;
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		progress = (progress*progress*progress)*6f/7f+progress/7f;
			//(.5f*(2*progress-1)*(2*progress-1)*(2*progress-1)+.5f)/2f+progress/2f;
		
		//so here at progress = 0 each "tile" is 1 pixel,
		//and at progress = 1, each tile is "size".
		float tileWidth = (size.width-1)*progress+1;
		float tileHeight = (size.height-1)*progress+1;
		
		float startX = size.width/2;
		float startY = size.height/2;
		while(startX>0) startX-=tileWidth;
		while(startY>0) startY-=tileHeight;
		
		
		Vector<Rectangle2D> v = new Vector<Rectangle2D>();
		if(progress>min) {
			for(float y = startY-tileHeight/2; y<size.height; y+=tileHeight) {
				for(float x = startX-tileWidth/2; x<size.width; x+=tileWidth) {
					v.add(new Rectangle2D.Double(x,y,tileWidth,tileHeight));
				}
			}
		}
		
		Rectangle2D[] r = v.toArray(new Rectangle2D[v.size()]);
		Transition2DInstruction[] instr = new Transition2DInstruction[1+r.length];
		
		Rectangle2D bigRect = new Rectangle2D.Double(0,0,tileWidth*size.width,tileHeight*size.height);
		bigRect.setFrame(size.getWidth()/2-bigRect.getWidth()/2,
				size.getHeight()/2-bigRect.getHeight()/2,
				bigRect.getWidth(),bigRect.getHeight());
		
		instr[0] = new ImageInstruction(true,bigRect,size,null);
		float maxOpacity = (progress-min)/(1-min);
		for(int a = 0; a<r.length; a++) {
			float multiplier = (float)Point2D.distance(size.getWidth()/2, size.getHeight()/2, r[a].getCenterX(),r[a].getCenterY());
			multiplier = (multiplier/100+1);
			float opacity = maxOpacity/multiplier;
			
			instr[a+1] = new ImageInstruction(false,opacity,r[a],size,null);
		}
		return instr;
	}
	
	@Override
	public String toString() {
		return "Microscope";
	}

	
}
