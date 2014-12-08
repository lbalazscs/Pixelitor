/*
 * @(#)Clipper.java
 *
 * $Date: 2014-04-14 08:05:51 +0200 (H, 14 ápr. 2014) $
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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Stack;

/** This class lets you clip/intersect an arbitrary shape to a Rectangle2D.
 *
 */
public abstract class Clipper {
	
	/** This is the tolerance with which 2 numbers must
	 * be similar to be considered "equal".
	 * <P>This is necessary because as we much around with numbers
	 * and equations, machine rounding will inevitably cause .5's
	 * to become .49999's and other harmless changes.
	 */
	private static final float TOLERANCE = .0001f;
	
	/** This does 2 things:
	 * 1.  It collapses redundant line segments that fall on the same horizontal
	 * or vertical line.  This is very important, given how clipToRect() works.
	 * And not only does it vastly simplify
	 * your shape (lots of redundant lineTo's will be called), but if a shape
	 * is properly collapsed it has a much better chance of return a truly
	 * accurate result when you call getBounds() on it.
	 * <P>Note that there are still some far fetched examples (involving discontinous
	 * shapes) where getBounds() may be inaccurate, though.
	 * 2.  This can take a Function (either quadratic or cubic) and split it over
	 * a smaller interval from an arbitrary [t0,t1].
	 */
	static class ClippedPath {
		public final GeneralPath g;
		private Stack<float[]> uncommittedPoints = new Stack<float[]>();
		private float initialX, initialY;
		
		public ClippedPath(int windingRule) {
			g = new GeneralPath(windingRule);
		}
		
		public void moveTo(float x,float y) {
			flush();
			g.moveTo(x,y);
			initialX = x;
			initialY = y;
		}
		
		/** This makes a cubic curve to based on xf and yf that ranges from
		 * [t0,t1].  So this takes a little subset of the curves if [t0,t1]
		 * is smaller than [0,1].
		 */
		public void curveTo(Function xf,Function yf,double t0,double t1) {
			flush(); //flush out lines

			double dt = (t1-t0);
			//I know I'm not explaining the math here, but you can derive
			//it with a little time and a few sheets of paper.  The API for
			//the PathIterator shows the equations relating to bezier parametric
			//curves.  From there you can calculate whatever you need:
			//it just might take a few pages of pen & paper.
			double dx0 = xf.getDerivative(t0)*dt;
			double dx1 = xf.getDerivative(t1)*dt;
			double dy0 = yf.getDerivative(t0)*dt;
			double dy1 = yf.getDerivative(t1)*dt;
			double x0 = xf.evaluate(t0);
			double x1 = xf.evaluate(t1);
			double y0 = yf.evaluate(t0);
			double y1 = yf.evaluate(t1);

			g.curveTo(
					(float)(x0+dx0/3),
					(float)(y0+dy0/3),
					(float)(x1-dx1/3),
					(float)(y1-dy1/3),
					(float)(x1),
					(float)(y1) );
		}
		
		/** Adds a line to (x,y)
		 * <P>This method doesn't actually commit a line until it's sure
		 * that it isn't writing heavily redundant lines.  That is the
		 * points (0,0), (5,0) and (2,0) would be consolidated so only
		 * the first and last point remained.
		 * <P>However only horizontal/vertical lines are consolidated,
		 * because this method is aimed at clipping to (non-rotated) rectangles.
		 */
		public void lineTo(float x,float y) {
			
			if(uncommittedPoints.size()>0) {
				float[] last = uncommittedPoints.peek();
				//are we adding the same point?
				if(Math.abs(last[0]-x)<TOLERANCE && Math.abs(last[1]-y)<TOLERANCE)
					return;
			}
			
			
			float[] f = new float[2];
			f[0] = x;
			f[1] = y;
			uncommittedPoints.push(f);
		}
		
		public void closePath() {
			lineTo(initialX,initialY);
			flush();
			g.closePath();
		}
		
		/** Flush out the stack of uncommitted points. */
		public void flush() {
			while(uncommittedPoints.size()>0) {
				identifyLines : while(uncommittedPoints.size()>=3) {
					float[] first = uncommittedPoints.get(0);
					float[] middle = uncommittedPoints.get(1);
					float[] last = uncommittedPoints.get(2);
					
					if(Math.abs(first[0]-middle[0])<TOLERANCE && Math.abs(first[0]-last[0])<TOLERANCE) {
						//everything has the same x, so we have a vertical line
						float[] array = uncommittedPoints.remove(1);
					} else if(Math.abs(first[1]-middle[1])<TOLERANCE && Math.abs(first[1]-last[1])<TOLERANCE) {
						//everything has the same y, so we have a horizontal line
						float[] array = uncommittedPoints.remove(1);
					} else {
						break identifyLines;
					}
				}
			
				float[] point = uncommittedPoints.remove(0);
				g.lineTo( point[0], point[1]);
			}
		}
	}
	
	/** A function used to describe one of the 2 parametric equations
	 * for a segment of a path.  This can be thought of is f(t).
	 */
	static interface Function {
		/** evaluates this function at a given value */
		public double evaluate(double t);
		
		/** Calculates all the t-values which will yield the result "f"
		 * in this function.
		 * 
		 * @param f the function result you're searching for
		 * @param dest the array the results will be stored in
		 * @param destOffset the offset at which data will be added to
		 * the array
		 * @return the number of solutions found.
		 */
		public int evaluateInverse(double f,double[] dest,int destOffset);
		  
		/** Return the derivative (df/dt) for a given value of t */
		public double getDerivative(double t);
	}
	
	/** A linear function */
	static class LFunction implements Function {
		double slope, intercept;
		public LFunction() {}

		/** Defines this linear function.
		 * 
		 * @param x1 at t = 0, x1 is the output of this function
		 * @param x2 at t = 1, x2 is the output of this function
		 */
		public void define(double x1,double x2) {
			slope = (x2-x1);
			intercept = x1;
		}
		
		@Override
		public String toString() {
			return slope+"*t+"+intercept;
		}
		
		public double evaluate(double t) {
			return slope*t+intercept;
		}
		
		public int evaluateInverse(double x,double[] dest,int offset) {
			dest[offset] = (x-intercept)/slope;
			return 1;
		}
		
		public double getDerivative(double t) {
			return slope;
		}
	}
	
	/** A quadratic function */
	static class QFunction implements Function {
		double a, b, c;
		
		public QFunction() {}
		
		@Override
		public String toString() {
			return a+"*t*t+"+b+"*t+"+c;
		}

		/** Use the 3 control points of a bezier quadratic
		 */
		public void define(double x0, double x1, double x2) {
			a = x0-2*x1+x2;
			b = -2*x0+2*x1;
			c = x0;
		}
		
		public double evaluate(double t) {
			return a*t*t+b*t+c;
		}
		
		public double getDerivative(double t) {
			return 2*a*t+b;
		}
		
		public int evaluateInverse(double x,double[] dest,int offset) {
			double C = c-x;
			double det = b*b-4*a*C;
			if(det<0)
				return 0;
			if(det==0) {
				dest[offset] = (-b)/(2*a);
				return 1;
			}
			det = Math.sqrt(det);
			dest[offset++] = (-b+det)/(2*a);
			dest[offset++] = (-b-det)/(2*a);
			return 2;
		}
	}
	
	/** A cubic function */
	static class CFunction implements Function {
		double a, b, c, d;
		
		public CFunction() {}
		
		@Override
		public String toString() {
			return a+"*t*t*t+"+b+"*t*t+"+c+"*t+"+d;
		}

		public void define(double x0,double x1, double x2,double x3) {
			a = -x0+3*x1-3*x2+x3;
			b = 3*x0-6*x1+3*x2;
			c = -3*x0+3*x1;
			d = x0;
		}
		
		public double evaluate(double t) {
			return a*t*t*t+b*t*t+c*t+d;
		}
		
		public double getDerivative(double t) {
			return 3*a*t*t+2*b*t+c;
		}
		
		/** Recycle arrays here.
		 * Remember this is possibly going to be 1
		 * object called hundreds of times, so reusing
		 * the same arrays here will save us time &
		 * memory allocation.  In current setup there
		 * is only 1 thread that will be using these
		 * values.
		 */
		double[] t2;
		double[] eqn;
		public int evaluateInverse(double x,double[] dest,int offset) {
			if(eqn==null)
				eqn = new double[4];
			eqn[0] = d-x;
			eqn[1] = c;
			eqn[2] = b;
			eqn[3] = a;
			if(offset==0) {
				int k = CubicCurve2D.solveCubic(eqn,dest);
				if(k<0) return 0;
				return k;
			}
			if(t2==null)
				t2 = new double[3];
			int k = CubicCurve2D.solveCubic(eqn,t2);
			if(k<0) return 0;
			for(int i = 0; i<k; i++) {
				dest[offset+i] = t2[i];
			}
			return k;
		}
	}
	
	/** This creates a <code>GeneralPath</code> representing <code>s</code> when
	 * clipped to <code>r</code>
	 * @param s a shape that you want clipped
	 * @param r the rectangle to clip to
	 * @return a <code>GeneralPath</code> enclosing the new shape.
	 */
	public static GeneralPath clipToRect(Shape s,Rectangle2D r) {
		return clipToRect(s,null,r);
	}

	/** This creates a <code>GeneralPath</code> representing <code>s</code> when
	 * clipped to <code>r</code>
	 * @param s a shape that you want clipped
	 * @param t the transform to transform <code>s</code> by.
	 * <P>This may be <code>null</code>, indicating that <code>s</code> should
	 * not be transformed.
	 * @param r the rectangle to clip to
	 * @return a <code>GeneralPath</code> enclosing the new shape.
	 */
	public static GeneralPath clipToRect(Shape s,AffineTransform t,Rectangle2D r) {
		Clipper clipper = new RectangleClipper(r);
		return clipper.clip(s, t);
	}
	
	private static class RectangleClipper extends Clipper {
		final float rTop;
		final float rLeft;
		final float rRight;
		final float rBottom;
		
		private RectangleClipper(Rectangle2D rect) {
			rTop = (float)rect.getY();
			rLeft = (float)rect.getX();
			rRight = (float)(rect.getX()+rect.getWidth());
			rBottom = (float)(rect.getY()+rect.getHeight());
		}
		
		@Override
		boolean contains(float x,float y) {
			return (x>=rLeft && x<=rRight && y>=rTop && y<=rBottom);
		}
		
		@Override
		void cap(Point2D.Float p) {
			if(p.x<rLeft)
				p.x = rLeft;
			if(p.x>rRight)
				p.x = rRight;
			if(p.y<rTop)
				p.y = rTop;
			if(p.y>rBottom)
				p.y = rBottom;
		}

		@Override
		int collectIntersectionTimes(Function xf, Function yf,
				double[] intersectionTimes) {
			int sum = 0;
			sum += xf.evaluateInverse(rLeft,intersectionTimes,sum);
			sum += xf.evaluateInverse(rRight,intersectionTimes,sum);
			sum += yf.evaluateInverse(rTop,intersectionTimes,sum);
			sum += yf.evaluateInverse(rBottom,intersectionTimes,sum);
			return sum;
		}
	}
	
	abstract void cap(Point2D.Float p);
	abstract boolean contains(float x,float y);
	/** Calculates the t-values for which this shape intersects the parametric function
	 * provided.  The values in the array are not expected to be sorted.
	 * 
	 * @param xf the x parametric curve
	 * @param yf the y parametric curve
	 * @param intersectionTimes the array to store the data in.
	 * @return the number of values provided.
	 */
	abstract int collectIntersectionTimes(Function xf,Function yf,double[] intersectionTimes);

	GeneralPath clip(Shape incomingShape,AffineTransform transform) {
		PathIterator i = incomingShape.getPathIterator(transform);
		ClippedPath p = new ClippedPath(i.getWindingRule());
		float initialX = 0;
		float initialY = 0;
		int k;
		float[] f = new float[6];
		boolean shouldClose = false;
		float lastX = 0;
		float lastY = 0;
		boolean lastValueWasCapped, thisValueIsCapped, midValueInvalid;
		float x, y, x2, y2;
		
		//create 1 copy of objects and recycle them
		//to reduce memory allocation:
		LFunction lxf = new LFunction();
		LFunction lyf = new LFunction();
		QFunction qxf = new QFunction();
		QFunction qyf = new QFunction();
		CFunction cxf = new CFunction();
		CFunction cyf = new CFunction();
		Function xf = null;
		Function yf = null;
		Point2D.Float point = new Point2D.Float();
		double[] intersectionTimes = new double[16];
		int tCtr;
		
		while(i.isDone()==false) {
			k = i.currentSegment(f);
			if(k==PathIterator.SEG_MOVETO) {
				initialX = f[0];
				initialY = f[1];
				point.setLocation(f[0], f[1]);
				cap(point);
				
				p.moveTo(point.x, point.y);

				lastX = f[0];
				lastY = f[1];
			} else if(k==PathIterator.SEG_CLOSE) {
				f[0] = initialX;
				f[1] = initialY;
				k = PathIterator.SEG_LINETO;
				shouldClose = true;
			}
			xf = null;
			if(k==PathIterator.SEG_LINETO) {
				lxf.define(lastX,f[0]);
				lyf.define(lastY,f[1]);
				
				xf = lxf;
				yf = lyf;
			} else if(k==PathIterator.SEG_QUADTO) {
				qxf.define(lastX,f[0],f[2]);
				qyf.define(lastY,f[1],f[3]);
				
				xf = qxf;
				yf = qyf;
			} else if(k==PathIterator.SEG_CUBICTO) {
				cxf.define(lastX,f[0],f[2],f[4]);
				cyf.define(lastY,f[1],f[3],f[5]);
				
				xf = cxf;
				yf = cyf;
			}
			if(xf!=null) {
				//gather all the t values at which we might be
				//crossing the bounds of our rectangle:
				
				tCtr = collectIntersectionTimes(xf, yf, intersectionTimes);
				intersectionTimes[tCtr++] = 1;
				 //we never actually calculate with 0, but we need to know it's in the list
				intersectionTimes[tCtr++] = 0;
				
				//put them in ascending order:
				Arrays.sort(intersectionTimes,0,tCtr);
				
				lastValueWasCapped = !contains(lastX, lastY);
				
				for(int a = 0; a<tCtr; a++) {
					if(a>0 && intersectionTimes[a]==intersectionTimes[a-1]) {
						//do nothing
					} else if(intersectionTimes[a]>0 && intersectionTimes[a]<=1) {
						//this is the magic: take 2 t values and see what we need to
						//do with them.
						//Remember we can make redundant horizontal/vertical lines
						//all we want to because the ClippedPath will clean up
						//the mess.
						x = (float)xf.evaluate(intersectionTimes[a]);
						y = (float)yf.evaluate(intersectionTimes[a]);
						point.setLocation(x, y);
						cap(point);
						
						thisValueIsCapped = !(Math.abs(x-point.x)<TOLERANCE && Math.abs(y-point.y)<TOLERANCE);
						
						x2 = (float)xf.evaluate((intersectionTimes[a]+intersectionTimes[a-1])/2);
						y2 = (float)yf.evaluate((intersectionTimes[a]+intersectionTimes[a-1])/2);
						midValueInvalid = !contains(x2, y2);
							
						if(( xf instanceof LFunction) || thisValueIsCapped || lastValueWasCapped || midValueInvalid ) {
							p.lineTo(point.x, point.y);
						} else if((xf instanceof QFunction) || (xf instanceof CFunction)) {
							p.curveTo(xf,yf,intersectionTimes[a-1],intersectionTimes[a]);
						} else {
							throw new RuntimeException("Unexpected condition.");
						}
						
						lastValueWasCapped = thisValueIsCapped;
					}
				}
				lastX = (float)xf.evaluate(1);
				lastY = (float)yf.evaluate(1);
			}
			if(shouldClose) {
				p.closePath();
				shouldClose = false;
			}
			i.next();
		}
		p.flush();
		return p.g;
	}

	/** By default if a Graphics2D is asked to clip to a new shape,
	 * it may resort to Area objects if either the current clipping
	 * and the new clipping are not rectangles.
	 * <P>This method with offer a slight improvement over this model:
	 * if <i>either</i> the old clip or the new clip is a rectangle,
	 * then this uses the <code>Clipper.clipToRect()</code> method.
	 * This avoids the slow-but-accurate Area class.
	 * <P>This should only be used to replace <code>Graphics2D.clip()</code>,
	 * not <code>Graphics2D.setClip()</code>.
	 * @param g the graphics2D to clip to
	 * @param newClip the new clip
	 */
	public static void clip(Graphics2D g,Shape newClip) {
		Shape oldClip = g.getClip();
		if(oldClip==null) {
			g.setClip(newClip);
			return;
		}
		Rectangle2D oldRect = RectangleReader.convert(oldClip);
		Rectangle2D newRect = RectangleReader.convert(newClip);
		
		if(oldRect!=null && newRect!=null) {
			Rectangle2D intersectedClip = oldRect.createIntersection(newRect);
			if(intersectedClip.getWidth()<0 || intersectedClip.getHeight()<0) {
				//a negative width or height indicates there's no real intersection
				intersectedClip.setFrame(intersectedClip.getX(), intersectedClip.getY(), 0, 0);
			}
			g.setClip( intersectedClip );
			return;
		}
		
		if(newRect!=null && oldRect==null) {
			GeneralPath intersectedClip = Clipper.clipToRect(oldClip, newRect);
			g.setClip( intersectedClip);
			return;
		}
		
		if(newRect==null && oldRect!=null) {
			GeneralPath intersectedClip = Clipper.clipToRect(newClip, oldRect);
			g.setClip( intersectedClip );
			return;
		}

		g.clip(newClip);
	}
}
