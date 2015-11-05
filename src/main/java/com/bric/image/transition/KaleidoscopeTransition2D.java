/*
 * @(#)KaleidoscopeTransition2D.java
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

/** This resembles looking through a kaleidoscope. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/KaleidoscopeTransition2D/Kaleidoscope.gif" alt="KaleidoscopeTransition2D Demo">
 */
public class KaleidoscopeTransition2D extends Transition2D {
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		Vector<Rectangle2D> v1 = new Vector<Rectangle2D>();
		Vector<Transition2DInstruction> v2 = new Vector<Transition2DInstruction>();

		float factor = .2f;

		float ySize = (size.height)*factor;
		float xSize = (size.width)*factor;
		for(float y = 0; y<size.height; y+=ySize) {
			for(float x = 0; x<size.width; x+=xSize) {
				v1.add(new Rectangle2D.Float(x,y,xSize,ySize));
			}
		}

		progress = 1-progress;

		Point2D p1, p2;
		v2.add( new ImageInstruction(false) );
		float dx = 10; //(float)(0*FastMath.cos(2*(1-progress)*Math.PI));
		float dy = 10; //(float)(0*FastMath.sin(2*(1-progress)*Math.PI));
		for(int a = 0; a<v1.size(); a++) {
			try {
				Rectangle2D r = v1.get(a);
				p1 = new Point2D.Double(r.getCenterX(),r.getCenterY());
				AffineTransform transform = new AffineTransform();

				transform.setToRotation(-2*Math.PI*(1-Math.sqrt(progress)),size.width/2+dx,size.height/2+dy);
				transform.translate(size.width/2, size.height/2);
				transform.scale(progress*progress, progress*progress);
				transform.translate(-size.width/2, -size.height/2);

				p2 = new Point2D.Double();
				transform.transform(p1,p2);
				transform.setToTranslation(p2.getX()-p1.getX(),p2.getY()-p1.getY());
				v2.add( new ImageInstruction(false,progress,transform.createInverse(),r) );

				transform.setToRotation(-2*Math.PI*(1-Math.sqrt(progress)),size.width/2+dx,size.height/2+dy);

				p2 = new Point2D.Double();
				transform.transform(p1,p2);
				transform.setToTranslation(p2.getX()-p1.getX(),p2.getY()-p1.getY());
				v2.add( new ImageInstruction(true,progress*progress,transform.createInverse(),r) );
			} catch(Exception e) {

			}
		}
		return v2.toArray(new Transition2DInstruction[v2.size()]);
	}

	@Override
	public String toString() {
		return "Kaleidoscope";
	}

}
