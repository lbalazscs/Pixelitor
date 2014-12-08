/*
 * @(#)ShapeInstruction.java
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

/** This instruction is used to render a shape.  The fill color or
 * stroke color can support alpha values.
 * <P>Both the fill color and stroke color are optional.
 * 
 */
public class ShapeInstruction extends Transition2DInstruction {
	public Color fillColor;
	public Color strokeColor;
	public float strokeWidth;
	public Shape shape;
	
	/** Creates a ShapeInstruction.
	 * 
	 * @param shape the shape to render
	 * @param fillColor the fill to use with this shape.
	 * If this is null then no fill is painted.
	 * @param strokeColor the fill to paint the stroke with.
	 * If this is null then no stroke is painted.
	 * @param strokeWidth the width of the stroke.
	 * If this is 0 then no stroke is painted.
	 */
	public ShapeInstruction(Shape shape,Color fillColor,Color strokeColor,float strokeWidth) {
		if(shape==null) throw new NullPointerException("A ShapeInstruction cannot have a null shape.");
		this.fillColor = fillColor;
		this.strokeColor = strokeColor;
		this.strokeWidth = strokeWidth;
		this.shape = shape;
	}
	
	/** Creates a black-ish shape. */
	public ShapeInstruction(Shape shape,float opacity) {
		this(shape, new Color(0,0,0,(int)(255*opacity)));
	}
	
	/** Creates a shape.
	 */
	public ShapeInstruction(Shape shape,Color fillColor) {
		this.shape = new GeneralPath(shape);
		this.fillColor = fillColor;
	}
	
	@Override
	public void paint(Graphics2D g, BufferedImage frameA, BufferedImage frameB) {
		Paint oldPaint = g.getPaint();
		
		if(fillColor!=null) {
			g.setColor(fillColor);
			g.fill(shape);
		}
		if(strokeColor!=null && strokeWidth>0) {
			Stroke oldStroke = g.getStroke();
			
			g.setStroke(new BasicStroke(strokeWidth));
			g.setColor(strokeColor);
			g.draw(shape);
		
			g.setStroke(oldStroke);
		}
		
		g.setPaint(oldPaint);
	}
}
