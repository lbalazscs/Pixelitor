/*
 * @(#)MirageTransition2D.java
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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;
import java.util.Vector;

/** In a square-ish way, this resembles an image coming into focus like
 * a mirage in a desert.  Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/MirageTransition2D/Mirage.gif" alt="MirageTransition2D Demo">
 * 
 */
public class MirageTransition2D extends Transition2D {
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		float ySize = (size.height)*.1f;
		float xSize = (size.width)*.1f;
		Vector<Rectangle2D> v = new Vector<Rectangle2D>();
		for(float y = 0; y<size.height; y+=ySize) {
			for(float x = 0; x<size.width; x+=xSize) {
				v.add(new Rectangle2D.Float(x,y,xSize,ySize));
			}
		}
		Transition2DInstruction[] instr = new Transition2DInstruction[2*v.size()];
		Random random = new Random();
		Point2D center = new Point2D.Double(size.width/2,size.height/2);
		double max = Math.sqrt( size.width*size.width/4 + size.height*size.height/4 );
		Point2D p1, p2;
		try {
			for(int a = 0; a<v.size(); a++) {
				float progress2 = (float)Math.pow(progress, .9+.2*random.nextFloat());
				Rectangle2D r = v.get(a);
				AffineTransform transform = new AffineTransform();
				Shape shape;
				
				transform.setToRotation(-Math.PI+Math.PI*(1-progress2),size.width/2,size.height/2);
				p1 = new Point2D.Double(r.getCenterX(),r.getCenterY());
				p2 = new Point2D.Double();
				transform.transform(p1,p2);
				//transform.setToTranslation(p1.getX()-p2.getX(),p1.getY()-p2.getY());
				transform.setToTranslation(p2.getX()-p1.getX(),p2.getY()-p1.getY());
				shape = transform.createInverse().createTransformedShape(r);
				if(a==0) {
					instr[a] = new ImageInstruction(true);
				} else {
					instr[2*a+0] = new ImageInstruction(true,0,transform,shape);
				}
				transform.setToRotation(-1*Math.PI*(1-progress),size.width/2,size.height/2);
				transform.translate(size.width/2, size.height/2);
				transform.scale(1/progress2, 1/progress2);
				transform.translate(-size.width/2, -size.height/2);
				
				p1 = new Point2D.Double(r.getCenterX(),r.getCenterY());
				p2 = new Point2D.Double();
				transform.transform(p1,p2);
				//transform.setToTranslation(p1.getX()-p2.getX(),p1.getY()-p2.getY());
				transform.setToTranslation(p2.getX()-p1.getX(),p2.getY()-p1.getY());
				shape = r; //transform.createInverse().createTransformedShape(r);
				float progress3 = (float)((1-progress2)*(1-p1.distance(center)/max));
                /** This doesn't look terrible if you don't use opacity... but it looks better with it...
                 * 
                 */
				//instr[2*a+1] = new ImageInstruction(false,transform.createInverse(),shape);
                 instr[2*a+1] = new ImageInstruction(false,1-progress3,transform.createInverse(),shape);
			}
		} catch(Exception e) {
			
		}
		return instr;
	}
	
	@Override
	public String toString() {
		return "Mirage";
	}

}
