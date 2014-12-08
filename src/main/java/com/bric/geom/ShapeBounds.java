/*
 * @(#)ShapeBounds.java
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
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

/** This class features an efficient and accurate
 * <code>getBounds()</code> method.  The <code>java.awt.Shape</code>
 * API clearly states that the <code>Shape.getBounds2D()</code> method
 * may return a rectangle larger than the bounds of the actual shape,
 * so here I present a method to get the bounds without resorting to
 * the very-accurate-but-very-slow <code>java.awt.geom.Area</code> class.
 */
public class ShapeBounds {
	/** This calculates the precise bounds of a shape.
	 * @param shape the shape you want the bounds of.
	 * This method throws a NullPointerException if this is null.
	 * @return the bounds of <code>shape</code>.
	 * 
	 * @throws EmptyPathException if the shape argument is empty.
	 */
	public static Rectangle2D getBounds(Shape shape) throws EmptyPathException{
		return getBounds(shape,null,null);
	}
	
	public static Rectangle2D getBounds(Shape[] shapes) {
		Rectangle2D r = null;
		for(int a = 0; a<shapes.length; a++) {
			try {
				Rectangle2D t = getBounds(shapes[a]);
				if(r==null) {
					r = t;
				} else {
					r.add(t);
				}
			} catch(EmptyPathException e) {}
		}
		return r;
	}

	/** This calculates the precise bounds of a shape.
	 * @param shape the shape you want the bounds of.
	 * This method throws a NullPointerException if this is null.
	 * @param transform if this is non-null, then this method returns the bounds of
	 * <code>shape</code> as seen through <code>t</code>.
	 * @return the bounds of <code>shape</code>, as seen through <code>transform</code>.
	 * 
	 * @throws EmptyPathException if the shape argument is empty.
	 */
	public static Rectangle2D getBounds(Shape shape,AffineTransform transform) throws EmptyPathException {
		return getBounds(shape,transform,null);
	}

	/** This calculates the precise bounds of a shape.
	 * @param shape the shape you want the bounds of.
	 * This method throws a NullPointerException if this is null.
	 * @param transform if this is non-null, then this method returns the bounds of
	 * <code>shape</code> as seen through <code>t</code>.
	 * @param r if this is non-null, then the result is stored in
	 * this rectangle.  This is useful when you need to call this method
	 * repeatedly without allocating a lot of memory.
	 * @return the bounds of <code>shape</code>, as seen through <code>transform</code>.
	 * 
	 * @throws EmptyPathException if the shape argument is empty.
	 */
	public static Rectangle2D getBounds(Shape shape,AffineTransform transform,Rectangle2D r) throws EmptyPathException {
		PathIterator i = shape.getPathIterator(transform);
		return getBounds(i,r);
	}


	/** This calculates the precise bounds of a shape.
	 * @param shape the shape you want the bounds of.
	 * This method throws a NullPointerException if this is null.
	 * @param r if this is non-null, then the result is stored in
	 * this rectangle.  This is useful when you need to call this method
	 * repeatedly without allocating a lot of memory.
	 * @return the bounds of <code>shape</code>.
	 * 
	 * @throws EmptyPathException if the shape argument is empty.
	 */
	public static Rectangle2D getBounds(Shape shape,Rectangle2D r) throws EmptyPathException {
		return getBounds(shape,null,r);
	}

	/** This calculates the precise bounds of a shape.
	 * @param i the shape you want the bounds of.
	 * This method throws a NullPointerException if this is null.
	 * @return the bounds of <code>i</code>.
	 */
	public static Rectangle2D getBounds(PathIterator i) {
		return getBounds(i,null);
	}

	/** This calculates the precise bounds of a shape.
	 * @param i the shape you want the bounds of.
	 * This method throws a NullPointerException if this is null.
	 * @param r if this is non-null, then the result is stored in
	 * this rectangle.  This is useful when you need to call this method
	 * repeatedly without allocating a lot of memory.
	 * @return the bounds of <code>i</code>.
	 */
	public static Rectangle2D getBounds(PathIterator i,Rectangle2D r) {
		float[] f = new float[6];
		
		int k;

		/** left, top, right, and bottom bounds */
		float[] bounds = null;

		float lastX = 0;
		float lastY = 0;

		//A, B, C, and D in the equation x = a*t^3+b*t^2+c*t+d
		//or A, B, and C in the equation x = a*t^2+b*t+c
		float[] x_coeff = new float[4];
		float[] y_coeff = new float[4];

		float t, x, y, det;
		while(i.isDone()==false) {
			k = i.currentSegment(f);
			if(k==PathIterator.SEG_MOVETO) {
				lastX = f[0];
				lastY = f[1];
			} else if(k==PathIterator.SEG_CLOSE) {
				//do nothing
				//note if we had a simple MOVETO and SEG_CLOSE then
				//we haven't changed "bounds".  This is intentional,
				//so if the shape is badly defined the bounds
				//should still make sense.
			} else {
				if(bounds==null) {
					bounds = new float[] {lastX,lastY,lastX,lastY};
				} else {
					if(lastX<bounds[0]) bounds[0] = lastX;
					if(lastY<bounds[1]) bounds[1] = lastY;
					if(lastX>bounds[2]) bounds[2] = lastX;
					if(lastY>bounds[3]) bounds[3] = lastY;
				}

				if(k==PathIterator.SEG_LINETO) {
					if(f[0]<bounds[0]) bounds[0] = f[0];
					if(f[1]<bounds[1]) bounds[1] = f[1];
					if(f[0]>bounds[2]) bounds[2] = f[0];
					if(f[1]>bounds[3]) bounds[3] = f[1];
					lastX = f[0];
					lastY = f[1];
				} else if(k==PathIterator.SEG_QUADTO) {
					//check the end point
					if(f[2]<bounds[0]) bounds[0] = f[2];
					if(f[3]<bounds[1]) bounds[1] = f[3];
					if(f[2]>bounds[2]) bounds[2] = f[2];
					if(f[3]>bounds[3]) bounds[3] = f[3];

					//find the extrema
					x_coeff[0] = lastX-2*f[0]+f[2];
					x_coeff[1] = -2*lastX+2*f[0];
					x_coeff[2] = lastX;
					y_coeff[0] = lastY-2*f[1]+f[3];
					y_coeff[1] = -2*lastY+2*f[1];
					y_coeff[2] = lastY;

					//x = a*t^2+b*t+c
					//dx/dt = 0 = 2*a*t+b
					//t = -b/(2a)
					t = -x_coeff[1]/(2*x_coeff[0]);
					if(t>0 && t<1) {
						x = x_coeff[0]*t*t+x_coeff[1]*t+x_coeff[2];
						if(x<bounds[0]) bounds[0] = x;
						if(x>bounds[2]) bounds[2] = x;
					}
					t = -y_coeff[1]/(2*y_coeff[0]);
					if(t>0 && t<1) {
						y = y_coeff[0]*t*t+y_coeff[1]*t+y_coeff[2];
						if(y<bounds[1]) bounds[1] = y;
						if(y>bounds[3]) bounds[3] = y;
					}
					lastX = f[2];
					lastY = f[3];
				} else if(k==PathIterator.SEG_CUBICTO) {
					if(f[4]<bounds[0]) bounds[0] = f[4];
					if(f[5]<bounds[1]) bounds[1] = f[5];
					if(f[4]>bounds[2]) bounds[2] = f[4];
					if(f[5]>bounds[3]) bounds[3] = f[5];

					x_coeff[0] = -lastX+3*f[0]-3*f[2]+f[4];
					x_coeff[1] = 3*lastX-6*f[0]+3*f[2];
					x_coeff[2] = -3*lastX+3*f[0];
					x_coeff[3] = lastX;

					y_coeff[0] = -lastY+3*f[1]-3*f[3]+f[5];
					y_coeff[1] = 3*lastY-6*f[1]+3*f[3];
					y_coeff[2] = -3*lastY+3*f[1];
					y_coeff[3] = lastY;

					//x = a*t*t*t+b*t*t+c*t+d
					//dx/dt = 3*a*t*t+2*b*t+c
					//t = [-B+-sqrt(B^2-4*A*C)]/(2A)
					//A = 3*a
					//B = 2*b
					//C = c
					//t = (-2*b+-sqrt(4*b*b-12*a*c)]/(6*a)
					det = (4*x_coeff[1]*x_coeff[1]-12*x_coeff[0]*x_coeff[2]);
					if(det<0) {
						//there are no solutions!  nothing to do here
					} else if(det==0) {
						//there is 1 solution
						t = -2*x_coeff[1]/(6*x_coeff[0]);
						if(t>0 && t<1) {
							x = x_coeff[0]*t*t*t+x_coeff[1]*t*t+x_coeff[2]*t+x_coeff[3];
							if(x<bounds[0]) bounds[0] = x;
							if(x>bounds[2]) bounds[2] = x;
						}
					} else {
						//there are 2 solutions:
						det = (float)Math.sqrt(det);
						t = (-2*x_coeff[1]+det)/(6*x_coeff[0]);
						if(t>0 && t<1) {
							x = x_coeff[0]*t*t*t+x_coeff[1]*t*t+x_coeff[2]*t+x_coeff[3];
							if(x<bounds[0]) bounds[0] = x;
							if(x>bounds[2]) bounds[2] = x;
						}

						t = (-2*x_coeff[1]-det)/(6*x_coeff[0]);
						if(t>0 && t<1) {
							x = x_coeff[0]*t*t*t+x_coeff[1]*t*t+x_coeff[2]*t+x_coeff[3];
							if(x<bounds[0]) bounds[0] = x;
							if(x>bounds[2]) bounds[2] = x;
						}
					}

					det = (4*y_coeff[1]*y_coeff[1]-12*y_coeff[0]*y_coeff[2]);
					if(det<0) {
						//there are no solutions!  nothing to do here
					} else if(det==0) {
						//there is 1 solution
						t = -2*y_coeff[1]/(6*y_coeff[0]);
						if(t>0 && t<1) {
							y = y_coeff[0]*t*t*t+y_coeff[1]*t*t+y_coeff[2]*t+y_coeff[3];
							if(y<bounds[1]) bounds[1] = y;
							if(y>bounds[3]) bounds[3] = y;
						}
					} else {
						//there are 2 solutions:
						det = (float)Math.sqrt(det);
						t = (-2*y_coeff[1]+det)/(6*y_coeff[0]);
						if(t>0 && t<1) {
							y = y_coeff[0]*t*t*t+y_coeff[1]*t*t+y_coeff[2]*t+y_coeff[3];
							if(y<bounds[1]) bounds[1] = y;
							if(y>bounds[3]) bounds[3] = y;
						}

						t = (-2*y_coeff[1]-det)/(6*y_coeff[0]);
						if(t>0 && t<1) {
							y = y_coeff[0]*t*t*t+y_coeff[1]*t*t+y_coeff[2]*t+y_coeff[3];
							if(y<bounds[1]) bounds[1] = y;
							if(y>bounds[3]) bounds[3] = y;
						}
					}

					lastX = f[4];
					lastY = f[5];
				}
			}
			i.next();
		}

		if(bounds==null) {
			throw new EmptyPathException();
		}
		if(r!=null) {
			r.setFrame(bounds[0],bounds[1],bounds[2]-bounds[0],bounds[3]-bounds[1]);
			return r;
		}
		return new Rectangle2D.Float(bounds[0],bounds[1],bounds[2]-bounds[0],bounds[3]-bounds[1]);
	}
}
