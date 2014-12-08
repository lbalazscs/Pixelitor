/*
 * @(#)FlurryTransition2D.java
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

import com.bric.geom.ShapeBounds;
import com.bric.geom.TransformUtils;

/** In this transition one image breaks into several smaller tiles
 * and then is whisked away as if by a wind. Or, seen backwards, a flurry of incoming tiles assemble into
 * the incoming image.
 * <p>Here are playback samples:
 * <p><table summary="Sample Animations of FlurryTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlurryTransition2D/FlurryIn.gif" alt="Flurry In">
 * <p>Flurry In
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlurryTransition2D/FlurryOut.gif" alt="Flurry Out">
 * <p>Flurry Out
 * </td>
 * </tr></table>
 */
public class FlurryTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new FlurryTransition2D(IN), 
				new FlurryTransition2D(OUT)
		};
	}
    
    int type = OUT;
	
	Comparator<ImageInstruction> comparator = new Comparator<ImageInstruction>() {

		public int compare(ImageInstruction i1, ImageInstruction i2) {
			if(i1.isFirstFrame && i2.isFirstFrame==false)
				return 1;
			if(i2.isFirstFrame && i1.isFirstFrame==false)
				return -1;
			
			Rectangle2D r1 = ShapeBounds.getBounds(i1.clipping);
			Rectangle2D r2 = ShapeBounds.getBounds(i2.clipping);
			double area1 = r1.getWidth()*r1.getHeight();
			double area2 = r2.getWidth()*r2.getHeight();
			if(area1<area2) {
				return -1;
			}
			return 1;
		}
		
	};
    
	/** Creates a new flurry transition that moves out. 
	 */
    public FlurryTransition2D() {
        this(OUT);
    }
    
    /** Creates a new flurry transition
     * 
     * @param type must be OUT or IN
     */
    public FlurryTransition2D(int type) {
        if(!(type==OUT || type==IN)) {
            throw new IllegalArgumentException("This transition must use OUT or IN.");
        }
        this.type = type;
    }
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
        if(type==IN) {
            progress = 1-progress;
        }
		
		progress = progress*.78f;
		
		Vector<Rectangle2D> v1 = new Vector<Rectangle2D>();
		
		float yHeight = 200f/10f;
		float xWidth = 200f/10f;
		for(float y = 0; y<200; y+=yHeight) {
			for(float x = 0; x<200; x+=xWidth) {
				Rectangle2D r = new Rectangle2D.Double(x,y,xWidth,yHeight);
				v1.add(r);
			}
		}
		
		progress = (float)Math.pow(progress, 1);
        ImageInstruction[] instr = new ImageInstruction[v1.size()+1];
		instr[0] = new ImageInstruction(false);
		Random random = new Random();
		for(int a = 0; a<v1.size(); a++) {
			Rectangle2D r = v1.get(a);
			random.setSeed(a);
			Shape clipping = r;
			Point2D center = new Point2D.Double(r.getCenterX()-200f/2f,r.getCenterY()-200f/2f);
			float k = (float)(Math.sqrt(center.getX()*center.getX()+center.getY()*center.getY())/Math.sqrt(200f*200f/4+200f*200f/4));
			k = (1-progress)*k+progress;
			float scaleProgress = (float)Math.pow(2*progress*k,.02+4*random.nextFloat());
			AffineTransform transform = new AffineTransform();
			transform.translate(200f/2,200f/2);
			transform.scale(1+2*scaleProgress,1+2*scaleProgress);
			transform.rotate(progress);
			transform.translate(-200f/2,-200f/2);
			Point2D p1 = new Point2D.Double(r.getCenterX(),r.getCenterY());
			Point2D p2 = new Point2D.Double();
			Point2D p3 = new Point2D.Double();
			transform.transform(p1,p2);
			
			double dx = -(p1.getX()-p2.getX());
			double dy = -(p1.getY()-p2.getY());
			transform.setToIdentity();
			transform.concatenate(TransformUtils.createAffineTransform(
					0,0,
					0,200,
					200,0,
					0,0,
					0,size.height,
					size.width,0
			));
			transform.scale( 1+Math.abs(dx)/15f, 1+Math.abs(dy)/15f );
			transform.rotate(progress);
			transform.translate(dx,dy);
			
			clipping = transform.createTransformedShape(clipping);
			
			p1.setLocation(r.getX(),r.getY());
			p2.setLocation(r.getX()+r.getWidth(),r.getY());
			p3.setLocation(r.getX(),r.getY()+r.getHeight());
			transform.transform(p1, p1);
			transform.transform(p2, p2);
			transform.transform(p3, p3);
			
			transform = TransformUtils.createAffineTransform(
					r.getX()*size.width/200f, r.getY()*size.height/200f,
					(r.getX()+r.getWidth())*size.width/200f, r.getY()*size.height/200f,
					r.getX()*size.width/200f, (r.getY()+r.getHeight())*size.height/200f,
					
					p1.getX(), p1.getY(),
					p2.getX(), p2.getY(),
					p3.getX(), p3.getY()
			);
			
			instr[a+1] = new ImageInstruction(true,transform,clipping);
		}
		Arrays.sort(instr,comparator);
        if(type==IN) {
            for(int a = 0; a<instr.length; a++) {
                instr[a].isFirstFrame = !instr[a].isFirstFrame;
            }
        }
		return instr;
	}
	
	@Override
	public String toString() {
        if(type==OUT) {
            return "Flurry Out";
        } else {
            return "Flurry In";
        }
	}

}
