/*
 * @(#)Transition2D.java
 *
 * $Date: 2014-04-11 08:25:32 +0200 (P, 11 ápr. 2014) $
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
import java.awt.image.BufferedImage;


/** This uses Java2D operations to transition between two images.
 * Each operation is wrapped up in a Transition2DInstruction object.
 * 
 */
public abstract class Transition2D extends AbstractTransition {

	/** This determines how to transition from A to B.
	 * @param progress a float from [0,1].  When this is zero, these instructions should
	 * render the initial frame.  When this is one, these instructions should render
	 * the final frame.
	 *
	 */
	public abstract Transition2DInstruction[] getInstructions(float progress,Dimension size);

	/** This calls Transition2DInstruction.paint() for each instruction
	 * in this transition.
	 * <P>It is made final to reinforce that the instructions should be
	 * all that is needed to implement these transitions.
	 */
	protected final void doPaint(Graphics2D g,BufferedImage frameA,BufferedImage frameB,float progress) {
		Transition2DInstruction[] i = getInstructions(progress,new Dimension(frameA.getWidth(),frameA.getHeight()));
		for(int a = 0; a<i.length; a++) {
			i[a].paint(g, frameA, frameB);
		}
	}
}
