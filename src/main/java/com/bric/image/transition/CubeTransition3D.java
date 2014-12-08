/*
 * @(#)CubeTransition3D.java
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
import java.awt.image.BufferedImage;

import com.bric.image.ImageContext;
import com.bric.util.ResourcePool;

/** This transition rotates a cube 90 degrees to reveal the next image. Here are playback samples:
 * <p><table summary="Sample Animations of CubeTransition3D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeLeft.gif" alt="Cube Left">
 * <p>Cube Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeRight.gif" alt="Cube Right">
 * <p>Cube Right
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeUp.gif" alt="Cube Up">
 * <p>Cube Up
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeDown.gif" alt="Cube Down">
 * <p>Cube Down
 * </td>
 * </tr><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeLeftFlush.gif" alt="Cube Left Flush">
 * <p>Cube Left Flush
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeRightFlush.gif" alt="Cube Right Flush">
 * <p>Cube Right Flush
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeUpFlush.gif" alt="Cube UP Flush">
 * <p>Cube Up Flush
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/CubeTransition3D/CubeDownFlush.gif" alt="Cube Down Flush">
 * <p>Cube Down Flush
 * </td>
 * </tr></table>
 */
public class CubeTransition3D extends Transition3D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this 
	 * @return the transitions that should be used to demonstrate this
	 * 
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new CubeTransition3D(UP, false), 
				new CubeTransition3D(DOWN, false),
				new CubeTransition3D(LEFT, false), 
				new CubeTransition3D(RIGHT, false),
				new CubeTransition3D(UP, true), 
				new CubeTransition3D(DOWN, true),
				new CubeTransition3D(LEFT, true), 
				new CubeTransition3D(RIGHT, true)
		};
	}

	int direction;
	boolean flush;
	Color background;
	
	/** Create a CubeTransition3D with a black background.
	 * 
	 * @param direction one of the Transition constants: UP, DOWN, LEFT or RIGHT
	 * @param flush whether the z-axis should remain flush with the target destination.
	 * For example: if this is false then as a surface turns to the right a vertical edge
	 * will be pulled towards the camera and grow larger in height than than the target
	 * destination. So the center of the rotation is constant. But if this is true: then
	 * the center of the rotate pulls farther away from the camera to make sure that vertical
	 * edge is never larger than the height of the target graphics area.
	 */
	public CubeTransition3D(int direction,boolean flush) {
		this(direction, flush, Color.black);
	}

	/** Create a CubeTransition3D.
	 * 
	 * @param direction one of the Transition constants: UP, DOWN, LEFT or RIGHT
	 * @param flush whether the z-axis should remain flush with the target destination.
	 * For example: if this is false then as a surface turns to the right a vertical edge
	 * will be pulled towards the camera and grow larger in height than than the target
	 * destination. So the center of the rotation is constant. But if this is true: then
	 * the center of the rotate pulls farther away from the camera to make sure that vertical
	 * edge is never larger than the height of the target graphics area.
	 * @param background the optional background color to paint behind this transition.
	 */
	public CubeTransition3D(int direction,boolean flush,Color background) {
		if(!(direction==UP ||
				direction==DOWN ||
				direction==LEFT ||
				direction==RIGHT )) {
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
		Point3D topLeftA3D, topRightA3D, bottomLeftA3D, bottomRightA3D;
		Point3D topLeftB3D, topRightB3D, bottomLeftB3D, bottomRightB3D;
		BasicProjection p = new BasicProjection( w, h );
		double t = progress + 4f/8f;
		double k = Math.PI/2.0;
		if(direction==RIGHT) {
			double centerX = w/2.0;
			double z = Point2D.distance(centerX, centerX, 0, 0);
			double j = -z*Math.sin(2*Math.PI/8);
			topLeftA3D = new Point3D.Double( centerX - z*Math.cos(k*t), 0, z*Math.sin(k*t)+j );
			topRightA3D = new Point3D.Double( centerX - z*Math.cos(k*t+k), 0, z*Math.sin(k*t+k)+j );
			bottomLeftA3D = new Point3D.Double( centerX - z*Math.cos(k*t), h, z*Math.sin(k*t)+j );
			bottomRightA3D = new Point3D.Double( centerX - z*Math.cos(k*t+k), h, z*Math.sin(k*t+k)+j );
			topLeftB3D = new Point3D.Double( centerX - z*Math.cos(k*t-k), 0, z*Math.sin(k*t-k)+j );
			topRightB3D = new Point3D.Double( centerX - z*Math.cos(k*t), 0, z*Math.sin(k*t)+j );
			bottomLeftB3D = new Point3D.Double( centerX - z*Math.cos(k*t-k), h, z*Math.sin(k*t-k)+j );
			bottomRightB3D = new Point3D.Double( centerX - z*Math.cos(k*t), h, z*Math.sin(k*t)+j );
		} else if(direction==LEFT) {
			double centerX = w/2.0;
			double z = Point2D.distance(centerX, centerX, 0, 0);
			double j = -z*Math.sin(2*Math.PI/8);
			topRightA3D = new Point3D.Double( centerX + z*Math.cos(k*t), 0, z*Math.sin(k*t)+j );
			topLeftA3D = new Point3D.Double( centerX + z*Math.cos(k*t+k), 0, z*Math.sin(k*t+k)+j );
			bottomRightA3D = new Point3D.Double( centerX + z*Math.cos(k*t), h, z*Math.sin(k*t)+j );
			bottomLeftA3D = new Point3D.Double( centerX + z*Math.cos(k*t+k), h, z*Math.sin(k*t+k)+j );
			topRightB3D = new Point3D.Double( centerX + z*Math.cos(k*t-k), 0, z*Math.sin(k*t-k)+j );
			topLeftB3D = new Point3D.Double( centerX + z*Math.cos(k*t), 0, z*Math.sin(k*t)+j );
			bottomRightB3D = new Point3D.Double( centerX + z*Math.cos(k*t-k), h, z*Math.sin(k*t-k)+j );
			bottomLeftB3D = new Point3D.Double( centerX + z*Math.cos(k*t), h, z*Math.sin(k*t)+j );
		} else if(direction==DOWN) {
			double centerY = h/2.0;
			double z = Point2D.distance(centerY, centerY, 0, 0);
			double j = -z*Math.sin(2*Math.PI/8);
			topLeftA3D = new Point3D.Double( 0, centerY - z*Math.cos(k*t), z*Math.sin(k*t)+j );
			bottomLeftA3D = new Point3D.Double( 0, centerY - z*Math.cos(k*t+k), z*Math.sin(k*t+k)+j );
			topRightA3D = new Point3D.Double( w, centerY - z*Math.cos(k*t), z*Math.sin(k*t)+j );
			bottomRightA3D = new Point3D.Double( w, centerY - z*Math.cos(k*t+k), z*Math.sin(k*t+k)+j );
			topLeftB3D = new Point3D.Double( 0, centerY - z*Math.cos(k*t-k), z*Math.sin(k*t-k)+j );
			bottomLeftB3D = new Point3D.Double( 0, centerY - z*Math.cos(k*t), z*Math.sin(k*t)+j );
			topRightB3D = new Point3D.Double( w, centerY - z*Math.cos(k*t-k), z*Math.sin(k*t-k)+j );
			bottomRightB3D = new Point3D.Double( w, centerY - z*Math.cos(k*t), z*Math.sin(k*t)+j );
		} else {
			double centerY = h/2.0;
			double z = Point2D.distance(centerY, centerY, 0, 0);
			double j = -z*Math.sin(2*Math.PI/8);
			bottomLeftA3D = new Point3D.Double( 0, centerY + z*Math.cos(k*t), z*Math.sin(k*t)+j );
			topLeftA3D = new Point3D.Double( 0, centerY + z*Math.cos(k*t+k), z*Math.sin(k*t+k)+j );
			bottomRightA3D = new Point3D.Double( w, centerY + z*Math.cos(k*t), z*Math.sin(k*t)+j );
			topRightA3D = new Point3D.Double( w, centerY + z*Math.cos(k*t+k), z*Math.sin(k*t+k)+j );
			bottomLeftB3D = new Point3D.Double( 0, centerY + z*Math.cos(k*t-k), z*Math.sin(k*t-k)+j );
			topLeftB3D = new Point3D.Double( 0, centerY + z*Math.cos(k*t), z*Math.sin(k*t)+j );
			bottomRightB3D = new Point3D.Double( w, centerY + z*Math.cos(k*t-k), z*Math.sin(k*t-k)+j );
			topRightB3D = new Point3D.Double( w, centerY + z*Math.cos(k*t), z*Math.sin(k*t)+j );
		}
		
		if(flush)
			flushZCoordinateWithSurface(bottomLeftA3D, topLeftA3D, 
					bottomRightA3D, topRightA3D, 
					bottomLeftB3D, topLeftB3D, 
					bottomRightB3D, topRightB3D );
		
		Point2D topLeftA, topRightA, bottomLeftA, bottomRightA;
		Point2D topLeftB, topRightB, bottomLeftB, bottomRightB;
		bottomLeftA = p.transform( bottomLeftA3D );
		topLeftA = p.transform( topLeftA3D );
		bottomRightA = p.transform( bottomRightA3D );
		topRightA = p.transform( topRightA3D );
		bottomLeftB = p.transform( bottomLeftB3D );
		topLeftB = p.transform( topLeftB3D );
		bottomRightB = p.transform( bottomRightB3D );
		topRightB = p.transform( topRightB3D );

		//if the image is flipped (either horizontally or vertically): then it's
		//not supposed to be showing
		boolean visibleA = bottomRightA.getY()>topRightA.getY() && bottomRightA.getX()>bottomLeftA.getX();
		boolean visibleB = bottomRightB.getY()>topRightB.getY() && bottomRightB.getX()>bottomLeftB.getX();
		
		BufferedImage scratchImage = ResourcePool.get().getImage(w, h, BufferedImage.TYPE_INT_ARGB, true);
		try {
			ImageContext context = ImageContext.create(scratchImage);
			context.setRenderingHints(g.getRenderingHints());

			if(visibleB) {
				context.drawImage(frameB,
						topLeftB,
						topRightB,
						bottomRightB,
						bottomLeftB);
			}
			if(visibleA) {
				context.drawImage(frameA,
						topLeftA,
						topRightA,
						bottomRightA,
						bottomLeftA);
			}
			
			context.dispose();
			
			//draw the shadows
			GradientPaint shadowA, shadowB;
			if(direction==UP) {
				int alphaA = (int)( 255*(topLeftA.getX()/w*2) );
				int alphaB = (int)( 255*(bottomLeftB.getX()/w*2) );
				alphaA = Math.min(alphaA, 255);
				alphaB = Math.min(alphaB, 255);
				
				shadowA = new GradientPaint(0, (float)topLeftA.getY(), new Color(0,0,0,alphaA),
						0, (float)bottomLeftA.getY(), new Color(0,0,0,0));
				shadowB = new GradientPaint(0, (float)topLeftB.getY(), new Color(0,0,0,0),
						0, (float)bottomLeftB.getY(), new Color(0,0,0,alphaB));
			} else if(direction==DOWN) {
				int alphaA = (int)( 255*(bottomLeftA.getX()/w*2) );
				int alphaB = (int)( 255*(topLeftB.getX()/w*2) );
				alphaA = Math.min(alphaA, 255);
				alphaB = Math.min(alphaB, 255);
				
				shadowA = new GradientPaint(0, (float)bottomLeftA.getY(), new Color(0,0,0,alphaA),
						0, (float)topLeftA.getY(), new Color(0,0,0,0));
				shadowB = new GradientPaint(0, (float)topLeftB.getY(), new Color(0,0,0,alphaB),
						0, (float)bottomLeftB.getY(), new Color(0,0,0,0));
			} else if(direction==LEFT) {
				int alphaA = (int)( 255*(topLeftA.getY()/h*2) );
				int alphaB = (int)( 255*(topRightB.getY()/h*2) );
				alphaA = Math.min(alphaA, 255);
				alphaB = Math.min(alphaB, 255);
	
				shadowA = new GradientPaint((float)topLeftA.getX(), 0, new Color(0,0,0,alphaA),
						(float)topRightA.getX(), 0, new Color(0,0,0,0));
				shadowB = new GradientPaint((float)topLeftB.getX(), 0, new Color(0,0,0,0),
						(float)topRightB.getX(), 0, new Color(0,0,0,alphaB));
			} else { //right:
				int alphaA = (int)( 255*(topRightA.getY()/h*2) );
				int alphaB = (int)( 255*(topLeftB.getY()/h*2) );
				alphaA = Math.min(alphaA, 255);
				alphaB = Math.min(alphaB, 255);
				
				shadowA = new GradientPaint((float)topLeftA.getX(), 0, new Color(0,0,0,0),
						(float)topRightA.getX(), 0, new Color(0,0,0,alphaA));
				shadowB = new GradientPaint((float)topLeftB.getX(), 0, new Color(0,0,0,alphaB),
						(float)topRightB.getX(), 0, new Color(0,0,0,0));
			}
			
			Graphics2D g3 = scratchImage.createGraphics();
			g3.setComposite(AlphaComposite.SrcAtop);
			if(visibleA) {
				g3.setPaint(shadowA);
				g3.fill(getBounds(topLeftA, topRightA, bottomLeftA, bottomRightA));
			}
			if(visibleB) {
				g3.setPaint(shadowB);
				g3.fill(getBounds(topLeftB, topRightB, bottomLeftB, bottomRightB));
			}
			g3.dispose();

			
			g.drawImage(scratchImage,0,0,null);
		} finally {
			ResourcePool.get().put(scratchImage);
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("Cube ");
		if(direction==UP) {
			sb.append("Up");
		} else if(direction==DOWN) {
			sb.append("Down");
		} else if(direction==LEFT) {
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
