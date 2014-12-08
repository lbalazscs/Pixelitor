/*
 * @(#)AbstractPlanarTransition2D.java
 *
 * $Date: 2014-03-14 07:36:22 +0100 (P, 14 márc. 2014) $
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.media.jai.PerspectiveTransform;

import com.bric.geom.RectangularTransform;
import com.bric.geom.TransformUtils;

/** Here 2 frames slide around on an imaginary table top
 * extending into the distance.  This creates a 3D-type
 * effect where frames move in and out of the users field of view.
 * <P>(The best way to understand exactly what this does is to
 * see it in action.)
 * <P>More technically: this creates a PerspectiveTransform
 * and projects two dots into the plane stretching into the distance, and
 * then this positions the two frames based on those dots.
 *
 */
public abstract class AbstractPlanarTransition2D extends Transition2D {
	
	Color background;
	
	public AbstractPlanarTransition2D() {
		this(Color.black);
	}
	
	public AbstractPlanarTransition2D(Color background) {
		this.background = background;
	}

	@Override
	public Transition2DInstruction[] getInstructions(float progress,Dimension size) {

		PerspectiveTransform transform;
		double upperY, lowerY;

		upperY = size.height*7/10;
		lowerY = size.height;
		double x = size.width*5/20;
		transform = PerspectiveTransform.getQuadToQuad(
				0, 0, 
				1, 0, 
				0, 1, 
				1, 1, 
				x, upperY, 
				size.width-x, upperY, 
				0, lowerY, 
				size.width, lowerY);
		
		Point2D p = new Point2D.Double(0,.5);
		transform.transform(p,p);
		
		Point2D pA = getFrameALocation(progress);
		Point2D pB = getFrameBLocation(progress);
		transform.transform(pA, pA);
		transform.transform(pB, pB);
		
		double height, ratio, width;
		Rectangle2D r1 = new Rectangle2D.Double();
		Rectangle2D r2 = new Rectangle2D.Double();
		
		height = lowerY-(lowerY-pA.getY())*2;
		ratio = height/lowerY;
		width = size.getWidth()*ratio;
		r1.setFrame(
				pA.getX()-width/2, pA.getY()-height, width, height );


		height = lowerY-(lowerY-pB.getY())*2;
		ratio = height/lowerY;
		width = size.getWidth()*ratio;
		r2.setFrame(
				pB.getX()-width/2, pB.getY()-height, width, height );
		
		Rectangle big = new Rectangle(0,0,size.width,size.height);
		
		AffineTransform transform1 = RectangularTransform.create(big, r1);
		AffineTransform transform2 = RectangularTransform.create(big, r2);
		float opacity1 = getFrameAOpacity(progress);
		float opacity2 = getFrameBOpacity(progress);
		ImageInstruction i1A = new ImageInstruction(true,transform1,null);
		ShapeInstruction i1B = new ShapeInstruction(r1,getShade(1-opacity1));
		ImageInstruction i2A = new ImageInstruction(false,transform2,null);
		ShapeInstruction i2B = new ShapeInstruction(r2,getShade(1-opacity2));
		
		AffineTransform transform1z = TransformUtils.createAffineTransform(0,0,
				big.getWidth(),0,
				0,big.getHeight(),
				r1.getX(),r1.getY()+r1.getHeight()*2,
				r1.getX()+r1.getWidth(),r1.getY()+r1.getHeight()*2,
				r1.getX(),r1.getY()+r1.getHeight()+1);
		AffineTransform transform2z = TransformUtils.createAffineTransform(0,0,
				big.getWidth(),0,
				0,big.getHeight(),
				r2.getX(),r2.getY()+r2.getHeight()*2,
				r2.getX()+r2.getWidth(),r2.getY()+r2.getHeight()*2,
				r2.getX(),r2.getY()+r2.getHeight()+1);
		Rectangle2D shadow1Rect = new Rectangle2D.Double(
				r1.getX(), r1.getY()+r1.getHeight()+1,
				r1.getWidth(), r1.getHeight()
				);
		Rectangle2D shadow2Rect = new Rectangle2D.Double(
				r2.getX(), r2.getY()+r2.getHeight()+1,
				r2.getWidth(), r2.getHeight()
				);
		ImageInstruction i1ShadowA = new ImageInstruction(true,transform1z,null);
		ShapeInstruction i1ShadowB = new ShapeInstruction(shadow1Rect,getShade(1-opacity1*.3f));
		ImageInstruction i2ShadowA = new ImageInstruction(false,transform2z,null);
		ShapeInstruction i2ShadowB = new ShapeInstruction(shadow2Rect,getShade(1-opacity2*.3f));
		
		ShapeInstruction backgroundRect = new ShapeInstruction(new Rectangle(0,0,size.width,size.height),
				background, null, 0
				);
		
		if(r1.getHeight()>r2.getHeight()) {
			return new Transition2DInstruction[] { 
					backgroundRect,
					i2A, i2B,
					i2ShadowA, i2ShadowB,
					i1A, i1B,
					i1ShadowA, i1ShadowB };
		} else {
			return new Transition2DInstruction[] { 
					backgroundRect,
					i1A, i1B,
					i1ShadowA, i1ShadowB,
					i2A, i2B,
					i2ShadowA, i2ShadowB };
		}	
	}
	
	private Color getShade(float opacity) {
		return new Color(background.getRed(), background.getGreen(), background.getBlue(), (int)(255*opacity));
	}
	
	/** This should be a dot within the rectangle (0,0,1,1).
	 * Imagine the rectangle is a diagram of a stage
	 * (you know, a theatrical stage, facing an audience).
	 * When the point is (.5,1), this frame is exactly
	 * centered in the user's field of view.
	 * At (.5,0) this frame is centered, but small -- as if
	 * in the distance.  So the y-coordinate is used to
	 * represent depth, and the x-coordinate is used to
	 * represent horizontal movement.
	 */
	public abstract Point2D getFrameALocation(float p);

	/** This should be a dot within the rectangle (0,0,1,1).
	 * Imagine the rectangle is a diagram of a stage
	 * (you know, a theatrical stage, facing an audience).
	 * When the point is (.5,1), this frame is exactly
	 * centered in the user's field of view.
	 * At (.5,0) this frame is centered, but small -- as if
	 * in the distance.  So the y-coordinate is used to
	 * represent depth, and the x-coordinate is used to
	 * represent horizontal movement.
	 */
	public abstract Point2D getFrameBLocation(float p);
	
	/** The opacity of the first frame */
	public abstract float getFrameAOpacity(float p);

	/** The opacity of the second frame */
	public abstract float getFrameBOpacity(float p);
}
