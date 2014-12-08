/*
 * @(#)SquareRainTransition2D.java
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
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import java.util.Vector;

import com.bric.geom.TransformUtils;

/** In this transition geometric shapes (squares and circles)
 * trickle downwards revealing the next frame. Here are playback samples:
 * <p><table summary="Sample Animations of SquareRainTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SquareRainTransition2D/SquareRain(6).gif" alt="Square Rain (6)">
 * <p>Square Rain (6)
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SquareRainTransition2D/SquareRain(12).gif" alt="Square Rain (12)">
 * <p>Square Rain (12)
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SquareRainTransition2D/SquareRain(24).gif" alt="Square Rain (24)">
 * <p>Square Rain (24)
 * </td>
 * </tr></table>
 *
 */
public class SquareRainTransition2D extends AbstractClippedTransition2D {

	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new SquareRainTransition2D(6, false),
				new SquareRainTransition2D(12, false),
				new SquareRainTransition2D(24, false)
		};
	}

	float[] offset;
	float[] accel;
	
	/** Creates a new square rain transition with 12 columns that is not randomized.
	 * 
	 */
	public SquareRainTransition2D() {
		this(12,false);
	}

	/** Creates a new square rain transition.
	 * 
	 * @param columns how many columns to use
	 * @param randomize whether the transition should be randomly generated or not.
	 * If not, a constant random seed is used.
	 */
	public SquareRainTransition2D(int columns,boolean randomize) {
		Random r = new Random();
		offset = new float[columns];
		accel = new float[columns];
		boolean ok = false;
        
        long seed = 1196622174915L;
        if(randomize) {
            seed = System.currentTimeMillis();
        }
		while(!ok) {
			seed++;
			r.setSeed(seed);
			ok = true;
			for(int a = 0; a<columns && ok; a++) {
				float m = (a)/(columns-1f);
				if(m<.5f) {
					m = m/.5f;
				} else {
					m = (1-m)/.5f;
				}
				offset[a] = -m;
				accel[a] = 10*r.nextFloat();
				if(accel[a]+1+offset[a]<1.2)
					ok = false;
			}
			if(ok) {
				//make sure at least one strand takes up the whole time t=[0,1]
				boolean atLeastOneSlowOne = false;
				for(int a = 0; a<columns && atLeastOneSlowOne==false; a++) {
					atLeastOneSlowOne = accel[a]+1+offset[a]<1.3;
				}
				ok = atLeastOneSlowOne;
			}
		}
	}
	
	@SuppressWarnings("unused")
	@Override
	public Shape[] getShapes(float progress, Dimension size) {
		Vector<Shape> v = new Vector<Shape>();
		Rectangle2D rect;
		float columnWidth = ((float)size.width)/((float)offset.length);
		int rows = (int)(size.height/columnWidth+.5);
		float rowHeight = ((float)size.height)/rows;
		for(int a = 0; a<offset.length; a++) {
			float x = a*columnWidth;
			float centerX = x+columnWidth/2;
			float w = size.width/offset.length;
			float y = size.height*(offset[a]+progress+progress*progress*accel[a]);
			
			int row = (int)((y-2*rowHeight)/rowHeight);
			
			
			rect = new Rectangle2D.Float(x-1,0,w+2,row*rowHeight);
			v.add(rect);
			float centerY = row*rowHeight+rowHeight/2;
			float k = ((y-rowHeight*row))/((rowHeight));
			
			float k1 = k/3f;
			float k2 = (k-1f)/3f;
			float k3 = (k-2f)/3f;
			if(k1<0) k1 = 0;
			if(k2<0) k2 = 0;
			if(k3<0) k3 = 0;
			if(k1>1) k1 = 1;
			if(k2>1) k2 = 1;
			if(k3>1) k3 = 1;

			if(true) {
				if(k1>0) {
					Shape shape = new RoundRectangle2D.Float(centerX-k1*columnWidth/2,centerY-k1*rowHeight/2,k1*columnWidth,k1*rowHeight,columnWidth/4*(1-k1),rowHeight/4*(1-k1));
					v.add(shape);
				}
				if(k2>0) {
					Shape shape = new RoundRectangle2D.Float(centerX-k2*columnWidth/2,centerY-k2*rowHeight/2+rowHeight,k2*columnWidth,k2*rowHeight,columnWidth*(1-k2),rowHeight*(1-k2));
					v.add(shape);
				}
				if(k3>0) {
					Shape shape = new RoundRectangle2D.Float(centerX-k3*columnWidth/2,centerY-k3*rowHeight/2+2*rowHeight,k3*columnWidth,k3*rowHeight,columnWidth*(1-k3),rowHeight*(1-k3));
					v.add(shape);
				}
			} else {
				if(k1>0) {
					Shape shape = new Rectangle2D.Float(centerX-k1*columnWidth/2,centerY-k1*rowHeight/2,k1*columnWidth,k1*rowHeight);
					v.add(shape);
				}
				if(k2>0) {
					Shape shape = new Rectangle2D.Float(centerX-k2*columnWidth/2,centerY-k2*rowHeight/2+rowHeight,k2*columnWidth,k2*rowHeight);
					v.add(shape);
				}
				if(k3>0) {
					Shape shape = new Rectangle2D.Float(centerX-k3*columnWidth/2,centerY-k3*rowHeight/2+2*rowHeight,k3*columnWidth,k3*rowHeight);
					v.add(shape);
				}
			}
		}
		
		if(false) {
			AffineTransform transform = TransformUtils.createAffineTransform(
					0,0,
					size.width,0,
					0,size.height,
					0,size.height,
					size.width,size.height,
					0,0 );
			for(int a = 0; a<v.size(); a++) {
				Shape shape = v.get(a);
				shape = transform.createTransformedShape(shape);
				v.set(a, shape);
			}
		}
		
		Shape[] shapes = v.toArray(new Shape[v.size()]);
        
        //make sure the stroke doesn't show:
        float k = getStrokeWidth(progress, size)+1;
        
        AffineTransform fit = TransformUtils.createAffineTransform(
                0,0,
                size.width,0,
                0,size.height,
                -k,-k,
                size.width+k,-k,
                -k,size.height+k
        );
        for(int a = 0; a<shapes.length; a++) {
            if(shapes[a] instanceof GeneralPath) {
                ((GeneralPath)shapes[a]).transform(fit);
            } else {
                shapes[a] = fit.createTransformedShape(shapes[a]);
            }
        }
        return shapes;
	}

	@Override
	public float getStrokeWidth(float progress,Dimension size) {
		return 5;
	}
	
	@Override
	public String toString() {
		return "Square Rain ("+offset.length+")";
	}

}
