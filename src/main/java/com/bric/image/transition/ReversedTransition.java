/*
 * @(#)ReversedTransition2D.java
 *
 * $Date: 2014-05-04 18:08:30 +0200 (V, 04 máj. 2014) $
 *
 * Copyright (c) 2014 by Jeremy Wood.
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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** A Transition that plays another transition in reverse.
 */
public class ReversedTransition implements Transition {
	Transition transition;
	
	public ReversedTransition(Transition t) {
		transition = t;
	}

	@Override
	public void paint(Graphics2D g, BufferedImage frameA, BufferedImage frameB,
			float progress) {
		transition.paint(g, frameB, frameA, 1-progress);
	}
	
}
