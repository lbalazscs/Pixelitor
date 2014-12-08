/*
 * @(#)FlipTransition3D.java
 *
 * $Date: 2014-06-06 20:04:49 +0200 (P, 06 jún. 2014) $
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import com.bric.image.ImageContext;
import com.bric.util.ResourcePool;

/** This transition flips the viewing surface over 180 degrees to reveal the inverted back.
 * Here are playback samples:
 * <p><table summary="Sample Animations of FlipTransition3D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipLeft.gif" alt="Flip Left">
 * <p>Flip Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipRight.gif" alt="Flip Right">
 * <p>Flip Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipUp.gif" alt="Flip Up">
 * <p>Flip Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipDown.gif" alt="Flip Down">
 * <p>Flip Down
 * </td>
 * </tr><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipLeftFlush.gif" alt="Flip Left Flush">
 * <p>Flip Left Flush
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipRightFlush.gif" alt="Flip Right Flush">
 * <p>Flip Right Flush
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipUpFlush.gif" alt="Flip Up Flush">
 * <p>Flip Up Flush
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/FlipTransition3D/FlipDownFlush.gif" alt="Flip Down Flush">
 * <p>Flip Down Flush
 * </td>
 * </tr></table>
 */
public class FlipTransition3D extends Transition3D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new FlipTransition3D(UP, false), 
				new FlipTransition3D(DOWN, false),
				new FlipTransition3D(LEFT, false), 
				new FlipTransition3D(RIGHT, false),
				new FlipTransition3D(UP, true), 
				new FlipTransition3D(DOWN, true),
				new FlipTransition3D(LEFT, true), 
				new FlipTransition3D(RIGHT, true)
		};
	}

	int direction;
	boolean flush;
	Color background;

	/** Create a FlipTransition3D with a black background.
	 * 
	 * @param direction one of the Transition constants: UP, DOWN, LEFT or RIGHT
	 * @param flush whether the z-axis should remain flush with the target destination.
	 * For example: if this is false then as a surface flips to the right a vertical edge
	 * will be pulled towards the camera and grow larger in height than than the target
	 * destination. So the center of the flip is constant. But if this is true: then
	 * the center of the flip pulls farther away from the camera to make sure that vertical
	 * edge is never larger than the height of the target graphics area.
	 */
	public FlipTransition3D(int direction,boolean flush) {
		this(direction, flush, Color.black);
	}

	/** Create a FlipTransition3D.
	 * 
	 * @param direction one of the Transition constants: UP, DOWN, LEFT or RIGHT
	 * @param flush whether the z-axis should remain flush with the target destination.
	 * For example: if this is false then as a surface flips to the right a vertical edge
	 * will be pulled towards the camera and grow larger in height than than the target
	 * destination. So the center of the flip is constant. But if this is true: then
	 * the center of the flip pulls farther away from the camera to make sure that vertical
	 * edge is never larger than the height of the target graphics area.
	 * @param background the optional background color to paint behind this transition.
	 */
	public FlipTransition3D(int direction,boolean flush,Color background) {
		if(!(direction==Transition.UP ||
				direction==Transition.DOWN ||
				direction==Transition.LEFT ||
				direction==Transition.RIGHT )) {
			throw new IllegalArgumentException("direction must be UP, DOWN, LEFT or RIGHT");
		}
		this.flush = flush;
		this.direction = direction;
		this.background = background;
	}
	
	@Override
	protected void doPaint(Graphics2D g, BufferedImage frameA,
			BufferedImage frameB, float progress) {
		int h = frameA.getHeight();
		int w = frameB.getWidth();
		if(background!=null) {
			g.setColor(background);
			g.fillRect(0,0,w,h);
		}
		
		boolean vert;
		Point2D topLeft, topRight, bottomLeft, bottomRight;
		Point3D topLeft3D, topRight3D, bottomLeft3D, bottomRight3D;
		BasicProjection p = new BasicProjection( w, h );
		if(direction==UP) {
			topLeft3D = new Point3D.Double(0, h/2 - h/2*Math.cos(Math.PI*progress), - h/2*Math.sin(Math.PI*progress));
			topRight3D = new Point3D.Double(w, h/2 - h/2*Math.cos(Math.PI*progress), - h/2*Math.sin(Math.PI*progress));
			bottomLeft3D = new Point3D.Double(0, h/2 + h/2*Math.cos(Math.PI*progress), h/2*Math.sin(Math.PI*progress));
			bottomRight3D = new Point3D.Double(w, h/2 + h/2*Math.cos(Math.PI*progress), h/2*Math.sin(Math.PI*progress));
			vert = true;
		} else if(direction==DOWN) {
			topLeft3D = new Point3D.Double(0, h/2 - h/2*Math.cos(Math.PI*progress), h/2*Math.sin(Math.PI*progress));
			topRight3D = new Point3D.Double(w, h/2 - h/2*Math.cos(Math.PI*progress), h/2*Math.sin(Math.PI*progress));
			bottomLeft3D = new Point3D.Double(0, h/2 + h/2*Math.cos(Math.PI*progress), - h/2*Math.sin(Math.PI*progress));
			bottomRight3D = new Point3D.Double(w, h/2 + h/2*Math.cos(Math.PI*progress), - h/2*Math.sin(Math.PI*progress));
			vert = true;
		} else if(direction==LEFT) {
			topLeft3D = new Point3D.Double(w/2 - w/2*Math.cos(Math.PI*progress), 0, - w/2*Math.sin(Math.PI*progress));
			topRight3D = new Point3D.Double(w/2 + w/2*Math.cos(Math.PI*progress),0, w/2*Math.sin(Math.PI*progress));
			bottomLeft3D = new Point3D.Double(w/2 - w/2*Math.cos(Math.PI*progress), h, - w/2*Math.sin(Math.PI*progress));
			bottomRight3D = new Point3D.Double(w/2 + w/2*Math.cos(Math.PI*progress), h, w/2*Math.sin(Math.PI*progress));
			vert = false;
		} else {
			topLeft3D = new Point3D.Double(w/2 - w/2*Math.cos(Math.PI*progress), 0, w/2*Math.sin(Math.PI*progress));
			topRight3D = new Point3D.Double(w/2 + w/2*Math.cos(Math.PI*progress),0, - w/2*Math.sin(Math.PI*progress));
			bottomLeft3D = new Point3D.Double(w/2 - w/2*Math.cos(Math.PI*progress), h, w/2*Math.sin(Math.PI*progress));
			bottomRight3D = new Point3D.Double(w/2 + w/2*Math.cos(Math.PI*progress), h, - w/2*Math.sin(Math.PI*progress));
			vert = false;
		}
		if(flush)
			flushZCoordinateWithSurface(topLeft3D, topRight3D, bottomLeft3D, bottomRight3D);
		topLeft = p.transform(topLeft3D);
		topRight = p.transform(topRight3D);
		bottomLeft = p.transform(bottomLeft3D);
		bottomRight = p.transform(bottomRight3D);

		BufferedImage scratchImage = ResourcePool.get().getImage(w, h, BufferedImage.TYPE_INT_ARGB, true);
		try {
			ImageContext context = ImageContext.create(scratchImage);
			context.setRenderingHints(g.getRenderingHints());
	
			if(progress>.5) {
				if(vert) {
					context.drawImage(frameB,
							bottomLeft,
							bottomRight,
							topRight,
							topLeft);
				} else {
					context.drawImage(frameB,
							topRight,
							topLeft,
							bottomLeft,
							bottomRight);
				}
			} else {
				context.drawImage(frameA,
						topLeft,
						topRight,
						bottomRight,
						bottomLeft);
			}
			context.dispose();
	
			Graphics2D g3 = scratchImage.createGraphics();
			g3.setComposite(AlphaComposite.SrcAtop);
			double maxDarkness = 150;
			double z = vert ? Math.abs(bottomLeft.getY() - topLeft.getY())/h : Math.abs(bottomLeft.getX() - bottomRight.getX())/w;
			int alpha = (int)( maxDarkness*(1-z) );
			alpha = Math.min(Math.max(0,alpha),255);
			if(vert) {
				if(direction==UP) {
					g3.setPaint(new GradientPaint(
							0, (float)topLeft.getY(), new Color(0,0,0,alpha),
							0, (float)bottomLeft.getY(), new Color(0,0,0,0)
							));
				} else {
					g3.setPaint(new GradientPaint(
							0, (float)bottomLeft.getY(), new Color(0,0,0,alpha),
							0, (float)topLeft.getY(), new Color(0,0,0,0)
							));
				}
			} else {
				if(direction==LEFT) {
					g3.setPaint(new GradientPaint(
							(float)topLeft.getX(), 0, new Color(0,0,0,alpha),
							(float)topRight.getX(), 0, new Color(0,0,0,0)
							));
				} else {
					g3.setPaint(new GradientPaint(
							(float)topRight.getX(), 0, new Color(0,0,0,alpha),
							(float)topLeft.getX(), 0, new Color(0,0,0,0)
							));
				}
			}
			Rectangle2D r = CubeTransition3D.getBounds(topLeft, topRight, bottomLeft, bottomRight);
			r.setFrame(r.getX()-1, r.getY()-1, r.getWidth()+2, r.getHeight()+2);
			g3.fill( r );
			g3.dispose();
			
			
			g.drawImage(scratchImage,0,0,null);
		} finally {
			ResourcePool.get().put(scratchImage);
;		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("Flip ");
		if(direction==Transition.UP) {
			sb.append("Up");
		} else if(direction==Transition.DOWN) {
			sb.append("Down");
		} else if(direction==Transition.LEFT) {
			sb.append("Left");
		} else {
			sb.append("Right");
		}
		if(flush) {
			sb.append(" Flush");
		}
		return sb.toString();
	}

}
