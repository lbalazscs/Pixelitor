/*
 * @(#)Transition2DInstruction.java
 *
 * $Date: 2014-03-13 09:15:48 +0100 (Cs, 13 márc. 2014) $
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Vector;

import com.bric.geom.EmptyPathException;
import com.bric.geom.ShapeBounds;

/** This represents one "layer" of a frame of a transition.
 * <P>The goal of keeping these instructions abstract is that
 * they can be interpreted by other mechanisms (such as a Flash encoder).
 * <P>Any one frame of a Transition2D object is defined as a list
 * of instructions.  When these instructions are painted (in order,
 * on top of each other) they create the desired effect.
 */
public abstract class Transition2DInstruction {
	
	/** This renders this instruction. */
	public abstract void paint(Graphics2D g,BufferedImage frameA,BufferedImage frameB);
	
	/** This examines the instructions provided and returns only those instructions
	 * that are guaranteed to be visible.
	 * <P>It is recommended that this method be called by encoders and not by Transition2D
	 * objects directly.  For example, it may be advantageous to always include 20
	 * instructions, even if some instructions do not make a visual difference, because some
	 * day an encoder might be more interested in tweening those instructions than writing
	 * separate frames.
	 * 
	 * @param instr the list of instructions to filter
	 * @param frameSize the dimensions the transition 
	 * @return the subset of instructions that are guaranteed to be visible
	 */
	public static Transition2DInstruction[] filterVisibleInstructions(Transition2DInstruction[] instr,Dimension frameSize) {
		Vector<Transition2DInstruction> v = new Vector<Transition2DInstruction>();

		Rectangle2D movieRect = new Rectangle(0,0,frameSize.width,frameSize.height);
		Area movieArea = new Area(movieRect);
		for(int a = 0; a<instr.length; a++) {
			Transition2DInstruction i = instr[a];
			
			//check #1: if it's a shape instruction: are its elements visible?
			if(i instanceof ShapeInstruction) {
				ShapeInstruction i2 = (ShapeInstruction)i;
				if( (i2.fillColor==null || i2.fillColor.getAlpha()<5) &&
						(i2.strokeColor==null || i2.strokeWidth==0 || i2.strokeColor.getAlpha()<5)) {
					//this instruction is invisible:
					i = null;
				}
			}
			
			//check #2: if it's a translucent image: is it visible?
			if(i instanceof ImageInstruction) {
				ImageInstruction i2 = (ImageInstruction)i;
				if( i2.opacity<.05f ) {
					//this instruction is invisible:
					i = null;
				}
			}
			
			
			//check #3: is this instruction on screen?  If not, let's not write it.
			//this saves space.
			//However, to save time (against the infamously slow Area class), we'll
			//break this into a couple of different checks:
			Shape instructionShape = null;
			if(i instanceof ImageInstruction) {
				ImageInstruction i2 = (ImageInstruction)i;
				instructionShape = i2.clipping;
				
				//this is a special case, indicating that there is no clipping,
				//so everything should be drawn:
				if(instructionShape==null) {
                    v.add(i);
					break;
                }
			} else if(i instanceof ShapeInstruction) {
				ShapeInstruction i2 = (ShapeInstruction)i;
				instructionShape = i2.shape;
			}
            if(instructionShape!=null) {
        			try {
        				Rectangle2D instructionRect = ShapeBounds.getBounds(instructionShape);
        				if(movieRect.contains(instructionRect)) {
        					//definitely add
        					v.add(i);
        				} else if(!movieRect.intersects(instructionRect)) {
        					//definitely do not add: nothing to do here.
        				} else {
        					//further examination is necessary:
        					Area instructionArea = new Area(instructionShape);
        					instructionArea.intersect(movieArea);
        					if(instructionArea.isEmpty()==false) {
        						v.add(i);
        					}
        				}
        			} catch(EmptyPathException e) {
        				//we're good... no need to add this instruction since it has no shape
        			}
            }
		}
		
		return v.toArray(new Transition2DInstruction[v.size()]);
	}
}
