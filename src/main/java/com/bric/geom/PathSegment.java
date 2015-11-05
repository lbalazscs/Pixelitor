/*
 * @(#)PathSegment.java
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

import net.jafama.FastMath;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

public abstract class PathSegment {
	
	public int type;
	
	/** This maps the type integer to a human-readable string.
	 * This is intended for debugging and exceptions.
	 * 
	 * @param type one of the PathIterator "SEG" constants.
	 * @return a string such as "SEG_CLOSE" or "SEG_LINETO".
	 */
	public static final String toTypeName(int type) {
		if(type==PathIterator.SEG_CLOSE)
			return "SEG_CLOSE";
		if(type==PathIterator.SEG_CUBICTO)
			return "SEG_CUBICTO";
		if(type==PathIterator.SEG_LINETO)
			return "SEG_LINETO";
		if(type==PathIterator.SEG_MOVETO)
			return "SEG_MOVETO";
		if(type==PathIterator.SEG_QUADTO)
			return "SEG_QUADTO";
		return "UNKNOWN";
	}
	
	public abstract void rotate(float f);
	
	public static class Float extends PathSegment {
		public float data[];
		public PathSegment.Float next;
		public PathSegment.Float prev;
		private float[] xCoeffs;
		private float[] yCoeffs;
		
		/** A very small value that is considered "equivalent to zero"
		 * in some operations where machine error may occur.
		 */
		protected static final float ZERO = .01f;
		
		/** Initializes a SEG_MOVETO segment.
		 * 
		 * This is the only publicly available constructor: to
		 * add other segments you should use the methods lineTo,
		 * cubicTo, etc.
		 * 
		 * @param moveX
		 * @param moveY
		 */
		public Float(float moveX,float moveY) {
			type = PathIterator.SEG_MOVETO;
			data = new float[] { moveX, moveY };
		}
		
		/** This constructor does not initialize anything.
		 */
		protected Float() {}
		
		/** Subclasses must override this method to return
		 * a segment of the correct type.
		 */
		protected Float newSegment() {
			return new Float();
		}
		
		public float[] getXCoeffs() {
			return getXCoeffs(null);
		}
		
		public float[] getXCoeffs(AffineTransform transform) {
			if(prev==null) {
				System.err.println(this);
				throw new NullPointerException("prev was null");
			}
			if(prev.data==null) {
				System.err.println(this);
				throw new NullPointerException("prev.data was null");
			}
			if(transform!=null && transform.isIdentity())
				transform = null;
			if(xCoeffs!=null && transform==null)
				return xCoeffs;
			
			double[] last = new double[] {
					prev.data[prev.data.length-2],
					prev.data[prev.data.length-1] };
			if(transform!=null) {
				transform.transform(last, 0, last, 0, 1);
			}
			float[] myData;
			if(transform!=null) {
				myData = new float[data.length];
				transform.transform(data,0,myData,0,data.length/2);
			} else {
				myData = data;
			}
			float[] rValue = null;
			if(type==PathIterator.SEG_CUBICTO) {
				rValue = new float[4];
				rValue[0] = (float)(-last[0] + 3 * myData[0] - 3 * myData[2] + myData[4]);
				rValue[1] = (float)(3 * last[0] - 6 * myData[0] + 3 * myData[2]);
				rValue[2] = (float)(-3 * last[0] + 3 * myData[0]);
				rValue[3] = (float)(last[0]);
			} else if(type==PathIterator.SEG_QUADTO) {
				rValue = new float[3];
				rValue[0] = (float)(last[0] - 2 * myData[0] + myData[2]);
				rValue[1] = (float)(-2 * last[0] + 2 * myData[0]);
				rValue[2] = (float)(last[0]);
			} else if(type==PathIterator.SEG_LINETO) {
				rValue = new float[2];
				rValue[0] = (float)(-last[0]+myData[0]);
				rValue[1] = (float)(last[0]);
			} else if(type==PathIterator.SEG_MOVETO) {
				throw new UnsupportedOperationException("MOVETO segments cannot be broken down into parametric equations.");
			} else if(type==PathIterator.SEG_CLOSE) {
				throw new UnsupportedOperationException("CLOSE segments cannot be broken down into parametric equations.");
			}
			if(transform==null)
				xCoeffs = rValue;
			return rValue;
		}
		
		public float[] getYCoeffs() {
			return getYCoeffs(null);
		}
		
		public float[] getYCoeffs(AffineTransform transform) {
			if(prev==null)
				throw new NullPointerException("prev was null");
			if(prev.data==null)
				throw new NullPointerException("prev.data was null");
			if(transform!=null && transform.isIdentity())
				transform = null;
			if(yCoeffs!=null && transform==null)
				return yCoeffs;
			
			double[] last = new double[] {
					prev.data[prev.data.length-2],
					prev.data[prev.data.length-1] };
			if(transform!=null) {
				transform.transform(last, 0, last, 0, 1);
			}
			float[] myData;
			if(transform!=null) {
				myData = new float[data.length];
				transform.transform(data,0,myData,0,data.length/2);
			} else {
				myData = data;
			}
			float[] rValue = null;
			if(type==PathIterator.SEG_CUBICTO) {
				rValue = new float[4];
				rValue[0] = (float)(-last[1] + 3 * myData[1] - 3 * myData[3] + myData[5]);
				rValue[1] = (float)(3 * last[1] - 6 * myData[1] + 3 * myData[3]);
				rValue[2] = (float)(-3 * last[1] + 3 * myData[1]);
				rValue[3] = (float)(last[1]);
			} else if(type==PathIterator.SEG_QUADTO) {
				rValue = new float[3];
				rValue[0] = (float)(last[1] - 2 * myData[1] + myData[3]);
				rValue[1] = (float)(-2 * last[1] + 2 * myData[1]);
				rValue[2] = (float)(last[1]);
			} else if(type==PathIterator.SEG_LINETO) {
				rValue = new float[2];
				rValue[0] = (float)(-last[1]+myData[1]);
				rValue[1] = (float)(last[1]);
			} else if(type==PathIterator.SEG_MOVETO) {
				throw new UnsupportedOperationException("MOVETO segments cannot be broken down into parametric equations.");
			} else if(type==PathIterator.SEG_CLOSE) {
				throw new UnsupportedOperationException("CLOSE segments cannot be broken down into parametric equations.");
			}
			if(transform==null)
				yCoeffs = rValue;
			return rValue;
		}
		
		/** Returns the tangent angle at the point t.
		 * 
		 * @param t the t-value to get the tangent angle for.
		 * @param confine this may confine the result to [-pi/2,pi/2].
		 * If this is false, then the result may range from [-pi,pi].
		 * @return the tangent angle (in radians).
		 */
		public float getTheta(float t,AffineTransform transform,boolean confine) {
			float angle = getTheta(t,transform,0);
			if(confine) {
				if(angle>Math.PI/2) {
					angle = angle-(float)Math.PI;
				} else if(angle<-Math.PI/2) {
					angle = angle+(float)Math.PI;
				}
			}
			return angle;
		}
		
		public boolean isThetaWellDefined(float t) {
			float[] x_coeffs = getXCoeffs();
			float[] y_coeffs = getYCoeffs();
			float dx, dy;
			if(x_coeffs.length==2) {
				dx = x_coeffs[0];
				dy = y_coeffs[0];
			} else if(x_coeffs.length==3) {
				dx = 2*x_coeffs[0]*t+x_coeffs[1];
				dy = 2*y_coeffs[0]*t+y_coeffs[1];
			} else if(x_coeffs.length==4) {
				dx = 3*x_coeffs[0]*t*t+2*x_coeffs[1]*t+x_coeffs[2];
				dy = 3*y_coeffs[0]*t*t+2*y_coeffs[1]*t+y_coeffs[2];
			} else {
				System.err.println("x_coeffs.length = "+x_coeffs.length);
				System.err.println(this);
				throw new RuntimeException("Unexpected condition.");
			}
			if(Math.abs(dx)<ZERO && Math.abs(dy)<ZERO)
				return false;
			return true;
		}
		
		/** Returns the tangent angle at the point t.
		 * 
		 * @param t the t-value to get the tangent angle for.
		 * @param direction in the (extremely) rare case where the
		 * tangent slope is ambiguous, this indicates which side
		 * of the t argument to look at.
		 * That is, if you approach t from a smaller value,
		 * the angle might be K.  If you approach t from a larger
		 * value, the angle might be (K+pi).  This will happen
		 * when the direction of the curve is undefined at t, because
		 * the curve is switching from K to (K+pi).
		 * When this argument is positive, it assumes t is being
		 * approached by values greater than t.  When negative, it
		 * assumes t is approached from values less than t.
		 * @return the tangent angle (in radians).
		 */
		public float getTheta(float t,AffineTransform transform, int direction) {
			float[] x_coeffs = getXCoeffs(transform);
			float[] y_coeffs = getYCoeffs(transform);
			float dx, dy;
			if(x_coeffs.length==2) {
				dx = x_coeffs[0];
				dy = y_coeffs[0];
			} else if(x_coeffs.length==3) {
				dx = 2*x_coeffs[0]*t+x_coeffs[1];
				dy = 2*y_coeffs[0]*t+y_coeffs[1];
			} else if(x_coeffs.length==4) {
				dx = 3*x_coeffs[0]*t*t+2*x_coeffs[1]*t+x_coeffs[2];
				dy = 3*y_coeffs[0]*t*t+2*y_coeffs[1]*t+y_coeffs[2];
			} else {
				System.err.println("x_coeffs.length = "+x_coeffs.length);
				System.err.println(this);
				throw new RuntimeException("Unexpected condition.");
			}
			
			if(!(Math.abs(dx)<ZERO && Math.abs(dy)<ZERO)) {
				float angle = (float)Math.atan2(dy,dx);
				return angle;
			}
			
			//If both dx and dy approach zero, a friend who's much smarter
			//than I am pointed out L'Hopital's rule says you can then
			//take the next derivative
			if(x_coeffs.length==3) {
				dx = 2*x_coeffs[0];
				dy = 2*y_coeffs[0];
			} else if(x_coeffs.length==4) {
				dx = 6*x_coeffs[0]*t+2*x_coeffs[1];
				dy = 6*y_coeffs[0]*t+2*y_coeffs[1];
			}
			
			float angle1 = (float)Math.atan2(dy,dx);
			float angle2;
			if(angle1>0) {
				angle2 = angle1-(float)Math.PI;
			} else {
				angle2 = angle1+(float)Math.PI;
			}
			
			if(direction==0)
				return angle1;
			if(x_coeffs.length==2)
				return angle1; //you could throw an exception here, too?
			
			dx = 0;
			dy = 0;
			if(direction>1) direction = 1;
			if(direction<1) direction = -1;
			float incr = .000001f*direction;
			
			while(Math.abs(dx)<ZERO && Math.abs(dy)<ZERO) {
				if(x_coeffs.length==3) {
					dx = 2*x_coeffs[0]*t+x_coeffs[1];
					dy = 2*y_coeffs[0]*t+y_coeffs[1];
				} else if(x_coeffs.length==4) {
					dx = 3*x_coeffs[0]*t*t+2*x_coeffs[1]*t+x_coeffs[2];
					dy = 3*y_coeffs[0]*t*t+2*y_coeffs[1]*t+y_coeffs[2];
				}
				t = t+incr;
			}
			
			float otherAngle = (float)Math.atan2(dy,dx);
			if(difference(otherAngle,angle1)<difference(otherAngle,angle2)) {
				return angle1;
			}
			return angle2;
		}
		
		private float difference(float angle1,float angle2) {
			float diff = Math.abs(angle1-angle2);
			if(diff>Math.PI) {
				diff = (float)(2*Math.PI-diff);
			}
			return diff;
		}
		
		public float getX(float t) {
			float[] x_coeffs = getXCoeffs();
			if(x_coeffs.length==2) {
				return x_coeffs[0]*t+x_coeffs[1];
			} else if(x_coeffs.length==3) {
				return x_coeffs[0]*t*t+x_coeffs[1]*t+x_coeffs[2];
			} else if(x_coeffs.length==4) {
				return x_coeffs[0]*t*t*t+x_coeffs[1]*t*t+x_coeffs[2]*t+x_coeffs[3];
			} else {
				System.err.println("x_coeffs.length = "+x_coeffs.length);
				System.err.println(this);
				throw new RuntimeException("Unexpected condition.");
			}
		}
		
		public float getY(float t) {
			float[] y_coeffs = getYCoeffs();
			if(y_coeffs.length==2) {
				return y_coeffs[0]*t+y_coeffs[1];
			} else if(y_coeffs.length==3) {
				return y_coeffs[0]*t*t+y_coeffs[1]*t+y_coeffs[2];
			} else if(y_coeffs.length==4) {
				return y_coeffs[0]*t*t*t+y_coeffs[1]*t*t+y_coeffs[2]*t+y_coeffs[3];
			} else {
				System.err.println("y_coeffs.length = "+y_coeffs.length);
				System.err.println(this);
				throw new RuntimeException("Unexpected condition.");
			}
		}
		
		@Override
		public String toString() {
			return toString((Float)null);
		}
		
		public String toString(Float end) {
			return "PathSegment.Float[ "+getPath(end)+ " ]";
		}
		
		protected String getPath(Float end) {
			StringBuffer sb = new StringBuffer();
			Float f = this;
			while(f!=end && f!=null) {
				if(f.type==PathIterator.SEG_MOVETO) {
					sb.append("m "+f.data[0]+" "+f.data[1]+" ");
				} else if(f.type==PathIterator.SEG_LINETO) {
						sb.append("l "+f.data[0]+" "+f.data[1]+" ");
				} else if(f.type==PathIterator.SEG_QUADTO) {
					sb.append("q "+f.data[0]+" "+f.data[1]+" "+f.data[2]+" "+f.data[3]+" ");
				} else if(f.type==PathIterator.SEG_CUBICTO) {
					sb.append("c "+f.data[0]+" "+f.data[1]+" "+f.data[2]+" "+f.data[3]+" "+f.data[4]+" "+f.data[5]+" ");
				} else if(f.type==PathIterator.SEG_CLOSE) {
					sb.append("z ");
				} else {
					throw new RuntimeException("Unexpected type: "+type);
				}
				f = f.next;
			}
			return sb.toString().trim();
		}
		
		public Float moveTo(float x,float y) {
			Float newSegment = newSegment();
			newSegment.type = PathIterator.SEG_MOVETO;
			newSegment.data = new float[] {x, y};
			append(newSegment);
			return newSegment;
		}
		
		public Float lineTo(float x,float y) {
			Float newSegment = newSegment();
			newSegment.type = PathIterator.SEG_LINETO;
			newSegment.data = new float[] {x, y};
			append(newSegment);
			return newSegment;
		}
		
		public Float quadTo(float cx,float cy,float x, float y) {
			Float newSegment = newSegment();
			newSegment.type = PathIterator.SEG_QUADTO;
			newSegment.data = new float[] {cx, cy, x, y};
			append(newSegment);
			return newSegment;
		}
		
		public Float cubicTo(float cx0,float cy0,float cx1,float cy1,float x1,float y1) {
			Float newSegment = newSegment();
			newSegment.type = PathIterator.SEG_CUBICTO;
			newSegment.data = new float[] {cx0, cy0, cx1, cy1, x1, y1};
			append(newSegment);
			return newSegment;
		}
		
		public Float close() {
			Float newSegment = newSegment();
			newSegment.type = PathIterator.SEG_CLOSE;
			append(newSegment);
			return newSegment;
		}
		
		protected void append(Float seg) {
			if(next!=null)
				throw new RuntimeException("Illegal attempt to append shape data to a segment that already has a next segment.");
			if(seg.prev!=null)
				throw new RuntimeException("Illegal attempt to append shape data that already exists in another sequence.");
			next = seg;
			seg.prev = this;
		}
		
		public Float getHead() {
			Float t = this;
			while(t.prev!=null) {
				t = t.prev;
			}
			return t;
		}
		
		public Float getTail() {
			Float t = this;
			while(t.next!=null) {
				t = t.next;
			}
			return t;
		}
		
		@Override
		public void rotate(float f) {
			if(f==0) return;

			float cos = (float) FastMath.cos(f);
			float sin = (float) FastMath.sin(f);
			for(int a = 0; a<data.length; a+=2) {
				float x = data[a];
				float y = data[a+1];
				data[a] = cos*x-sin*y;
				data[a+1] = sin*x+cos*y;
			}
			xCoeffs = null;
			yCoeffs = null;
		}
		
		public void write(PathWriter dest,float t0,float t1,AffineTransform transform) {
			if(type==PathIterator.SEG_LINETO) {
				double[] pt = new double[] {
						getX(t1), getY(t1)
				};
				if(transform!=null) {
					transform.transform(pt, 0, pt, 0, 1);
				}
				dest.lineTo((float)pt[0],(float)pt[1]);
			} else if(type==PathIterator.SEG_QUADTO) {
				float[] x_ = getXCoeffs(transform);
				float[] y_ = getYCoeffs(transform);
				PathWriter.quadTo(dest, t0, t1, 
						x_[0], x_[1], x_[2], 
						y_[0], y_[1], y_[2]);
			} else if(type==PathIterator.SEG_CUBICTO) {
				float[] x_ = getXCoeffs(transform);
				float[] y_ = getYCoeffs(transform);
				PathWriter.cubicTo(dest, t0, t1, 
						x_[0], x_[1], x_[2], x_[3], 
						y_[0], y_[1], y_[2], y_[3]);
			} else if(type==PathIterator.SEG_MOVETO) {
				double[] pt = new double[] {
						data[0], data[1]
				};
				if(transform!=null) {
					transform.transform(pt, 0, pt, 0, 1);
				}
				dest.moveTo((float)pt[0],(float)pt[1]);
			} else {
				throw new UnsupportedOperationException(toTypeName(type)+" not supported here.");
			}
		}
	}

	public static String toString(float[] array) {
		if (array == null)
			return null;
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int a = 0; a < array.length; a++) {
			if (a != 0) {
				sb.append(", ");
			}
			sb.append(java.lang.Float.toString(array[a]));
		}
		sb.append("]");
		return sb.toString();
	}
}
