/*
 * @(#)MeasuredShape.java
 *
 * $Date: 2014-04-06 05:02:15 +0200 (V, 06 ápr. 2014) $
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
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Vector;

/** This represents a single closed path.
 * <P>This object can trace arbitrary amounts of itself using the
 * <code>writeShape()</code> methods.
 * 
 */
public class MeasuredShape implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/** Because a MeasuredShape must be exactly 1 subpath, this method
	 * will safely break up a path into separate subpaths and create one
	 * MeasuredShape for each. 
	 * 
	 * @param s a path, possibly containing multiple subpaths
	 * @return a MeasuredShape object for each subpath in <code>i</code>
	 */
	public static MeasuredShape[] getSubpaths(Shape s) {
		return getSubpaths(s.getPathIterator(null),DEFAULT_SPACING);
	}

	/** Because a MeasuredShape must be exactly 1 subpath, this method
	 * will safely break up a path into separate subpaths and create one
	 * MeasuredShape for each. 
	 * 
	 * @param s a path, possibly containing multiple subpaths
	 * @param spacing the spacing to be used for each <code>MeasuredShape</code>
	 * @return a MeasuredShape object for each subpath in <code>i</code>
	 */
	public static MeasuredShape[] getSubpaths(Shape s,float spacing) {
		return getSubpaths(s.getPathIterator(null),spacing);
	}
	
	/** Because a MeasuredShape must be exactly 1 subpath, this method
	 * will safely break up a path into separate subpaths and create one
	 * MeasuredShape for each. 
	 * 
	 * @param i a path, possibly containing multiple subpaths
	 * @return a MeasuredShape object for each subpath in <code>i</code>
	 */
	public static MeasuredShape[] getSubpaths(PathIterator i) {
		return getSubpaths(i,DEFAULT_SPACING);
	}
	
	/** Because a MeasuredShape must be exactly 1 subpath, this method
	 * will safely break up a path into separate subpaths and create one
	 * MeasuredShape for each. 
	 * 
	 * @param i a path, possibly containing multiple subpaths
	 * @return a MeasuredShape object for each subpath in <code>i</code>
	 */
	public static MeasuredShape[] getSubpaths(PathIterator i,float spacing) {
		Vector<MeasuredShape> v = new Vector<MeasuredShape>();
		GeneralPath path = null;
		float[] coords = new float[6];
		while(i.isDone()==false) {
			int k = i.currentSegment(coords);
			if(k==PathIterator.SEG_MOVETO) {
				if(path!=null) {
					v.add(new MeasuredShape(path,spacing));
					path = null;
				}
				path = new GeneralPath();
				path.moveTo(coords[0],coords[1]);
			} else if(k==PathIterator.SEG_LINETO) {
				path.lineTo(coords[0],coords[1]);
			} else if(k==PathIterator.SEG_QUADTO) {
				path.quadTo(coords[0],coords[1],coords[2],coords[3]);
			} else if(k==PathIterator.SEG_CUBICTO) {
				path.curveTo(coords[0],coords[1],coords[2],coords[3],coords[4],coords[5]);
			} else if(k==PathIterator.SEG_CLOSE) {
				path.closePath();
			}
			i.next();
		}
		if(path!=null) {
			v.add(new MeasuredShape(path,spacing));
			path = null;
		}
		return v.toArray(new MeasuredShape[v.size()]);
	}
	
	static class Segment implements Serializable {
		private static final long serialVersionUID = 1L;
		
		int type;
		float[] data;
		float realDistance;
		float normalizedDistance;
		
		public void write(PathWriter path,float t0,float t1) {
			if(t0==0 && t1==1) {
				if(type==PathIterator.SEG_MOVETO) {
					path.moveTo(data[0], data[1]);
				} else if(type==PathIterator.SEG_LINETO) {
					path.lineTo(data[2], data[3]);
				} else if(type==PathIterator.SEG_QUADTO) {
					path.quadTo(data[2],data[3],data[4],data[5]);
				} else if(type==PathIterator.SEG_CUBICTO) {
					path.curveTo(data[2],data[3],data[4],data[5],data[6],data[7]);
				} else {
					throw new RuntimeException();
				}
				return;
			} else if(t0==1 && t1==0) {
				if(type==PathIterator.SEG_MOVETO) {
					path.moveTo(data[0], data[1]);
				} else if(type==PathIterator.SEG_LINETO) {
					path.lineTo(data[0], data[1]);
				} else if(type==PathIterator.SEG_QUADTO) {
					path.quadTo(data[2],data[3],data[0],data[1]);
				} else if(type==PathIterator.SEG_CUBICTO) {
					path.curveTo(data[4],data[5],data[2],data[3],data[0],data[1]);
				} else {
					throw new RuntimeException();
				}
				return;
			}
			if(type==PathIterator.SEG_MOVETO) {
				path.moveTo(data[0], data[1]); //not sure what this means?
			} else if(type==PathIterator.SEG_LINETO) {
				path.lineTo(getX(t1),getY(t1));
			} else if(type==PathIterator.SEG_QUADTO) {
				float ax = data[0]-2*data[2]+data[4];
	 			float bx = -2*data[0]+2*data[2];
				float cx = data[0];
				float ay = data[1]-2*data[3]+data[5];
				float by = -2*data[1]+2*data[3];
				float cy = data[1];
				
				PathWriter.quadTo(path, t0, t1, ax, bx, cx, ay, by, cy);
			} else if(type==PathIterator.SEG_CUBICTO) {
				float ax = -data[0]+3*data[2]-3*data[4]+data[6];
				float bx = 3*data[0]-6*data[2]+3*data[4];
				float cx = -3*data[0]+3*data[2];
				float dx = data[0];
				float ay = -data[1]+3*data[3]-3*data[5]+data[7];
				float by = 3*data[1]-6*data[3]+3*data[5];
				float cy = -3*data[1]+3*data[3];
				float dy = data[1];
				PathWriter.cubicTo(path, t0, t1, ax, bx, cx, dx, ay, by, cy, dy);
			} else if(type==PathIterator.SEG_CLOSE) {
				path.closePath();
			} else {
				throw new RuntimeException();
			}
		}
		
		public float getTangentSlope(float t) {
			if(type==PathIterator.SEG_LINETO) {
				float ax = data[2]-data[0];
				float ay = data[3]-data[1];
				return (float)Math.atan2(ay, ax);
			} else if(type==PathIterator.SEG_QUADTO) {
				float ax = data[0]-2*data[2]+data[4];
				float bx = -2*data[0]+2*data[2];
				float ay = data[1]-2*data[3]+data[5];
				float by = -2*data[1]+2*data[3];
				return (float)Math.atan2( 2*ay*t+by, 2*ax*t+bx  );
			} else if(type==PathIterator.SEG_CUBICTO) {
				float ax = -data[0]+3*data[2]-3*data[4]+data[6];
				float bx = 3*data[0]-6*data[2]+3*data[4];
				float cx = -3*data[0]+3*data[2];
				float ay = -data[1]+3*data[3]-3*data[5]+data[7];
				float by = 3*data[1]-6*data[3]+3*data[5];
				float cy = -3*data[1]+3*data[3];
				return (float)Math.atan2( 3*ay*t*t+2*by*t+cy, 3*ax*t*t+2*bx*t+cx );
			} else if(type==PathIterator.SEG_MOVETO) {
				return data[0];
			} else if(type==PathIterator.SEG_CLOSE) {
				throw new RuntimeException();
			} else {
				throw new RuntimeException();
			}
		}
		
		public float getX(float t) {
			if(type==PathIterator.SEG_LINETO) {
				float ax = data[2]-data[0];
				return ax*t+data[0];
			} else if(type==PathIterator.SEG_QUADTO) {
				float ax = data[0]-2*data[2]+data[4];
				float bx = -2*data[0]+2*data[2];
				float cx = data[0];
				return (ax*t+bx)*t+cx;
			} else if(type==PathIterator.SEG_CUBICTO) {
				float ax = -data[0]+3*data[2]-3*data[4]+data[6];
				float bx = 3*data[0]-6*data[2]+3*data[4];
				float cx = -3*data[0]+3*data[2];
				float dx = data[0];
				return ((ax*t+bx)*t+cx)*t+dx;
			} else if(type==PathIterator.SEG_MOVETO) {
				return data[0];
			} else if(type==PathIterator.SEG_CLOSE) {
				throw new RuntimeException();
			} else {
				throw new RuntimeException();
			}
		}

		public float getY(float t) {
			if(type==PathIterator.SEG_LINETO) {
				float ay = data[3]-data[1];
				return ay*t+data[1];
			} else if(type==PathIterator.SEG_QUADTO) {
				float ay = data[1]-2*data[3]+data[5];
				float by = -2*data[1]+2*data[3];
				float cy = data[1];
				return (ay*t+by)*t+cy;
			} else if(type==PathIterator.SEG_CUBICTO) {
				float ay = -data[1]+3*data[3]-3*data[5]+data[7];
				float by = 3*data[1]-6*data[3]+3*data[5];
				float cy = -3*data[1]+3*data[3];
				float dy = data[1];
				return ((ay*t+by)*t+cy)*t+dy;
			} else if(type==PathIterator.SEG_MOVETO) {
				return data[1];
			} else if(type==PathIterator.SEG_CLOSE) {
				throw new RuntimeException();
			} else {
				throw new RuntimeException();
			}
		}
		
		public Segment(int type,float lastX,float lastY,float[] coords,float spacing) {
			this.type = type;
			if(type==PathIterator.SEG_MOVETO) {
				data = new float[] {coords[0],coords[1]};
				realDistance = 0;
			} else if(type==PathIterator.SEG_LINETO) {
				data = new float[] {lastX, lastY, coords[0],coords[1]};
				realDistance = (float)(Math.sqrt(
						(coords[0]-lastX)*(coords[0]-lastX) + (coords[1]-lastY)*(coords[1]-lastY) ));
			} else if(type==PathIterator.SEG_CLOSE) {
				data = new float[0];
			} else {
				double ax, bx, cx, dx, ay, by, cy, dy;
				if(type==PathIterator.SEG_QUADTO) {
					ay = 0;
					by = lastY-2*coords[1]+coords[3];
					cy = -2*lastY+2*coords[1];
					dy = lastY;
                
					ax = 0;
					bx = lastX-2*coords[0]+coords[2];
					cx = -2*lastX+2*coords[0];
					dx = lastX;
					data = new float[] {lastX, lastY, coords[0], coords[1], coords[2], coords[3]};
				} else if(type==PathIterator.SEG_CUBICTO) {
					ay = -lastY+3*coords[1]-3*coords[3]+coords[5];
					by = 3*lastY-6*coords[1]+3*coords[3];
					cy = -3*lastY+3*coords[1];
					dy = lastY;
                
					ax = -lastX+3*coords[0]-3*coords[2]+coords[4];
					bx = 3*lastX-6*coords[0]+3*coords[2];
					cx = -3*lastX+3*coords[0];
					dx = lastX;
					data = new float[] {lastX, lastY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]};
				} else {
					throw new RuntimeException("Unrecognized type: "+type);
				}
				realDistance = calculateDistance(ax,bx,cx,dx,ay,by,cy,dy,spacing);
			}
		}
		
		private float calculateDistance(double ax,double bx,double cx,double dx,
				double ay,double by,double cy,double dy,float spacing) {
			double x0 = dx;
			double y0 = dy;
			double x1, y1;
			
			double sum = 0;
			for(double t = spacing; t<1; t+=spacing) {
				x1 = ((ax*t+bx)*t+cx)*t+dx;
				y1 = ((ay*t+by)*t+cy)*t+dy;
				sum += Math.sqrt( (x0-x1)*(x0-x1)+(y0-y1)*(y0-y1) );
				x0 = x1;
				y0 = y1;
			}
			return (float)sum;
		}
	}

	/** This is the increments t goes throw as each shape segment is
	 * traversed.  For quadratic and cubic curves, this affects
	 * how the shape distance is measured.  The default value is .05,
	 * meaning quadratic and cubic curves are converted to linear segments
	 * connecting at t = 0, t = .05, t = .1, ... t = .95, t = 1.
	 */
	public static final float DEFAULT_SPACING = .05f;
	
	Segment[] segments;
	float closedDistance = 0;
	float originalDistance;

	/** Construct a <code>MeasuredShape</code> from a <code>Shape</code>,
	 * using the default spacing.
	 * 
	 * @param s the shape data
	 * @throws IllegalArgumentException if the shape has more than 1 path.
	 */
	public MeasuredShape(Shape s) {
		this(s.getPathIterator(null),DEFAULT_SPACING);
	}
	
	/** Construct a <code>MeasuredShape</code> from a <code>Shape</code>.
	 * 
	 * @param s the shape data to create
	 * @param spacing the value to increment t as each segment is traversed.
	 * The default value is .05.
	 * @throws IllegalArgumentException if the shape has more than 1 path.
	 */
	public MeasuredShape(Shape s,float spacing) {
		this(s.getPathIterator(null),spacing);
	}

	/** Construct a <code>MeasuredShape</code> from a <code>PathIterator</code>
	 * using the default spacing.
	 * 
	 * @param i the shape data to create
	 * @throws IllegalArgumentException if the shape has more than 1 path.
	 */
	public MeasuredShape(PathIterator i) {
		this(i,DEFAULT_SPACING);
	}
	
	/** Construct a <code>MeasuredShape</code> from a <code>PathIterator</code>.
	 * 
	 * @param i the shape data to create
	 * @param spacing the value to increment t as each segment is traversed.
	 * The default value is .05.
	 * @throws IllegalArgumentException if the shape has more than 1 path.
	 */
	public MeasuredShape(PathIterator i,float spacing) {
		Vector<Segment> v = new Vector<Segment>();
		float lastX = 0;
		float lastY = 0;
		float moveX = 0;
		float moveY = 0;
		int pathCount = 0; 
		boolean closed = false;
		
		float[] coords = new float[6];
		while(i.isDone()==false) {
			int k = i.currentSegment(coords);
			if(k==PathIterator.SEG_CLOSE) {
				closed = true;
			} else if(k==PathIterator.SEG_MOVETO) {
				if(pathCount==1)
					throw new IllegalArgumentException("this object can only contain 1 subpath");
				moveX = coords[0];
				moveY = coords[1];
				lastX = moveX;
				lastY = moveY;
				pathCount++;
			} else if(k==PathIterator.SEG_LINETO ||
					k==PathIterator.SEG_QUADTO ||
					k==PathIterator.SEG_CUBICTO) {
				if(pathCount!=1)
					throw new IllegalArgumentException("this shape data did not begin with a moveTo");
				Segment s = new Segment(k,lastX,lastY,coords,spacing);
				lastX = s.data[s.data.length-2];
				lastY = s.data[s.data.length-1];
				v.add(s);
				closedDistance += s.realDistance;
			}
			i.next();
		}
		float t = closedDistance;
		if(v.size()>0) {
			Segment last = v.get(v.size()-1);
			if(Math.abs(last.data[last.data.length-2]-moveX)>.001 ||
					Math.abs(last.data[last.data.length-1]-moveY)>.001) {
				coords[0] = moveX;
				coords[1] = moveY;
				Segment s = new Segment(PathIterator.SEG_LINETO,lastX,lastY,coords,spacing);
				v.add(s);
				closedDistance += s.realDistance;
			}
		}
		if(!closed) {
			originalDistance = t;
		} else {
			originalDistance = closedDistance;
		}
		
		segments = v.toArray(new Segment[v.size()]);
		//normalize everything:
		for(int a = 0; a<segments.length; a++) {
			segments[a].normalizedDistance = segments[a].realDistance/closedDistance;
		}
	}
	
	/** Writes the entire shape
	 * @param w the destination to write to
	 */
	public void writeShape(PathWriter w) {
		w.moveTo(segments[0].getX(0), 
				segments[0].getY(0) );
		for(int a = 0; a<segments.length; a++) {
			segments[a].write(w,0,1);
		}
		w.closePath();
	}
	
	/** The distance of this shape, assuming that the path is closed.
	 * This will be greater than or equal to <code>getOriginalDistance()</code>.
	 * 
	 * @see #getOriginalDistance()
	 */
	public float getClosedDistance() {
		return closedDistance;
	}
	

	/** The distance of the shape used to construct this
	 * <code>MeasuredShape</code>.
	 * <p>This will be less than or equal to <code>getClosedDistance()</code>.
	 * 
	 * @return The distance this path covered when the shape was constructed.
	 * 
	 * @see #getClosedDistance()
	 */
	public float getOriginalDistance() {
		return originalDistance;
	}

	/** Writes the entire shape backwards
	 * @param w the destination to write to
	 */
	public void writeShapeBackwards(PathWriter w) {
		w.moveTo(segments[segments.length-1].getX(1), 
				segments[segments.length-1].getY(1) );
		for(int a = segments.length-1; a>=0; a--) {
			segments[a].write(w,1,0);
		}
		w.closePath();
	}
	
	/** Returns the x-value of where this path begins.
	 * <P>Because a <code>MeasuredShape</code> can only be one
	 * path, there is only possible <code>moveTo()</code>.
	 * 
	 * @return the x-value of where this path begins.
	 */
	public float getMoveToX() {
		Segment s = segments[0];
		return s.getX(0);
	}


	/** Returns the y-value of where this path begins.
	 * <P>Because a <code>MeasuredShape</code> can only be one
	 * path, there is only possible <code>moveTo()</code>.
	 * 
	 * @return the y-value of where this path begins.
	 */
	public float getMoveToY() {
		Segment s = segments[0];
		return s.getY(0);
	}
	

	/** Trace the shape.
	 * 
	 * @param position a fraction from zero to one indicating where to start tracing
	 * @param length a fraction from negative one to one indicating how much to trace.
	 * If this value is negative then the shape will be traced backwards.
	 * @param w the destination to write to
	 */
	public void writeShape(float position,float length,PathWriter w) {
		writeShape(position,length,w,true);
	}

	/** Trace the shape.
	 * 
	 * @param position a fraction from zero to one indicating where to start tracing
	 * @param length a fraction from negative one to one indicating how much to trace.
	 * If this value is negative then the shape will be traced backwards.
	 * @param w the destination to write to
	 * @param includeMoveTo this controls whether a moveTo is the first thing
	 * written to the path.
	 * Note setting this to <code>false</code> means its the caller's responsibility
	 * to make sure the path is in the correct position.
	 */
	public void writeShape(float position,float length,PathWriter w,boolean includeMoveTo) {
		if(length>=.999999f) {
			writeShape(w);
			return;
		} else if(length<=-.999999f) {
			writeShapeBackwards(w);
			return;
		} else if(length<.000001 && length>-.000001) {
			return;
		}
		
		Position i1 = getIndexOfPosition(position);
		Position i2 = getIndexOfPosition(position+length);
		
		if(includeMoveTo) {
			w.moveTo(segments[i1.i].getX(i1.innerPosition),
					segments[i1.i].getY(i1.innerPosition));
		}
		if(i1.i==i2.i && ((length>0 && i2.innerPosition>i1.innerPosition)
				|| (length<0 && i2.innerPosition<i1.innerPosition) )) {
			segments[i1.i].write(w,i1.innerPosition,i2.innerPosition);
		} else {
			if(length>0) {
				segments[i1.i].write(w,i1.innerPosition,1);
				int i = i1.i+1;
				if(i>=segments.length)
					i = 0;
				while(i!=i2.i) {
					segments[i].write(w,0,1);
					i++;
					if(i>=segments.length)
						i = 0;
				}
				segments[i2.i].write(w,0,i2.innerPosition);
			} else {
				segments[i1.i].write(w,i1.innerPosition,0);
				int i = i1.i-1;
				if(i<0)
					i = segments.length-1;
				while(i!=i2.i) {
					segments[i].write(w,1,0);
					i--;
					if(i<0)
						i = segments.length-1;
				}
				segments[i2.i].write(w,1,i2.innerPosition);
			}
		}
		
	}
	
	/** Returns the point at a certain distance from the beginning of this shape.
	 * 
	 * @param distance the distance from the beginning of this shape to measure
	 * @param dest the destination to store the result in.  (If this is null a new
	 * Point2D will be constructed.)
	 * @return the point at a certain distance from the beginning of this shape.
	 * Note this will be <code>dest</code> if <code>dest</code> is non-null.
	 */
	public Point2D getPoint(float distance,Point2D dest) {
		if(distance<0) throw new IllegalArgumentException("distance ("+distance+") must not be negative");
		if(distance>closedDistance) throw new IllegalArgumentException("distance ("+distance+") must not be greater than the total distance of this shape ("+closedDistance+")");
		if(dest==null) dest = new Point2D.Float();
		for(int a = 0; a<segments.length; a++) {
			float t = distance/segments[a].realDistance;
			if(t>=1) {
				distance = distance - segments[a].realDistance;
			} else {
				dest.setLocation(segments[a].getX(t),segments[a].getY(t));
				return dest;
			}
		}
		dest.setLocation(segments[0].getX(0),segments[0].getY(0)); //a fluke case, where we're basically at the end of the shape
		return dest;
	}

	
	/** Returns the tangent slope at a certain distance from the beginning of this shape.
	 * The behavior of this method when the point you request falls exactly on an edge
	 * (that is, when two bordering segments don't have a continuous slope) is undefined.
	 * 
	 * @param distance the distance from the beginning of this shape to measure
	 * @return the tangent slope (in radians) at a specific position
	 */
	public float getTangentSlope(float distance) {
		if(distance<0) throw new IllegalArgumentException("distance ("+distance+") must not be negative");
		if(distance>closedDistance) throw new IllegalArgumentException("distance ("+distance+") must not be greater than the total distance of this shape ("+closedDistance+")");
		for(int a = 0; a<segments.length; a++) {
			float t = distance/segments[a].realDistance;
			if(t>=1) {
				distance = distance - segments[a].realDistance;
			} else {
				return segments[a].getTangentSlope(t);
			}
		}
		return segments[0].getTangentSlope(0); //a fluke case, where we're basically at the end of the shape
	}
	
	private static boolean equal(float f1,float f2) {
		float d = f1-f2;
		if(d<0) d = -d;
		return d<.0001;
	}

	/** Returns the length that this shape has in common with the argument.
	 * This assumes the two shapes begin at the same point, and in the same
	 * direction.
	 * @param s
	 */
	public float getCommonDistance(MeasuredShape s) {
		float distance = 0;
		int m = Math.min(segments.length, s.segments.length);
		for(int a = 0; a<m; a++) {
			if(segments[a].type!=PathIterator.SEG_MOVETO &&
					s.segments[a].type!=PathIterator.SEG_MOVETO) {
				if(equal(segments[a].data[0],s.segments[a].data[0]) &&
						equal(segments[a].data[1],s.segments[a].data[1]) &&
						equal(segments[a].data[segments[a].data.length-2], s.segments[a].data[s.segments[a].data.length-2]) &&
						equal(segments[a].data[segments[a].data.length-1], s.segments[a].data[s.segments[a].data.length-1]) &&
						equal(segments[a].realDistance, s.segments[a].realDistance)) {
					distance += segments[a].realDistance;
				} else {
					return distance;
				}
			} else if(segments[a].type==PathIterator.SEG_MOVETO &&
					s.segments[a].type==PathIterator.SEG_MOVETO) {
				//skip
			} else {
				return distance;
			}
		}
		return distance;
	}
	
	/** Trace the shape.
	 * 
	 * @param position a fraction from zero to one indicating where to start tracing
	 * @param length a fraction from negative one to one indicating how much to trace.
	 * If this value is negative then the shape will be traced backwards.
	 * @return a new path
	 */
	public GeneralPath getShape(float position,float length) {
		GeneralPath dest = new GeneralPath(Path2D.WIND_NON_ZERO);
		PathWriter w = new GeneralPathWriter(dest);
		writeShape(position,length,w,true);
		return dest;
	}
	
	static class Position {
		int i;
		float innerPosition;
		
		public Position(int segmentIndex,float p) {
			this.i = segmentIndex;
			this.innerPosition = p;
		}
		
		@Override
		public String toString() {
			return "Position[ i="+i+" t="+innerPosition+"]";
		}
	}
	
	private Position getIndexOfPosition(float p) {
		while(p<0) p+=1;
		while(p>1) p-=1;
		if(p>.99999f)
			p = 0;
		
		int i = 0;
		float original = p;
		while(i<segments.length) {
			if(p<=segments[i].normalizedDistance && segments[i].normalizedDistance!=0) {
				return new Position(i,p/segments[i].normalizedDistance);
			}
			p-=segments[i].normalizedDistance;
			i++;
		}
		System.err.println("p = "+p);
		throw new RuntimeException("the position "+original+" could not be found.");
	}
}
