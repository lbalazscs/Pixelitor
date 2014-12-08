/*
 * @(#)Transition3D.java
 *
 * $Date: 2014-05-04 18:08:30 +0200 (V, 04 máj. 2014) $
 *
 * Copyright (c) 2014 by Jeremy Wood.
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
package com.bric.image.transition;

import java.awt.geom.Point2D;

import com.bric.math.function.PolynomialFunction;

/** This provides a few static tools to help make 3D transitions.
 */
public abstract class Transition3D extends AbstractTransition {

	
	public abstract static class Point3D {
		public static class Double extends Point3D {
			double x, y, z;
			
			public Double(double x, double y, double z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}

			public double getX() {
				return x;
			}
			public double getY() {
				return y;
			}
			public double getZ() {
				return z;
			}

			public void setLocation(double x, double y, double z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}
			
		}
		
		public abstract double getX();
		public abstract double getY();
		public abstract double getZ();
		public abstract void setLocation(double x, double y, double z);
		
		@Override
		public String toString() {
			return "("+getX()+", "+getY()+", "+getZ()+")";
		}
	}
	
	public static class BasicProjection {
		double w, h;
		PolynomialFunction pf = PolynomialFunction.createFit( new double[] {-500, 1.5}, new double[] {0, 1} );
		
		public BasicProjection(double width,double height) {
			w = width;
			h = height;
		}
		
		public Point2D transform(Point3D p) {
			return transform(p.getX(), p.getY(), p.getZ());
		}
		
		public Point2D transform(double x,double y,double z) {
			//based on:
			// http://stackoverflow.com/questions/519106/projecting-a-3d-point-to-a-2d-screen-coordinate
			
			x = x-w/2.0;
			y = y-h/2.0;
			
			   double screenX = 0d, screenY = 0d;

			    // Camera is defined in XAML as:
			    //        <Viewport3D.Camera>
			    //             <PerspectiveCamera Position="0,0,800" LookDirection="0,0,-1" />
			    //        </Viewport3D.Camera>


			    // Translate input point using camera position
			    double inputX = x - 0; //cam.Position.X;
			    double inputY = y - 0; //cam.Position.Y;
			    double inputZ = z - w; //cam.Position.Z;

			    double aspectRatio = w / h;

			    // Apply projection to X and Y
			    screenX = inputX / (-inputZ * Math.tan(Math.PI * 1 / 4));

			    screenY = (inputY * aspectRatio) / (-inputZ * Math.tan(Math.PI * 1 / 4));

			    // Convert to screen coordinates
			    screenX = screenX * w;

			    screenY = screenY * h;

			    return new Point2D.Double(screenX + w/2.0, screenY + h/2.0);
		}
	}

	/** Flush all z-coordinates with zero.
	 */
	protected static void flushZCoordinateWithSurface(Point3D... points) {
		double maxZ = 0;
		for(Point3D p : points) {
			maxZ = Math.max(maxZ, p.getZ());
		}
		for(Point3D p : points) {
			p.setLocation(p.getX(), p.getY(), p.getZ() - maxZ);
		}
	}
}
