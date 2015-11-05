/*
 * @(#)CalligraphyPathWriter.java
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

import com.bric.geom.GeneralPathWriter;
import com.bric.geom.PathSegment;
import com.bric.geom.PathWriter;
import com.bric.geom.SimplifiedPathIterator;
import net.jafama.FastMath;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

/** This creates a calligraphic or ribbon-like effect while
 * tracing a shape.
 * <p>There are two separate shape bodies being written.  If you
 * paint them separately: this creates an "over" and "under" effect.
 * If you paint them the same color: then this class is basically
 * a <code>java.awt.Stroke</code>.
 */
public class CalligraphyPathWriter extends PathWriter {
	private final float angle, offset1, offset2;
	private final PathWriter dest1, dest2;
	
	/** null=undefined, true=dest1, false=dest2
	 */
	private Boolean writingTrack1;
	private PathSegment.Float tail;
	private float moveX, moveY;
	private final AffineTransform rotate;
	
	/** This is only used internally when we require a PathWriter to
	 * write fragments of paths.
	 */
	PathWriter tailWriter = new PathWriter() {
		@Override
		public void moveTo(float x, float y) {
			throw new UnsupportedOperationException("moveTo("+x+", "+y+")");
		}

		@Override
		public void lineTo(float x, float y) {
			tail = tail.lineTo(x, y);
		}

		@Override
		public void quadTo(float cx, float cy, float x, float y) {
			tail = tail.quadTo(cx, cy, x, y);
		}

		@Override
		public void curveTo(float cx1, float cy1, float cx2, float cy2,
				float x, float y) {
			tail = tail.cubicTo(cx1, cy1, cx2, cy2, x, y);
		}

		@Override
		public void closePath() {
			throw new UnsupportedOperationException("closePath()");
		}

		@Override
		public void flush() {}
	};
	
	/** Create a new <code>CalligraphyPathWriter</code>.
	 * 
	 * @param angle the angle of the nib in this path.
	 * @param offset1 the offset of one side of the traced shape.
	 * One side of shape you trace will be offset by:
	 * <br>(+offset1*cos(angle), +offset1*sin(angle) )
	 * <p>It is possible for this value to be zero, which means one
	 * side of this path will exactly line up with the original shape
	 * being traced.
	 * <p>If both offsets are equal: then no new path segments will be
	 * visible because they will be pencil then.
	 * <p>To achieve a simple stroke: one offset should be (+width/2),
	 * and the other offset should be (-width/2).
	 * @param offset2 the offset of the other side of the traced shape.
	 * This side of shape will be offset by:
	 * <br>(+offset2*cos(angle), +offset2*sin(angle) )
	 * @param destination the destination for the data to write.
	 */
	public CalligraphyPathWriter(float angle,float offset1,float offset2,GeneralPath destination) {
		this(angle, offset1, offset2, new GeneralPathWriter(destination), new GeneralPathWriter(destination) );
	}

	/** Create a new <code>CalligraphyPathWriter</code>.
	 * 
	 * @param angle the angle of the nib in this path.
	 * @param offset1 the offset of one side of the traced shape.
	 * One side of shape you trace will be offset by:
	 * <br>(+offset1*cos(angle), +offset1*sin(angle) )
	 * <p>It is possible for this value to be zero, which means one
	 * side of this path will exactly line up with the original shape
	 * being traced.
	 * <p>If both offsets are equal: then no new path segments will be
	 * visible because they will be pencil then.
	 * <p>To achieve a simple stroke: one offset should be (+width/2),
	 * and the other offset should be (-width/2).
	 * @param offset2 the offset of the other side of the traced shape.
	 * This side of shape will be offset by:
	 * <br>(+offset2*cos(angle), +offset2*sin(angle) )
	 * @param dest1 the destination for half of the calligraphic shape.
	 * @param dest2 the destination for the other half.  This may be the
	 * same as <code>dest1</code> if you plan on painting this path
	 * at the same time with the same fill.
	 */
	public CalligraphyPathWriter(float angle,float offset1,float offset2,PathWriter dest1,PathWriter dest2) {
		checkArgument(angle, "angle is invalid");
		checkArgument(offset1, "offset1 is invalid");
		checkArgument(offset2, "offset2 is invalid");
		if(dest1==null && dest2==null) throw new NullPointerException();
		this.angle = angle;
		this.offset1 = offset1;
		this.offset2 = offset2;
		this.dest1 = dest1;
		this.dest2 = dest2;
		rotate = AffineTransform.getRotateInstance(-angle);
	}
	
	private void checkArgument(float value,String text) {
		if(Float.isInfinite(value) || Float.isNaN(value))
			throw new IllegalArgumentException(text+" ("+value+")");
	}
	
	@Override
	public synchronized void moveTo(float x, float y) {
		checkArgument(x, "x is invalid");
		checkArgument(y, "y is invalid");
		flush( x, y );
		moveX = x;
		moveY = y;
	}
	
	@Override
	public synchronized void lineTo(final float x,final float y) {
		checkArgument(x, "x is invalid");
		checkArgument(y, "y is invalid");
		if(tail==null) throw new NullPointerException("missing moveTo segment");

		PathSegment.Float moveTo = new PathSegment.Float( tail.data[tail.data.length-2], tail.data[tail.data.length-1] );
		PathSegment.Float line = moveTo.lineTo(x, y);
		
		defineTrack( 0, 1, line);
		
		tail = tail.lineTo(x, y);
	}
	
	@Override
	public synchronized void quadTo(final float cx, final float cy,final float x,final float y) {
		checkArgument(cx, "cx is invalid");
		checkArgument(cy, "cy is invalid");
		checkArgument(x, "x is invalid");
		checkArgument(y, "y is invalid");
		if(tail==null) throw new NullPointerException("missing moveTo segment");
		
		PathSegment.Float moveTo = new PathSegment.Float( tail.data[tail.data.length-2], tail.data[tail.data.length-1] );
		PathSegment.Float quad = moveTo.quadTo(cx, cy, x, y);

		//first, check to see if this is really quad data:
		int simplifiedSegment = SimplifiedPathIterator.simplify(PathIterator.SEG_QUADTO, moveTo.data[0], moveTo.data[1], quad.data);
		if(simplifiedSegment==PathIterator.SEG_LINETO) {
			lineTo( quad.data[0], quad.data[1] );
			return;
		}
		
		float[] ry_ = quad.getYCoeffs(rotate);
		
		float t = -ry_[1]/(2*ry_[0]);
		if(t>=0 && t<=1) {
			float[] x_ = quad.getXCoeffs();
			float[] y_ = quad.getYCoeffs();
			
			defineTrack( 0, t, quad);
			PathWriter.quadTo(tailWriter, 0, t, x_[0], x_[1], x_[2], 
					y_[0], y_[1], y_[2]);
			flush( quad.getX(t), quad.getY(t) );

			defineTrack( t, 1, quad);
			PathWriter.quadTo(tailWriter, t, 1, x_[0], x_[1], x_[2], 
					y_[0], y_[1], y_[2]);
		} else {
			defineTrack( 0, 1, quad);
			tail = tail.quadTo(cx, cy, x, y);
		}
		
	}
	@Override
	public synchronized void curveTo(float cx1, float cy1, float cx2, float cy2,
			float x, float y) {
		checkArgument(cx1, "cx1 is invalid");
		checkArgument(cy1, "cy1 is invalid");
		checkArgument(cx2, "cx2 is invalid");
		checkArgument(cy2, "cy2 is invalid");
		checkArgument(x, "x is invalid");
		checkArgument(y, "y is invalid");
		if(tail==null) throw new NullPointerException("missing moveTo segment");
		
		PathSegment.Float moveTo = new PathSegment.Float( tail.data[tail.data.length-2], tail.data[tail.data.length-1] );
		PathSegment.Float cubic = moveTo.cubicTo(cx1, cy1, cx2, cy2, x, y);
		
		//first, check to see if this is really cubic data:
		int simplifiedSegment = SimplifiedPathIterator.simplify(PathIterator.SEG_CUBICTO, moveTo.data[0], moveTo.data[1], cubic.data);
		if(simplifiedSegment==PathIterator.SEG_LINETO) {
			lineTo( cubic.data[0], cubic.data[1] );
			return;
		} else if(simplifiedSegment==PathIterator.SEG_QUADTO) {
			quadTo( cubic.data[0], cubic.data[1], cubic.data[2], cubic.data[3] );
			return;
		}
		
		float[] ry_ = cubic.getYCoeffs(rotate);
		float[] times = null;
		if( Math.abs(ry_[0])<.00001) {
			//the leading coefficent is effectively 0, so
			//instead of ay*(t^3)+by*(t^2)+cy*t+dy we have
			//0+bx*(t^2)+cx*t+dx

			float t = -ry_[2]/(2*ry_[1]);
			if(t>=0 && t<=1) {
				times = new float[] { t };
			}
		} else {
			float determinant = 4*ry_[1]*ry_[1]-12*ry_[0]*ry_[2];
			if(determinant>=0) {
				float t1 = -1;
				float t2 = -1;
				determinant = (float)Math.sqrt(determinant);
				if(determinant==0) {
					t1 = (-2*ry_[1])/(6*ry_[0]);
				} else {
					t1 = (-2*ry_[1]+determinant)/(6*ry_[0]);
					t2 = (-2*ry_[1]-determinant)/(6*ry_[0]);
				}
				
				if(cubic.isThetaWellDefined(t1)==false) {
					t1 = -1;
				}
				if(cubic.isThetaWellDefined(t2)==false) {
					t2 = -1;
				}
				
				//sort our list of 2 elements:
				if(t2<t1) {
					float swap = t1;
					t1 = t2;
					t2 = swap;
				}
				
				if(t1>=0 && t2<=1) {
					times = new float[] { t1, t2};
				} else if(t1>=0 && t1<=1) {
					times = new float[] { t1 };
				} else if(t2>=0 && t2<=1) {
					times = new float[] { t2 };
				}
			}
		}
			
		if(times!=null) {
			float[] x_ = cubic.getXCoeffs();
			float[] y_ = cubic.getYCoeffs();
			if(times.length==2) {
				defineTrack( 0, times[0], cubic);
				PathWriter.cubicTo(tailWriter, 0, times[0], x_[0], x_[1], x_[2], x_[3], 
						y_[0], y_[1], y_[2], y_[3]);
				flush( cubic.getX(times[0]), cubic.getY(times[0]) );
				defineTrack( times[0], times[1], cubic);
				PathWriter.cubicTo(tailWriter, times[0], times[1], x_[0], x_[1], x_[2], x_[3], 
						y_[0], y_[1], y_[2], y_[3]);
				flush( cubic.getX(times[1]), cubic.getY(times[1]) );
				defineTrack( times[1], 1, cubic);
				PathWriter.cubicTo(tailWriter, times[1], 1, x_[0], x_[1], x_[2], x_[3], 
						y_[0], y_[1], y_[2], y_[3]);
			} else {
				defineTrack( 0, times[0], cubic);
				PathWriter.cubicTo(tailWriter, 0, times[0], x_[0], x_[1], x_[2], x_[3], 
						y_[0], y_[1], y_[2], y_[3]);
				flush( cubic.getX(times[0]), cubic.getY(times[0]) );
				defineTrack( times[0], 1, cubic);
				PathWriter.cubicTo(tailWriter, times[0], 1, x_[0], x_[1], x_[2], x_[3], 
						y_[0], y_[1], y_[2], y_[3]);
			}
			return;
		}
		defineTrack( 0, 1, cubic);
		tail = tail.cubicTo(cx1, cy1, cx2, cy2, x, y);			
	}
	
	private void defineTrack(float t0,float t1,PathSegment.Float segment) {
		float[] y_ = segment.getYCoeffs(rotate);
		
		float t = (t0+t1)/2;
		boolean b;
		if(y_.length==2) {
			b = (y_[0]) > 0;
		} else if(y_.length==3) {
			b = (2*y_[0]*t+y_[1]) > 0;
		} else if(y_.length==4) {
			b = (3*y_[0]*t*t+2*y_[1]*t+y_[2]) > 0;
		} else {
			throw new RuntimeException("unexpected condition");
		}
		if(writingTrack1!=null && writingTrack1.booleanValue()!=b) {
			float x = segment.getX(t0);
			float y = segment.getY(t0);
			flush(x, y);
		}
		
		if(writingTrack1==null) {
			writingTrack1 = b ? Boolean.TRUE : Boolean.FALSE;
		}
	}
	
	@Override
	public synchronized void closePath() {
		if(tail==null) throw new NullPointerException("missing moveTo segment");
		float lastX = tail.data[tail.data.length-2];
		float lastY = tail.data[tail.data.length-1];
		if(Math.abs(lastX-moveX)>.00001 || Math.abs(lastY-moveY)>.00001)
			lineTo(moveX, moveY);
		
		flush();
	}
	
	@Override
	public void write(Shape s) {
		PathIterator iter = s.getPathIterator(null);
		write(iter);
	}
	
	private void flush(float moveX,float moveY) {
		flush();
		tail = new PathSegment.Float( moveX, moveY);
	}
	
	@Override
	public synchronized void flush() {
		try {
			/** This occurs if there hasn't been a moveTo
			 */
			if(tail==null) return;
			
			/** This occurs if a moveTo is followed by a close
			 * with no segment data.
			 */
			if(writingTrack1==null) return;
			
			PathSegment.Float head = tail.getHead();
			PathWriter dest;
			double cos = FastMath.cos(angle);
			double sin = FastMath.sin(angle);
			AffineTransform transform1, transform2;
			
			/** Switching the order we apply the transforms fixes winding
			 * problems when dest1 and dest2 are the same path.  The same
			 * basic path data is being written either way.
			 */
			if(writingTrack1.booleanValue()) {
				dest = dest1;
				transform1 = AffineTransform.getTranslateInstance( offset1*cos, offset1*sin);
				transform2 = AffineTransform.getTranslateInstance( offset2*cos, offset2*sin);
			} else {
				dest = dest2;
				transform1 = AffineTransform.getTranslateInstance( offset2*cos, offset2*sin);
				transform2 = AffineTransform.getTranslateInstance( offset1*cos, offset1*sin);
			}
			
			/** This can happen if only 1 dest path was provided to write to. */
			if(dest==null) return;
			
			PathSegment.Float t = head;
			while(t!=null) {
				t.write(dest, 0, 1, transform1);
				t = t.next;
			}
	
			t = tail;
			double[] pts = new double[t.data.length];
			transform2.transform(t.data, 0, pts, 0, t.data.length/2);
			dest.lineTo( (float)pts[pts.length-2], (float)pts[pts.length-1]);
			
			while(t!=null) {
				if(t.type==PathIterator.SEG_MOVETO) {
					if(t.prev==null) {
						dest.closePath();
					} else {
						throw new RuntimeException("Unexpected condition.");
					}
				} else {
					t.write(dest, 1, 0, transform2);
				}
				t = t.prev;
			}
			dest.flush();
		} finally {
			tail = null;
			writingTrack1 = null;
		}
	}
}
