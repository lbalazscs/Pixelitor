/*
 * @(#)CalligraphyStroke.java
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
package com.bric.awt;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import com.bric.geom.PathSegment;
import com.bric.geom.PathWriter;
import com.bric.geom.SimplifiedPathIterator;
import com.bric.geom.PathSegment.Float;

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
		this(width, (float) ((Math.PI / 4.0) * 3.0));
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
		// the path we write our final strokes shape in:
		GeneralPath dest = new GeneralPath(PathIterator.WIND_NON_ZERO);

		//isolate each subpath, and the append that stroked
		//path to dest.
		PathIterator i = p.getPathIterator(null);
		i = new SimplifiedPathIterator(i);
		Float subpath = null;
		float[] coords = new float[6];
		float lastX = 0;
		float lastY = 0;
		float moveX = 0;
		float moveY = 0;
		boolean done = i.isDone();
		boolean flush = false;
		while (done == false) {

			int k;
			if (i.isDone() == false) {
				k = i.currentSegment(coords);

				if(k==PathIterator.SEG_CLOSE &&
					(Math.abs(lastX - moveX) > .01
						|| Math.abs(lastY - moveY) > .01)) {
					subpath = subpath.lineTo(moveX, moveY);
				}

				flush = (k == PathIterator.SEG_CLOSE || k == PathIterator.SEG_MOVETO);
			} else {
				//we're at the very last iteration
				//of this loop, our only goal is to flush
				//any remaining data:
				k = PathIterator.SEG_MOVETO;
				flush = true;
				done = true;
			}

			if (flush && subpath!=null) {
				//this is where the magic happens:
				//we have a complete subpath.  Now add the stroke:
				createStrokedShape(dest, (Segment)subpath);
				subpath = null;
			}

			if (k == PathIterator.SEG_MOVETO) {
				moveX = coords[0];
				moveY = coords[1];
				lastX = coords[0];
				lastY = coords[1];
				subpath = new Segment(moveX, moveY);
			} else if (k == PathIterator.SEG_LINETO) {
				subpath = subpath.lineTo(coords[0], coords[1]);
				lastX = coords[0];
				lastY = coords[1];
			} else if (k == PathIterator.SEG_QUADTO) {
				subpath = subpath.quadTo(coords[0], coords[1],
						coords[2], coords[3]);
				lastX = coords[2];
				lastY = coords[3];
			} else if (k == PathIterator.SEG_CUBICTO) {
				subpath = subpath.cubicTo(coords[0], coords[1],
						coords[2], coords[3],
						coords[4], coords[5] );
				lastX = coords[4];
				lastY = coords[5];
			}

			if (!done)
				i.next();
		}
		return dest;
	}

	/**
	 * Given a list of segments, create the calligraphic outline for that
	 * subpath.
	 */
	private void createStrokedShape(GeneralPath dest, Segment subpath) {
		CalligraphyPathWriter writer = new CalligraphyPathWriter(dest);

		boolean track1 = true;
		Segment current = (Segment)subpath.getHead();
		while (current!=null) {
			track1 = current.write(writer, width / 2f, track1, true);
			current = (Segment)current.next;
		}

		track1 = !track1;
		current = (Segment)subpath.getTail();
		while(current!=null) {
			track1 = current.write(writer, width / 2f, track1, false);
			current = (Segment)current.prev;
		}
	}


	/**
	 * A piece of the shape we're drawing.
	 *
	 * This is not static because it needs to reference the "theta" of the
	 * enclosing CalligraphyStroke object.
	 */
	private class Segment extends PathSegment.Float {

		/**
		 * If not null, this contains the times in this curve when the angle is
		 * tangent to the pen angle. Sorted in ascending order.
		 */
		float[] tangentTimes;

		public Segment(float moveX, float moveY) {
			super(moveX, moveY);
		}

		private Segment() {}

		protected Float newSegment() {
			return new Segment();
		}

		public Float cubicTo(float cx0, float cy0, float cx1, float cy1,
				float x1, float y1) {
			Segment newSegment = (Segment)super.cubicTo(cx0, cy0, cx1, cy1, x1, y1);

			newSegment.rotate(-theta);
			newSegment.prev.rotate(-theta);

			float[] y_ = newSegment.getYCoeffs();

			float determinant = 4*y_[1]*y_[1]-12*y_[0]*y_[2];
			if(determinant>=0) {
				float t1 = -1;
				float t2 = -1;
				determinant = (float)Math.sqrt(determinant);
				if(determinant==0) {
					t1 = (-2*y_[1])/(6*y_[0]);
				} else {
					t1 = (-2*y_[1]+determinant)/(6*y_[0]);
					t2 = (-2*y_[1]-determinant)/(6*y_[0]);
				}

				if(newSegment.isThetaWellDefined(t1)==false) {
					t1 = -1;
				}
				if(newSegment.isThetaWellDefined(t2)==false) {
					t2 = -1;
				}

				if(t2<t1) {
					float swap = t1;
					t1 = t2;
					t2 = swap;
				}

				if(t1>=0 && t2<=1) {
					newSegment.tangentTimes = new float[] { t1, t2};
				} else if(t1>=0 && t1<=1) {
					newSegment.tangentTimes = new float[] { t1 };
				} else if(t2>=0 && t2<=1) {
					newSegment.tangentTimes = new float[] { t2 };
				}
			}

			newSegment.rotate(theta);
			newSegment.prev.rotate(theta);

			return newSegment;
		}

		public Float quadTo(float cx, float cy, float x, float y) {
			Segment newSegment = (Segment)super.quadTo(cx, cy, x, y);


			newSegment.rotate(-theta);
			newSegment.prev.rotate(-theta);

			float[] y_ = newSegment.getYCoeffs();
			float t = -y_[1]/(2*y_[0]);
			if(t>=0 && t<=1)
				newSegment.tangentTimes = new float[] { t };

			newSegment.rotate(theta);
			newSegment.prev.rotate(theta);

			return newSegment;
		}

		public String toString() {
			return "Segment[ path=" + getPath(null) + ", t="
					+ toString(tangentTimes) + "]";
		}

		/**
		 * Writes this data.
		 *
		 * @param writer
		 *            the destination to write to.
		 * @param distance
		 *            the offset from the original segment (used in combination
		 *            with <code>theta</code>).
		 * @param firstTrack
		 *            whether we're writing the first track or the second track.
		 *            The first track adds (dx, dy) to the original path; the
		 *            second track subtracts (dx, dy) to the original path.
		 * @param forward
		 *            whether t increases from 0 to 1, or decreases from 1 to 0.
		 * @return the track that was last written (the track flips every time a
		 *         tangent point is hit).
		 */
		public boolean write(CalligraphyPathWriter writer, float distance,
				boolean firstTrack, boolean forward) {
			if(type==PathIterator.SEG_MOVETO)
				return firstTrack;

			// if the the previous segment and this
			// segment are not continuous, then we
			// may have to immediately shift tracks:
			if (prev!=null && prev.type!=PathIterator.SEG_MOVETO && forward) {
				float lastTheta = prev.getTheta(1,-1);
				float startingTheta = getTheta(0,1);

				double lastY = Math.sin(lastTheta-theta);
				double newY = Math.sin(startingTheta-theta);

				if (lastY*newY < 0) {
					firstTrack = !firstTrack;
				}
			} else if (next!=null && forward==false) {
				float lastTheta = next.getTheta(0,1);
				float startingTheta = getTheta(1,-1);

				double lastY = Math.sin(lastTheta-theta);
				double newY = Math.sin(startingTheta-theta);

				if (lastY*newY < 0) {
					firstTrack = !firstTrack;
				}
			}

			float dx = (float) (distance * Math.cos(theta));
			float dy = (float) (distance * Math.sin(theta));
			if (!firstTrack) {
				dx = -dx;
				dy = -dy;
			}

			//the only segment type to have null data
			//is SEG_CLOSE, and we never write that
			//in this class, so that won't be an issue.

			float lastX = prev.data[prev.data.length-2];
			float lastY = prev.data[prev.data.length-1];

			if (type == PathIterator.SEG_LINETO) {
				float x0 = lastX;
				float y0 = lastY;
				float x1 = data[0];
				float y1 = data[1];
				if (forward) {
					writer.lineTo(x0 + dx, y0 + dy);
					writer.lineTo(x1 + dx, y1 + dy);
				} else {
					writer.lineTo(x1 + dx, y1 + dy);
					writer.lineTo(x0 + dx, y0 + dy);
				}
			} else if (type==PathIterator.SEG_QUADTO) {
				if (tangentTimes == null) {
					// simplest possible case: no tangent points to deal with
					if (forward) {
						writer.lineTo(lastX + dx, lastY + dy);
						writer.quadTo(data[0] + dx, data[1] + dy, data[2] + dx,
								data[3] + dy);
					} else {
						writer.lineTo(data[2] + dx, data[3] + dy);
						writer.quadTo(data[0] + dx, data[1] + dy, lastX + dx,
								lastY + dy);
					}
				} else {
					// we have 1 tangent point, so we're going to
					// have to swap tracks exactly once:
					float[] x_ = getXCoeffs();
					float[] y_ = getYCoeffs();

					float startTime, endTime;
					if (forward) {
						startTime = 0;
						endTime = 1;
					} else {
						startTime = 1;
						endTime = 0;
					}
					writer.lineTo(((x_[0] * startTime + x_[1]) * startTime + x_[2])
							+ dx, ((y_[0] * startTime + y_[1]) * startTime + y_[2])
							+ dy);
					float t = tangentTimes[0];
					PathWriter.quadTo(writer, startTime, t, x_[0], x_[1], x_[2] + dx,
							y_[0], y_[1], y_[2] + dy);
					firstTrack = !firstTrack;
					dx = -dx;
					dy = -dy;
					writer.lineTo(((x_[0] * t + x_[1]) * t + x_[2]) * t + dx,
							((y_[0] * t + y_[1]) * t + y_[2]) * t + dy);
					PathWriter.quadTo(writer, t, endTime,
							x_[0], x_[1], x_[2] + dx,
							y_[0], y_[1], y_[2] + dy);
				}
			} else if (type==PathIterator.SEG_CUBICTO) {
				if (tangentTimes == null) {
					// simplest possible case: no tangent points to deal with
					if (forward) {
						writer.lineTo(lastX + dx, lastY + dy);
						writer.curveTo(data[0] + dx, data[1] + dy,
								data[2] + dx, data[3] + dy,
								data[4] + dx, data[5] + dy);
					} else {
						writer.lineTo(data[4] + dx, data[5] + dy);
						writer.curveTo(data[2] + dx, data[3] + dy,
								data[0] + dx, data[1] + dy,
								lastX + dx, lastY + dy);
					}
				} else {
					// the most complicated scenario:
					// we have one or two tangent points, so we'll
					// have to switch tracks once or twice.
					float[] x_ = getXCoeffs();
					float[] y_ = getYCoeffs();

					float startTime = forward ? 0 : 1;
					float endTime = forward ? 1 : 0;

					writer.lineTo(((x_[0] * startTime + x_[1]) * startTime + x_[2])
							* startTime + x_[3] + dx,
							((y_[0] * startTime + y_[1])
							* startTime + y_[2])
							* startTime + y_[3] + dy);

					float prevTime = startTime;
					int a = forward ? 0 : tangentTimes.length - 1;
					boolean done = false;
					while (done == false) {
						float t = tangentTimes[a];
						PathWriter.cubicTo(writer, prevTime, t,
								x_[0], x_[1], x_[2], x_[3] + dx,
								y_[0], y_[1], y_[2], y_[3] + dy);
						firstTrack = !firstTrack;
						dx = -dx;
						dy = -dy;
						writer.lineTo(((x_[0] * t + x_[1]) * t + x_[2]) * t + x_[3]
								+ dx, ((y_[0] * t + y_[1]) * t + y_[2]) * t + y_[3]
								+ dy);
						prevTime = t;
						if (forward) {
							a++;
						} else {
							a--;
						}
						done = (a < 0) || (a >= tangentTimes.length);
					}
					PathWriter.cubicTo(writer, prevTime, endTime,
							x_[0], x_[1], x_[2], x_[3] + dx,
							y_[0], y_[1], y_[2], y_[3] + dy);
				}
			}
			return firstTrack;
		}
	}


	/**
	 * This is a special PathWriter for the CalligraphyStroke class.
	 *
	 * <P>
	 * The heart of the calligraphy-writing code does not refer to moveTo()'s.
	 * Instead overly redundant calls to lineTo() are used. This write will
	 * automatically insert a moveTo() if necessarily, and will not repeat
	 * redundant lineTo() instructions.
	 *
	 */
	private static class CalligraphyPathWriter extends PathWriter {
		GeneralPath path;
		boolean moved = false;
		float lastX = 0;
		float lastY = 0;

		public CalligraphyPathWriter(GeneralPath p) {
			path = p;
		}

		public void closePath() {
			path.closePath();
			// this shouldn't be necessary, but just in case:
			moved = false;

		}

		public void curveTo(float cx1, float cy1, float cx2, float cy2,
				float x, float y) {
			if (!moved) {
				// we could simply call moveTo(x,y),
				// but really if we haven't received an initial
				// lineTo(), then something is wrong. There should
				// always be a lineTo() preceding a call to
				// curveTo()....
				throw new RuntimeException();
			} else {
				path.curveTo(cx1, cy1, cx2, cy2, x, y);
				lastX = x;
				lastY = y;
			}
		}

		public void lineTo(float x, float y) {
			if (!moved) {
				moveTo(x, y);
			} else {
				if (Math.abs(x - lastX) < .01 && Math.abs(y - lastY) < .01) {
					return;
				} else {
					path.lineTo(x, y);
				}
				lastX = x;
				lastY = y;
			}
		}

		public void quadTo(float cx, float cy, float x, float y) {
			if (!moved) {
				// we could simply call moveTo(x,y),
				// but really if we haven't received an initial
				// lineTo(), then something is wrong. There should
				// always be a lineTo() preceding a call to
				// quadTo()....
			} else {
				path.quadTo(cx, cy, x, y);
				lastX = x;
				lastY = y;
			}
		}

		public void flush() {
			// does nothing
		}

		public void moveTo(float x, float y) {
			moved = true;
			path.moveTo(x, y);
		}
	}
}
