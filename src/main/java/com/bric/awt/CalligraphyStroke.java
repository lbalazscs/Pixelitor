/*
 * @(#)CalligraphyStroke.java
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
package com.bric.awt;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

import com.bric.geom.GeneralPathWriter;

/**
 * This <code>Stroke</code> resembles calligraphy.
 * <P>
 * The angle of the pen (or nib) is fixed.
 * 
 */
public class CalligraphyStroke implements Stroke {

	/** The width of this stroke in pixels. */
	public final float width;

	/** The angle of the pen in radians. */
	public final float theta;
	

	/**
	 * Create a simple CalligraphyStroke with an angle of 3*pi/4.
	 * 
	 * @param width
	 *            the width of the stroke (in pixels).
	 */
	public CalligraphyStroke(float width) {
		this(width, (float) (Math.PI / 4.0 * 3.0));
	}

	/**
	 * Creates a new CalligraphyStroke
	 * 
	 * @param width
	 *            the width of the pen (in pixels)
	 * @param angle
	 *            the angle of the pen (in radians)
	 */
	public CalligraphyStroke(float width, float angle) {
		this.width = width;
		this.theta = angle;
	}

	/**
	 * Returns the width of this stroke.
	 * 
	 * @return the width of this stroke.
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Returns the angle of the pen (in radians).
	 * 
	 * @return the angle of the pen (in radians).
	 */
	public float getTheta() {
		return theta;
	}

	/**
	 * Creates the calligraphic outline of the argument shape.
	 * 
	 */
	public Shape createStrokedShape(Shape p) {
		GeneralPath dest = new GeneralPath();
		GeneralPathWriter writer = new GeneralPathWriter(dest);
		CalligraphyPathWriter cpw = new CalligraphyPathWriter(theta, width/2, -width/2, writer, writer);
		cpw.write(p);
		cpw.flush();
		return dest;
	}
}
