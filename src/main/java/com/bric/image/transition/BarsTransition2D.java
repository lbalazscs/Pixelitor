/*
 * @(#)BarsTransition2D.java
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
import java.util.Random;
import java.util.Vector;

/** This is a series of random bars that increase in frequency, slowly revealing
 * the new frame. Here are playback samples:
 * <p><table summary="Sample Animations of BarsTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/BarsTransition2D/BarsHorizontalRandom.gif" alt="Bars Horizontal Random">
 * <p>Bars Horizontal Random
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/BarsTransition2D/BarsHorizontal.gif" alt="Bars Horizontal">
 * <p>Bars Horizontal
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/BarsTransition2D/BarsVerticalRandom.gif" alt="Bars Vertical Random">
 * <p>Bars Vertical Random
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/BarsTransition2D/BarsVertical.gif" alt="Bars Vertical">
 * <p>Bars Vertical
 * </td>
 * </tr></table>
 *
 */
public class BarsTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new BarsTransition2D(HORIZONTAL, true),
				new BarsTransition2D(HORIZONTAL, false),
				new BarsTransition2D(VERTICAL, true),
				new BarsTransition2D(VERTICAL, false)
		};
	}
	
	/** Keep a truly random seed; constantly creating a new
	 * Random object with the current time as its seed created
	 * several non-random frames generated in the same millisecond
	 * 
	 */
	static Random random = new Random(System.currentTimeMillis());
	
	int type;
	boolean isRandom;
	
	/** Creates a randomized horizontal BarsTransition2D
	 * 
	 */
	public BarsTransition2D() {
		this(HORIZONTAL,true);
	}
	
	/** Creates a BarsTransition2D.
	 * 
	 * @param type must be HORIZONTAL or VERTICAL
	 * @param random whether each frame is 100% random, or whether the
	 * bars are cumulative as the transition progresses.
	 */
	public BarsTransition2D(int type,boolean random) {
		if(!(type==HORIZONTAL || type==VERTICAL)) {
			throw new IllegalArgumentException("Type must be HORIZONTAL or VERTICAL.");
		}
		this.type = type;
		this.isRandom = random;
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		boolean[] k;
		if(type==HORIZONTAL) {
			k = new boolean[size.height];
		} else {
			k = new boolean[size.width];
		}
		Random r;
		if(isRandom) {
			r = random;
		} else {
			r = new Random(0);
		}
		for(int a = 0; a<k.length; a++) {
			k[a] = r.nextFloat()>progress;
		}
		Vector<Transition2DInstruction> v = new Vector<Transition2DInstruction>();
		v.add(new ImageInstruction(false));
		if(type==HORIZONTAL) {
			int a = 0;
			while(a<k.length) {
				int run = 0;
				while(a+run<k.length && k[a+run]) {
					run++;
				}
				if(run!=0) {
					Rectangle2D r2 = new Rectangle2D.Float(0,a,size.width,run);
					v.add(new ImageInstruction(true, null, r2));
					a+=run;
				}
				a++;
			}
		} else {
			int a = 0;
			while(a<k.length) {
				int run = 0;
				while(a+run<k.length && k[a+run]) {
					run++;
				}
				if(run!=0) {
					Rectangle2D r2 = new Rectangle2D.Float(a,0,run,size.height);
					v.add(new ImageInstruction(true, null, r2));
					a+=run;
				}
				a++;
			}
		}
		return v.toArray(new Transition2DInstruction[v.size()]);
	}
	
	@Override
	public String toString() {
		if(type==HORIZONTAL) {
			if(isRandom) {
				return "Bars Horizontal Random";
			}
			return "Bars Horizontal";
		}
		if(isRandom) {
			return "Bars Vertical Random";
		}
		return "Bars Vertical";
	}
}
