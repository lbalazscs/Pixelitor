/*
 * @(#)PathWriter.java
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
package com.bric.geom;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

/** This object writes path data.
 * <P>With this abstract class you could filter shape data as it is
 * being written.  The simplest extension of this is the
 * {@link GeneralPathWriter}.
 * 
 * <P>This is designed to match the <code>GeneralPath</code> method
 * signatures.
 * 
 */
public abstract class PathWriter {
	
	/** Adds a point to the path by moving to the specified coordinates. 
	 * 
	 * @param x the x-coordinate to move to
	 * @param y the y-coordinate to move to
	 */
	public abstract void moveTo(float x,float y);
	
	/** Adds a point to the path by drawing a straight line from the current coordinates to the new specified coordinates.
	 *
	 *  @param x the x-coordinate of the end point.
	 *  @param y the y-coordinate of the end point. 
	 */
	public abstract void lineTo(float x,float y);
	
	/** Adds a curved segment, defined by two new points, to the path by drawing a Quadratic curve that intersects both the current coordinates and the coordinates (x2, y2), using the specified point (x1, y1) as a quadratic parametric control point.
	 *
	 *  @param cx the x-coordinate of the control point.
	 *  @param cy the y-coordinate of the control point.
	 *  @param x the x-coordinate of the end point.
	 *  @param y the y-coordinate of the end point.
	 */
	public abstract void quadTo(float cx,float cy,float x,float y);
	
	/** Adds a curved segment, defined by three new points, to the path by drawing a Bezier curve that intersects both the current coordinates and the coordinates (x3, y3), using the specified points (x1, y1) and (x2, y2) as Bezier control points.
	 * 
	 *  @param cx1 the x-coordinate of the first control point.
	 *  @param cy1 the y-coordinate of the first control point.
	 *  @param cx2 the x-coordinate of the second control point.
	 *  @param cy2 the y-coordinate of the second control point.
	 *  @param x the x-coordinate of the end point.
	 *  @param y the y-coordinate of the end point.
	 *  */
	public abstract void curveTo(float cx1,float cy1,float cx2,float cy2,float x,float y);
	
	/** Closes the current subpath by drawing a straight line back to the coordinates of the last moveTo. */
	public abstract void closePath();
	
	/** This guarantees that this writer has flushed all currently written
	 * information.
	 * <P>For example, some writers may need to perform complex calculations
	 * and re-organize shapes.  Or some writers may simply be buffering
	 * path instructions to better manage memory.  Calling this method
	 * guarantees that all path instructions be immediately processed.
	 * <P>Because some writers may be manipulating path data you should
	 * <i>not call this method</i> until all shape data has been written.
	 * 
	 */
	public abstract void flush();
	
	/** Writes a shape.
	 * <P>This iterates through the shape and makes the appropriate
	 * calls to <code>moveTo()</code>, <code>curveTo()</code>, etc.
	 * 
	 * @param s the shape to write.
	 */
	public void write(Shape s) {
		write(s.getPathIterator(null));
	}
	
	/** Writes a path.
	 * <P>This iterates through the path and makes the appropriate
	 * calls to <code>moveTo()</code>, <code>curveTo()</code>, etc.
	 * 
	 * @param i the path to write.
	 */
	public void write(PathIterator i) {
		float[] coords = new float[6];
		int k;
		while(i.isDone()==false) {
			k = i.currentSegment(coords);
			if(k==PathIterator.SEG_MOVETO) {
				moveTo(coords[0],coords[1]);
			} else if(k==PathIterator.SEG_LINETO) {
				lineTo(coords[0],coords[1]);
			} else if(k==PathIterator.SEG_QUADTO) {
				quadTo(coords[0],coords[1],coords[2],coords[3]);
			} else if(k==PathIterator.SEG_CUBICTO) {
				curveTo(coords[0],coords[1],coords[2],coords[3],coords[4],coords[5]);
			} else if(k==PathIterator.SEG_CLOSE) {
				closePath();
			} else {
				throw new RuntimeException("Unexpected segment: "+k);
			}
			i.next();
		}
	}

	/** This appends a cubic curve to the GeneralPath provided that ranges from t = t0 to t = t1.
	 * This assumes that the path already ends at the point (ax*t0*t0*t0+bx*t0*t0+cx*t0+dx, ay*t0*t0*t0+by*t0*t0+cy*t0+dx).
	 * @param path the path to append data to.
	 * @param t0 the t-value for the beginning of this curve
	 * @param t1 the t-value for the end of this curve
	 * @param ax the coefficient for the (t^3) term of the x parametric equation
	 * @param bx the coefficient for the (t^2) term of the x parametric equation
	 * @param cx the coefficient for the t term of the x parametric equation
	 * @param dx the constant for the x parametric equation.
	 * @param ay the coefficient for the (t^3) term of the y parametric equation
	 * @param by the coefficient for the (t^2) term of the y parametric equation
	 * @param cy the coefficient for the t term of the y parametric equation
	 * @param dy the constant for the y parametric equation.
	 */
	public static void cubicTo(GeneralPath path,double t0,double t1,double ax,double bx,double cx,double dx,double ay,double by,double cy,double dy) {
		cubicTo2(path,t0,t1,ax,bx,cx,dx,ay,by,cy,dy);
	}
	/** This appends a cubic curve to the PathWriter provided that ranges from t = t0 to t = t1.
	 * This assumes that the path already ends at the point (ax*t0*t0*t0+bx*t0*t0+cx*t0+dx, ay*t0*t0*t0+by*t0*t0+cy*t0+dx).
	 * @param path the path to append data to.
	 * @param t0 the t-value for the beginning of this curve
	 * @param t1 the t-value for the end of this curve
	 * @param ax the coefficient for the (t^3) term of the x parametric equation
	 * @param bx the coefficient for the (t^2) term of the x parametric equation
	 * @param cx the coefficient for the t term of the x parametric equation
	 * @param dx the constant for the x parametric equation.
	 * @param ay the coefficient for the (t^3) term of the y parametric equation
	 * @param by the coefficient for the (t^2) term of the y parametric equation
	 * @param cy the coefficient for the t term of the y parametric equation
	 * @param dy the constant for the y parametric equation.
	 */
	public static void cubicTo(PathWriter path,double t0,double t1,double ax,double bx,double cx,double dx,double ay,double by,double cy,double dy) {
		cubicTo2(path,t0,t1,ax,bx,cx,dx,ay,by,cy,dy);
	}
	private static void cubicTo2(Object obj,double t0,double t1,double ax,double bx,double cx,double dx,double ay,double by,double cy,double dy) {
	    double tW = 2.0*t0/3.0+t1/3.0;
        double tZ = t0/3.0+2.0*t1/3.0;
        
        double f0 = ay*t0*t0*t0+by*t0*t0+cy*t0+dy;
        double f1 = ay*tW*tW*tW+by*tW*tW+cy*tW+dy;
        double f2 = ay*tZ*tZ*tZ+by*tZ*tZ+cy*tZ+dy;
        double f3 = ay*t1*t1*t1+by*t1*t1+cy*t1+dy;
        
        double dy2 = f0;
        double cy2 = (-11*f0+18*f1-9*f2+2*f3)/2.0;
        double by2 = (-19*f0+27*f2-8*f3-10*cy2)/4;
        double ay2 = f3-by2-cy2-f0;
        
        f0 = ax*t0*t0*t0+bx*t0*t0+cx*t0+dx;
        f1 = ax*tW*tW*tW+bx*tW*tW+cx*tW+dx;
        f2 = ax*tZ*tZ*tZ+bx*tZ*tZ+cx*tZ+dx;
        f3 = ax*t1*t1*t1+bx*t1*t1+cx*t1+dx;
        
        double dx2 = f0;
        double cx2 = (-11*f0+18*f1-9*f2+2*f3)/2.0;
        double bx2 = (-19*f0+27*f2-8*f3-10*cx2)/4;
        double ax2 = f3-bx2-cx2-f0;
        
        double cy0 = (3*dy2+cy2)/3;
        double cy1 = (by2-3*dy2+6*cy0)/3;
        double y1 = ay2+dy2-3*cy0+3*cy1;
        
        double cx0 = (3*dx2+cx2)/3;
        double cx1 = (bx2-3*dx2+6*cx0)/3;
        double x1 = ax2+dx2-3*cx0+3*cx1;
        
        if(obj instanceof GeneralPath) {
	        ((GeneralPath)obj).curveTo((float)cx0, (float)cy0, 
	        		(float)cx1, (float)cy1, 
	        		(float)x1, (float)y1);
        } else if(obj instanceof PathWriter) {
        	((PathWriter)obj).curveTo((float)cx0, (float)cy0, 
	        		(float)cx1, (float)cy1, 
	        		(float)x1, (float)y1);
        }
	}
	
	/** This appends a quadratic curve to the GeneralPath provided that ranges from t = t0 to t = t1.
	 * This assumes that the path already ends at the point (ax*t0*t0+bx*t0+cx, ay*t0*t0+by*t0+cy).
	 * @param path the path to append data to.
	 * @param t0 the t-value for the beginning of this curve
	 * @param t1 the t-value for the end of this curve
	 * @param ax the coefficient for the (t^2) term of the x parametric equation
	 * @param bx the coefficient for the t term of the x parametric equation
	 * @param cx the constant for the x parametric equation.
	 * @param ay the coefficient for the (t^2) term of the y parametric equation
	 * @param by the coefficient for the t term of the y parametric equation
	 * @param cy the constant for the y parametric equation.
	 */
	public static void quadTo(GeneralPath path,double t0,double t1,double ax,double bx,double cx,double ay,double by,double cy) {
        quadTo2(path,t0,t1,ax,bx,cx,ay,by,cy);
	}

	/** This appends a quadratic curve to the PathWriter provided that ranges from t = t0 to t = t1.
	 * This assumes that the path already ends at the point (ax*t0*t0+bx*t0+cx, ay*t0*t0+by*t0+cy).
	 * @param path the path to append data to.
	 * @param t0 the t-value for the beginning of this curve
	 * @param t1 the t-value for the end of this curve
	 * @param ax the coefficient for the (t^2) term of the x parametric equation
	 * @param bx the coefficient for the t term of the x parametric equation
	 * @param cx the constant for the x parametric equation.
	 * @param ay the coefficient for the (t^2) term of the y parametric equation
	 * @param by the coefficient for the t term of the y parametric equation
	 * @param cy the constant for the y parametric equation.
	 */
	public static void quadTo(PathWriter path,double t0,double t1,double ax,double bx,double cx,double ay,double by,double cy) {
        quadTo2(path,t0,t1,ax,bx,cx,ay,by,cy);
	}
	
	private static void quadTo2(Object obj,double t0,double t1,double ax,double bx,double cx,double ay,double by,double cy) {
		double tZ = (t0+t1)/2.0;
        
        double f0 = ay*t0*t0+by*t0+cy;
        double f1 = ay*tZ*tZ+by*tZ+cy;
        double f2 = ay*t1*t1+by*t1+cy;
        
        double ay2 = 2*f2-4*f1+2*f0;
        double cy2 = f0;
        double by2 = f2-cy2-ay2;
        
        f0 = ax*t0*t0+bx*t0+cx;
        f1 = ax*tZ*tZ+bx*tZ+cx;
        f2 = ax*t1*t1+bx*t1+cx;
        
        double ax2 = 2*f2-4*f1+2*f0;
        double cx2 = f0;
        double bx2 = f2-cx2-ax2;
        
        double ctrlY = (2*cy2+by2)/2;
        double y1 = ay2-cy2+2*ctrlY;
        
        double ctrlX = (2*cx2+bx2)/2;
        double x1 = ax2-cx2+2*ctrlX;
        
        if(obj instanceof GeneralPath) {
	        ((GeneralPath)obj).quadTo((float)ctrlX, (float)ctrlY,
	        		(float)x1, (float)y1);
        } else if(obj instanceof PathWriter) {
	        ((PathWriter)obj).quadTo((float)ctrlX, (float)ctrlY,
	        		(float)x1, (float)y1);
        }
	}
}
