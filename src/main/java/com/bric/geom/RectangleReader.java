/*
 * @(#)RectangleReader.java
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
package com.bric.geom;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import com.bric.math.MathG;

/** This can identify if a shape is a Rectangle, Rectangle2D or other.
 * <P>If a shape is a rectangle, then certain operations can be
 * optimized.
 * <P>Also there is a bug when clipping shapes using Quartz on
 * Mac: a GeneralPath encapsulating exactly the same area
 * as a Rectangle2D may not clip correctly.  If this abstract
 * shape is instead converted to a Rectangle2D: the bug
 * goes away!
 */
public class RectangleReader {
	/** Returns true if a shape is a rectangle. */
	public static boolean isRectangle(Shape s) {
		return convert(s)!=null;
	}
	
	/** Returns true if a shape is a rectangle when the transform is applied. */
	public static boolean isRectangle(Shape s,AffineTransform tx) {
		return convert(s, tx)!=null;
	}
	
	/** This studies a shape and determines if it is
	 * a <code>Rectangle</code>, a <code>Rectangle2D</code>,
	 * or neither.
	 * 
	 * @param shape the shape to study
	 * @return a <code>Rectangle</code>, <code>Rectangle2D</code>,
	 * or <code>null</code>.
	 */
	public static final Rectangle2D convert(Shape shape) {
		return convert(shape, null);
	}
	
	/** This studies a shape and determines if it is
	 * a <code>Rectangle</code>, a <code>Rectangle2D</code>,
	 * or neither.
	 * 
	 * @param shape the shape to study
	 * @param transform the optional transform to apply to the shape.
	 * @return a <code>Rectangle</code>, <code>Rectangle2D</code>,
	 * or <code>null</code>.
	 */	
	public static final Rectangle2D convert(Shape shape,AffineTransform transform) {
		if(shape==null)
			return null;
		
		if(transform!=null && transform.isIdentity())
			transform = null;
		
		if(shape instanceof Rectangle && transform==null)
			return (Rectangle)shape;
		
		if(shape instanceof Rectangle2D && transform==null) {
			Rectangle2D rect = (Rectangle2D)shape;
			return getRectangle( rect );
		}
				
		/* Lots of ways we could approach this...
		 * This is a straight-forward logical approach that
		 * could probably stand to be improved performance-
		 * wise:
		 * 1.  Get the bounds of the shape.
		 * 2.  Iterate over the shape a second time, and see if
		 * all points are collinear with the bounds.
		 * 
		 */
		
		double[] data = new double[6];
		
		int k;

		double lastX = 0;
		double lastY = 0;
		
		PathIterator i = shape.getPathIterator(transform);
		
		double left = 0;
		double right = 0;
		double top = 0;
		double bottom = 0;
		boolean defined = false;
		double moveX = 0;
		double moveY = 0;

		while(i.isDone()==false) {
			k = i.currentSegment(data);
			k = SimplifiedPathIterator.simplify(k, lastX, lastY, data);
			if(k==PathIterator.SEG_CLOSE) {
				k = PathIterator.SEG_LINETO;
				data[0] = moveX;
				data[1] = moveY;
			}
			
			if(k==PathIterator.SEG_MOVETO) {
				moveX = data[0];
				moveY = data[1];
				lastX = data[0];
				lastY = data[1];
				//multiple paths are a deal-breaker
				if(defined)
					return null;
			} else if(k==PathIterator.SEG_CLOSE) {
				//do nothing
			} else if(k==PathIterator.SEG_LINETO) {
				if(defined==false) {
					left = right = lastX;
					top = bottom = lastY;
					defined = true;
				} else {
					if(lastX<left) left = lastX;
					if(lastY<top) top = lastY;
					if(lastX>right) right = lastX;
					if(lastY>bottom) bottom = lastY;
					
					//either X or Y needs to be the same
					//in a rectangle:
					if(lastX!=data[0] && lastY!=data[1])
						return null;
				}

				if(data[0]<left) left = data[0];
				if(data[1]<top) top = data[1];
				if(data[0]>right) right = data[0];
				if(data[1]>bottom) bottom = data[1];
				lastX = data[0];
				lastY = data[1];
			} else {
				return null;
			}
			i.next();
		}

		if(defined==false)
			return null;
		
		if(lastX!=moveX && lastY!=moveY)
			return null;
		
		i = shape.getPathIterator(transform);
		
		while(i.isDone()==false) {
			k = i.currentSegment(data);
			k = SimplifiedPathIterator.simplify(k, lastX, lastY, data);
			if(k==PathIterator.SEG_MOVETO) {
				lastX = data[0];
				lastY = data[1];
			} else if(k==PathIterator.SEG_LINETO) {
				double midX = (data[0]+lastX)/2;
				double midY = (data[1]+lastY)/2;
				if(data[1]==top) {
					if(SimplifiedPathIterator.collinear(left, top, right, top, data[0], data[1])==false) {
						return null;
					}
				} else if(data[1]==bottom) {
					if(SimplifiedPathIterator.collinear(left, bottom, right, bottom, data[0], data[1])==false) {
						return null;
					}
				} else if(data[0]==left) {
					if(SimplifiedPathIterator.collinear(left, top, left, bottom, data[0], data[1])==false) {
						return null;
					}
				} else if(data[0]==right) {
					if(SimplifiedPathIterator.collinear(right, top, right, bottom, data[0], data[1])==false) {
						return null;
					}
				} else {
					return null;
				}

				if(midY==top) {
					if(SimplifiedPathIterator.collinear(left, top, right, top, midX, midY)==false) {
						return null;
					}
				} else if(midY==bottom) {
					if(SimplifiedPathIterator.collinear(left, bottom, right, bottom, midX, midY)==false) {
						return null;
					}
				} else if(midX==left) {
					if(SimplifiedPathIterator.collinear(left, top, left, bottom, midX, midY)==false) {
						return null;
					}
				} else if(midX==right) {
					if(SimplifiedPathIterator.collinear(right, top, right, bottom, midX, midY)==false) {
						return null;
					}
				} else {
					return null;
				}
				lastX = data[0];
				lastY = data[1];
			}
			
			i.next();
		}
		
		Rectangle intRect = getRectangle(left,top,right-left,bottom-top);
		if(intRect!=null) return intRect;
		
		return new Rectangle2D.Double(left, top, right-left, bottom-top);
	}
	
	private static final double TOL = .000000000001;
	
	/** This checks to see if a Rectangle2D can be expressed as
	 * a int-based Rectangle.
	 * @param r
	 * @return a new <code>Rectangle</code> if possible, or 
	 * the original argument if not.
	 */
	private static final Rectangle2D getRectangle(Rectangle2D r) {
		double x = r.getX();
		double y = r.getY();
		double w = r.getWidth();
		double h = r.getHeight();
		Rectangle newRect = getRectangle(x,y,w,h);
		if(newRect!=null)
			return newRect;
		return r;
	}
	
	/** This checks to see if a Rectangle2D can be expressed as
	 * a int-based Rectangle.
	 * @param r
	 * @return a new <code>Rectangle</code> if possible, or 
	 * the null if not.
	 */
	private static final Rectangle getRectangle(double x,double y,double w,double h) {
		if(w<0) {
			x = x+w;
			w = -w;
		}
		if(h<0) {
			y = y+w;
			h = -h;
		}
		
		int iw = MathG.roundInt(w);
		int ih = MathG.roundInt(h);
		if(Math.abs(iw-w)>TOL)
			return null;
		if(Math.abs(ih-h)>TOL)
			return null;
		int ix = MathG.roundInt(x);
		int iy = MathG.roundInt(y);
		if(Math.abs(ix-x)>TOL)
			return null;
		if(Math.abs(iy-y)>TOL)
			return null;
		
		return new Rectangle(ix,iy,iw,ih);
	}
}
