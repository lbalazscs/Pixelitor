/*
 * @(#)SimplifiedPathIterator.java
 *
 * $Date: 2009-02-22 14:56:51 -0600 (Sun, 22 Feb 2009) $
 *
 * Copyright (c) 2009 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.blogspot.com/
 * 
 * And the latest version should be available here:
 * https://javagraphics.dev.java.net/
 */
package com.bric.geom;

import java.awt.geom.PathIterator;

/** This filters a <code>PathIterator</code> and makes sure
 * that each curve is of the smallest possible degree.
 * <P>In addition to being more efficient, this can avoid
 * divide-by-zero errors for some operations.
 * 
 */
public class SimplifiedPathIterator implements PathIterator {
    /** This is the tolerance a term can be to be considered "zero".
     * This value is .0001.
     */
	final static private double TOL = .0001;
	PathIterator i;
	double lastX, lastY;
	
	/** Creates a new SimplifiedPathIterator that filters
	 * the argument.
	 * 
	 * @param i
	 */
	public SimplifiedPathIterator(PathIterator i) {
		this.i = i;
	}
	
	/** Returns true if all 3 points are on the same line.
	 * This checks to see if:
	 * <BR><code>x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2)<TOL*TOL</code>
	 * 
	 * @param x1 the x-coordinate of point #1
	 * @param y1 the y-coordinate of point #1
	 * @param x2 the x-coordinate of point #2
	 * @param y2 the y-coordinate of point #2
	 * @param x3 the x-coordinate of point #3
	 * @param y3 the y-coordinate of point #3
	 * @return true if all 3 points are on the same line.
	 */
	public static boolean collinear(double x1,double y1,double x2,double y2,double x3,double y3) {
		double determinant = 
			x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
		return Math.abs(determinant)<TOL*TOL;
	}
	
	/** This possibly reduces the degree of a segment, if possible.
	 * 
	 * @param type the current expected type of the segment data
	 * @param lastX the previous X value from which this segment begins
	 * @param lastY the previous Y value from which this segment begins
	 * @param data the data of the current segment.
	 * @return the new segment type, or the original <code>type</code>
	 * argument if nothing was modified.
	 */
	public static int simplify(int type,double lastX,double lastY,double[] data) {
		if(type==SEG_CUBICTO) {
			if(collinear(lastX,lastY,data[4],data[5],data[0],data[1])
					&& collinear(lastX,lastY,data[4],data[5],data[2],data[3])) {
				data[0] = data[4];
				data[1] = data[5];
				return SEG_LINETO;
			}
		} else if(type==SEG_QUADTO) {
			if(collinear(lastX,lastY,data[2],data[3],data[0],data[1])) {
				data[0] = data[2];
				data[1] = data[3];
				return SEG_LINETO;
			}
		}

		//Are there other checks we can be doing?
		//for example: can we end up with a cubic
		//curve where ax and ay equal zero?
		
		return type;
	}

	public int currentSegment(double[] f) {
		int type = i.currentSegment(f);
		type = simplify(type,lastX,lastY,f);
		if(type==PathIterator.SEG_LINETO || type==PathIterator.SEG_MOVETO) {
			lastX = f[0];
			lastY = f[1];
		} else if(type==PathIterator.SEG_QUADTO) {
			lastX = f[2];
			lastY = f[3];
		} else if(type==PathIterator.SEG_CUBICTO) {
			lastX = f[4];
			lastY = f[5];
		}
		return type;
	}

	private double[] d;
	public int currentSegment(float[] f) {
		if(d==null) {
			d = new double[6];
		}
		int k = currentSegment(d);
		f[0] = (float)d[0];
		f[1] = (float)d[1];
		f[2] = (float)d[2];
		f[3] = (float)d[3];
		f[4] = (float)d[4];
		f[5] = (float)d[5];
		return k;
	}

	public int getWindingRule() {
		return i.getWindingRule();
	}

	public boolean isDone() {
		return i.isDone();
	}

	public void next() {
		i.next();
	}
}
