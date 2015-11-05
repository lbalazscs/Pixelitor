/*
 * @(#)FunkyWipeTransition2D.java
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

import com.bric.geom.Clipper;
import com.bric.geom.MeasuredShape;
import com.bric.geom.RectangularTransform;
import net.jafama.FastMath;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** This is a fun variation of a "Wipe" transition.  The line
 * that separates the two frames spins as it slides.  The
 * circular wipe is especially interesting because it begins and ends
 * on the same side of the frame.
 * <P>(This is loosely based on a transition I saw while
 * watching Royal Pains on Hulu one weekend...)
 * <p>Here are playback samples:
 * <p><table summary="Sample Animations of FunkyWipeTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FunkyWipeTransition2D/FunkyWipeCircular.gif" alt="Funky Wipe Circular">
 * <p>Funky Wipe Circular
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FunkyWipeTransition2D/FunkyWipeAcross.gif" alt="Funky Wipe Across"> 
 * <p>Funky Wipe Across
 * </td>
 * </tr></table>
 *
 */
public class FunkyWipeTransition2D extends Transition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new FunkyWipeTransition2D(true), 
				new FunkyWipeTransition2D(false)
		};
	}
	
	private static final GeneralPath pathCyclic = createPathCyclic();
	private static final MeasuredShape measuredPathCyclic = new MeasuredShape(pathCyclic);
	private static final GeneralPath pathAcross = createPathAcross();
	private static final MeasuredShape measuredPathAcross = new MeasuredShape(pathAcross);
	
	private static GeneralPath createPathCyclic() {
		GeneralPath p = new GeneralPath();
		p.moveTo(99.936f, 51.019f);
		p.curveTo(99.936f, 51.019f, 78.316f, 86.931f, 51.019f, 89.745f);
		p.curveTo(23.721f, 92.559f, -2.012f, 75.843f, 11.082f, 61.21f);
		p.curveTo(4.178f, 46.576f, 34.931f, 39.565f, 62.229f, 36.751f);
		p.curveTo(89.526f, 33.937f, 99.936f, 51.019f, 99.936f, 51.019f);
		return p;
	}
	private static GeneralPath createPathAcross() {
		GeneralPath p = new GeneralPath();
		p.moveTo(99.936f, 21.019f);
		p.curveTo(99.936f, 51.019f, 78.316f, 86.931f, 51.019f, 89.745f);
		p.curveTo(23.721f, 92.559f, -2.012f, 75.843f, 0, 61.21f);
		return p;
	}
	
	boolean circular;
	
	public FunkyWipeTransition2D(boolean fullCircle) {
		circular = fullCircle;
	}
	

	@Override
	public Transition2DInstruction[] getInstructions(float progress,
			Dimension size) {
		Rectangle2D.Float frameRect = new Rectangle2D.Float(0, 0, size.width, size.height);
		Point2D p = new Point2D.Double();
		MeasuredShape path = circular ? measuredPathCyclic : measuredPathAcross;
		
		path.getPoint(progress*path.getOriginalDistance(), p );
		
		int m = circular ? 1 : 2;
		double angle = Math.PI/2+m*Math.PI*progress;
		
		float k = 10000;
		GeneralPath clip = new GeneralPath();
		clip.moveTo((float)p.getX(), (float)p.getY());
		clip.lineTo((float)(p.getX()+k*FastMath.cos(angle)), (float)(p.getY()+k* FastMath.sin(angle)));
		clip.lineTo((float)(p.getX()+k*FastMath.cos(angle)+k*FastMath.cos(angle-Math.PI/2)),
				(float)(p.getY()+k*FastMath.sin(angle)+k*FastMath.sin(angle-Math.PI/2)) );
		clip.lineTo((float)(p.getX()-100*FastMath.cos(angle)+k*FastMath.cos(angle-Math.PI/2)),
				(float)(p.getY()-k*FastMath.sin(angle)+k*FastMath.sin(angle-Math.PI/2)) );
		clip.lineTo((float)(p.getX()-k*FastMath.cos(angle)),
				(float)(p.getY()-k*FastMath.sin(angle)) );
		clip.closePath();
		
		AffineTransform map = RectangularTransform.create(
				new Rectangle2D.Float(0, 0, 100, 100),
				frameRect
		);
		clip.transform( map );
		
		clip = Clipper.clipToRect(clip, frameRect);
		
		return new Transition2DInstruction[] {
				new ImageInstruction(true, 1, frameRect, size, null),
				new ImageInstruction(false, 1, frameRect, size, clip)
		};
	}
	
	@Override
	public String toString() {
		String s = ( circular ? " Circular" : " Across");
		return "Funky Wipe "+s;
	}
	
}
