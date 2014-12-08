/*
 * @(#)SquaresTransition2D.java
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
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import com.bric.geom.RectangularTransform;

/** In this transition the current frame splits off into several
 * tiles that calmly "explode" towards the viewer, revealing the
 * next frame underneath. Here are playback samples:
 * <p><table summary="Sample Animations of SquaresTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SquaresTransition2D/Squares(10x10).gif" alt="Squares (10x10)">
 * <p>Squares (10x10)
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SquaresTransition2D/Squares(20x20).gif" alt="Squares (20x20)">
 * <p>Squares (20x20)
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/SquaresTransition2D/Squares(30x30).gif" alt="Squares (30x30)">
 * <p>Squares (30x30)
 * </td>
 * </tr></table>
 *
 */
public class SquaresTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new SquaresTransition2D(10, 10),
				new SquaresTransition2D(30, 30),
				new SquaresTransition2D(20, 20),
		};
	}
	
	Comparator<ImageInstruction> comparator = new Comparator<ImageInstruction>() {

		public int compare(ImageInstruction i1, ImageInstruction i2) {
			if(i1.isFirstFrame && i2.isFirstFrame==false)
				return 1;
			if(i2.isFirstFrame && i1.isFirstFrame==false)
				return -1;
			
			double d1 = i1.transform.getDeterminant();
			double d2 = i2.transform.getDeterminant();
			if(d1<d2) {
				return -1;
			}
			return 1;
		}
		
	};
	
	float[][] accels;
	float[][] delays;
	float progressMax = 1;
	/** Creates a new squares transition with 10 rows and columns. */
	public SquaresTransition2D() {
		this(10,10);
	}
	
	/** Creates a new squares transition.
	 * 
	 * @param columns the number of columns
	 * @param rows the number of rows
	 */
	public SquaresTransition2D(int columns,int rows) {
		delays = new float[columns][rows];
		accels = new float[columns][rows];
		Random random = new Random();
		for(int x = 0; x<columns; x++) {
			for(int y = 0; y<rows; y++) {
				float offset = (y-rows/2)*(y-rows/2)+(x-columns/2)*(x-columns/2);
				offset = offset/(rows*rows/4+columns*columns/4);
				//float offset = ((float)(y+x))/((float)(rows+columns));
				delays[x][y] = .3f*offset+.1f*random.nextFloat();
				accels[x][y] = (.5f+.8f*random.nextFloat());
			}
		}
		progressMax = findMax(0,2);
	}
	
	protected float findMax(float t0,float t1) {
		if(t1-t0<.0001) return Math.max(t0,t1);
		
		Rectangle2D r = new Rectangle2D.Float(0,0,100,100);
		float mid = t0/2f+t1/2f;
		Transition2DInstruction[] instrA = getInstructions(t0,new Dimension(100,100));
		Transition2DInstruction[] instrB = getInstructions(mid,new Dimension(100,100));
		Transition2DInstruction[] instrC = getInstructions(t1,new Dimension(100,100));
		boolean validA = false;
		boolean validB = false;
		boolean validC = false;
		for(int a = 1; a<instrA.length; a++) {
			if(r.intersects((Rectangle2D)((ImageInstruction)instrA[a]).clipping)) {
				validA = true;
			}
			if(r.intersects((Rectangle2D)((ImageInstruction)instrB[a]).clipping)) {
				validB = true;
			}
			if(r.intersects((Rectangle2D)((ImageInstruction)instrC[a]).clipping)) {
				validC = true;
			}
		}
		if(validA && validC)
			return Math.max(t0,t1);
		if(validA) {
			if(validB) {
				return findMax(mid,t1);
			} else {
				return findMax(t0,mid);
			}
		} else {
			throw new RuntimeException();
		}
	}
	
	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		progress = progress*progressMax;
		
		int columns = accels.length;
		int rows = accels[0].length;
		ImageInstruction[] instr = new ImageInstruction[columns*rows+1];
		instr[0] = new ImageInstruction(false);
		float columnWidth = ((float)size.width)/((float)columns);
		float rowHeight = ((float)size.height)/((float)rows);
		int ctr = 0;
		for(int x = 0; x<columns; x++) {
			for(int y = 0; y<rows; y++) {
				float delay = delays[x][y];
				float accel = accels[x][y];
				float t = progress-delay;
				if(t<0)
					t = 0;
				float z = 1+120*accel*t*t;
				Rectangle2D r = new Rectangle2D.Float(x*columnWidth,y*rowHeight,columnWidth,rowHeight);
				RectangularTransform transform = new RectangularTransform();

				float centerX = size.width/2;
				float centerY = size.height/2;
				
				transform.translate(centerX,centerY);
				transform.scale(z,z);
				double dx = centerX-r.getCenterX();
				double dy = centerY-r.getCenterY();
				transform.translate(-centerX-10*t*dx*progress,-centerY-10*t*dy*progress);
				
				Rectangle2D clip = transform.transform(r);
				instr[1+(ctr++)] = new ImageInstruction(true,transform.createAffineTransform(),clip);
			}
		}
		Arrays.sort(instr,comparator);
		return instr;
	}
	
	@Override
	public String toString() {
		return "Squares ("+delays[0].length+"x"+delays.length+")";
	}

}
