/*
 * @(#)PlafPaintUtils.java
 *
 * $Date: 2014-03-16 23:30:29 +0100 (V, 16 márc. 2014) $
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
package com.bric.plaf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.bric.util.JVM;

/** Some static methods for some common painting functions.
 *
 * @author Jeremy Wood
 **/
public class PlafPaintUtils {

	/** Four shades of white, each with increasing opacity. */
	final static Color[] whites = new Color[] {
			new Color(255,255,255,50),
			new Color(255,255,255,100),
			new Color(255,255,255,150)
	};
	
	/** Four shades of black, each with increasing opacity. */
	final static Color[] blacks = new Color[] {
			new Color(0,0,0,50),
			new Color(0,0,0,100),
			new Color(0,0,0,150)
	};
	
	/** @return the color used to indicate when a component has
	 * focus.  By default this uses the color (64,113,167), but you can
	 * override this by calling:
	 * <BR><code>UIManager.put("focusRing",customColor);</code>
	 */
	public static Color getFocusRingColor() {
		Object obj = UIManager.getColor("Focus.color");
		if(obj instanceof Color)
			return (Color)obj;
		obj = UIManager.getColor("focusRing");
		if(obj instanceof Color)
			return (Color)obj;
		return new Color(64,113,167);
	}
	
	/** Paints 3 different strokes around a shape to indicate focus.
	 * The widest stroke is the most transparent, so this achieves a nice
	 * "glow" effect.
	 * <P>The catch is that you have to render this underneath the shape,
	 * and the shape should be filled completely.
	 * 
	 * @param g the graphics to paint to
	 * @param shape the shape to outline
	 * @param pixelSize the number of pixels the outline should cover.
	 */
	public static void paintFocus(Graphics2D g,Shape shape,int pixelSize) {
		g = (Graphics2D)g.create();
		try {
			Color focusColor = getFocusRingColor();
			Color[] focusArray = new Color[] {
				new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(),235),
				new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(),130),
				new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(),80)	
			};
			if(JVM.usingQuartz) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			} else {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
			}
			
			g.setStroke(new BasicStroke(2*pixelSize+1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(focusArray[2]);
			g.draw(shape);
			if(2*pixelSize+1>0) {
				g.setStroke(new BasicStroke(2*pixelSize-2+1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(focusArray[1]);
				g.draw(shape);
			}
			if(2*pixelSize-4+1>0) {
				g.setStroke(new BasicStroke(2*pixelSize-4+1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(focusArray[0]);
				g.draw(shape);
			}
		} finally {
			g.dispose();
		}
	}
	
	/** Uses translucent shades of white and black to draw highlights
	 * and shadows around a rectangle, and then frames the rectangle
	 * with a shade of gray (120).
	 * <P>This should be called to add a finishing touch on top of
	 * existing graphics.
	 * @param g the graphics to paint to.
	 * @param r the rectangle to paint.
	 */
	public static void drawBevel(Graphics2D g,Rectangle r) {
		g.setStroke(new BasicStroke(1));
		drawColors(blacks,g, r.x, r.y+r.height, r.x+r.width, r.y+r.height, SwingConstants.SOUTH);
		drawColors(blacks,g, r.x+r.width, r.y, r.x+r.width, r.y+r.height, SwingConstants.EAST);

		drawColors(whites,g, r.x, r.y, r.x+r.width, r.y, SwingConstants.NORTH);
		drawColors(whites,g, r.x, r.y, r.x, r.y+r.height, SwingConstants.WEST);
		
		g.setColor(new Color(120, 120, 120));
		g.drawRect(r.x, r.y, r.width, r.height);
	}
	
	private static void drawColors(Color[] colors,Graphics g,int x1,int y1,int x2,int y2,int direction) {
		for(int a = 0; a<colors.length; a++) {
			g.setColor(colors[colors.length-a-1]);
			if(direction==SwingConstants.SOUTH) {
				g.drawLine(x1, y1-a, x2, y2-a);
			} else if(direction==SwingConstants.NORTH) {
				g.drawLine(x1, y1+a, x2, y2+a);
			} else if(direction==SwingConstants.EAST) {
				g.drawLine(x1-a, y1, x2-a, y2);
			} else if(direction==SwingConstants.WEST) {
				g.drawLine(x1+a, y1, x2+a, y2);
			}
		}
	}
	
	/** The table used to store vertical gradients. */
	private static Hashtable<String, TexturePaint> verticalGradients;
	
	/** Create a vertical gradient.  This gradient is stored in a
	 * table and reused throughout the rest of this session.
	 * 
	 * @param name an identifying key for this gradient (used to cache it).
	 * @param height the height of the gradient
	 * @param y the y offset of the gradient
	 * @param positions the fractional positions of each color (between [0,1]).
	 * @param colors one color for each position.
	 * @return the vertical gradient.
	 */
	synchronized static Paint getVerticalGradient(String name,
			int height,int y,
			float[] positions,
			Color[] colors) {
		if(verticalGradients==null) {
			verticalGradients = new Hashtable<String, TexturePaint>();
		}
		
		String key = name+" "+height+" "+y;
		TexturePaint paint = verticalGradients.get(key);
		if(paint==null) {
			height = Math.max(height, 1); //before a component is laid out, it may be 0x0
			BufferedImage bi = new BufferedImage(1,height,BufferedImage.TYPE_INT_ARGB);
			int[] array = new int[height];
			for(int a = 0; a<array.length; a++) {
				float f = a;
				f = f/((array.length-1));
				boolean hit = false;
				findMatch : for(int b = 1; b<positions.length; b++) {
					if(f>=positions[b-1] && f<positions[b]) {
						float p = (f-positions[b-1])/(positions[b]-positions[b-1]);
						array[a] = tween(colors[b-1],colors[b],p).getRGB();
						hit = true;
						break findMatch;
					}
				}
				if(!hit)
					array[a] = colors[colors.length-1].getRGB();
			}
			bi.getRaster().setDataElements(0, 0, 1, height, array);
			paint = new TexturePaint( bi, new Rectangle(0,y,1,height) );
			verticalGradients.put(key,paint);
		}
		return paint;
	}
	
	/** Tweens between the two arguments. */
	private static Color tween(Color c1,Color c2,float p) {
		int r1 = c1.getRed();
		int g1 = c1.getGreen();
		int b1 = c1.getBlue();
		int a1 = c1.getAlpha();
		
		int r2 = c2.getRed();
		int g2 = c2.getGreen();
		int b2 = c2.getBlue();
		int a2 = c2.getAlpha();
		
		return new Color( (int)(r1*(1-p)+r2*p),
				(int)(g1*(1-p)+g2*p),
				(int)(b1*(1-p)+b2*p),
				(int)(a1*(1-p)+a2*p) 
		);
	}

	private static Hashtable<String, TexturePaint> checkers;
	public static TexturePaint getCheckerBoard(int checkerSize) {
		return getCheckerBoard( checkerSize, Color.white, Color.lightGray );
	}
	public static TexturePaint getCheckerBoard(int checkerSize,Color color1,Color color2) {
		String key = checkerSize+" "+color1.toString()+" "+color2.toString();
		if(checkers==null)
			checkers = new Hashtable<String, TexturePaint>();
		TexturePaint paint = checkers.get(key);
		if(paint==null) {
			BufferedImage bi = new BufferedImage(2*checkerSize, 2*checkerSize, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bi.createGraphics();
			g.setColor(color1);
			g.fillRect(0,0,2*checkerSize,2*checkerSize);
			g.setColor(color2);
			g.fillRect(0,0,checkerSize,checkerSize);
			g.fillRect(checkerSize,checkerSize,checkerSize,checkerSize);
			g.dispose();
			paint = new TexturePaint(bi,new Rectangle(0,0,bi.getWidth(),bi.getHeight()));
			checkers.put(key, paint);
		}
		return paint;
	}
}
