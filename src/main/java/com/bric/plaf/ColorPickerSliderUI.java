/*
 * @(#)ColorPickerSliderUI.java
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
package com.bric.plaf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicSliderUI;

import com.bric.swing.ColorPicker;
import com.bric.swing.ColorPickerPanel;

/** This is a SliderUI designed specifically for the
 * <code>ColorPicker</code>.
 * 
 */
public class ColorPickerSliderUI extends BasicSliderUI {
	ColorPicker colorPicker;
	
	/** Half of the height of the arrow */
	int ARROW_HALF = 8;
	
	int[] intArray = new int[ Toolkit.getDefaultToolkit().getScreenSize().height ];
	BufferedImage bi = new BufferedImage(1,intArray.length,BufferedImage.TYPE_INT_RGB);
	int lastMode = -1;

	public ColorPickerSliderUI(JSlider b,ColorPicker cp) {
		super(b);
		colorPicker = cp;
		cp.getColorPanel().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				ColorPickerSliderUI.this.calculateGeometry();
				slider.repaint();
			}
		});
	}

	@Override
	public void paintThumb(Graphics g) {
		int y = thumbRect.y+thumbRect.height/2;
		Polygon polygon = new Polygon();
		polygon.addPoint(0,y-ARROW_HALF);
		polygon.addPoint(ARROW_HALF,y);
		polygon.addPoint(0,y+ARROW_HALF);
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.black);
		g2.fill(polygon);
		g2.setColor(Color.white);
		g2.setStroke(new BasicStroke(1));
		g2.draw(polygon);
	}

	@Override
	protected void calculateThumbSize() {
		super.calculateThumbSize();
		thumbRect.height+=4;
		thumbRect.y-=2;
	}

	@Override
	protected void calculateTrackRect() {
		super.calculateTrackRect();
		ColorPickerPanel cp = colorPicker.getColorPanel();
		int size = Math.min(ColorPickerPanel.MAX_SIZE, Math.min(cp.getWidth(), cp.getHeight()));
		int max = slider.getHeight()-ARROW_HALF*2-2;
		if(size>max) {
			size = max;
		}
		trackRect.y = slider.getHeight()/2-size/2;
		trackRect.height = size;
	}

	@Override
	public synchronized void paintTrack(Graphics g) {
		int mode = colorPicker.getMode();
		if(mode==ColorPicker.HUE || mode==ColorPicker.BRI || mode==ColorPicker.SAT) {
			float[] hsb = colorPicker.getHSB();
			if(mode==ColorPicker.HUE) {
				for(int y = 0; y<trackRect.height; y++) {
					float hue = ((float)y)/((float)trackRect.height);
					intArray[y] = Color.HSBtoRGB( hue, 1, 1);
				}
			} else if(mode==ColorPicker.SAT) {
				for(int y = 0; y<trackRect.height; y++) {
					float sat = 1-((float)y)/((float)trackRect.height);
					intArray[y] = Color.HSBtoRGB( hsb[0], sat, hsb[2]);
				}
			} else {
				for(int y = 0; y<trackRect.height; y++) {
					float bri = 1-((float)y)/((float)trackRect.height);
					intArray[y] = Color.HSBtoRGB( hsb[0], hsb[1], bri);
				}
			}
		} else {
			int[] rgb = colorPicker.getRGB();
			if(mode==ColorPicker.RED) {
				for(int y = 0; y<trackRect.height; y++) {
					int red = 255-(int)(y*255/trackRect.height+.49);
					intArray[y] = (red << 16)+(rgb[1] << 8)+(rgb[2]);
				}
			} else if(mode==ColorPicker.GREEN) {
				for(int y = 0; y<trackRect.height; y++) {
					int green = 255-(int)(y*255/trackRect.height+.49);
					intArray[y] = (rgb[0] << 16)+(green << 8)+(rgb[2]);
				}
			} else if(mode==ColorPicker.BLUE) {
				for(int y = 0; y<trackRect.height; y++) {
					int blue = 255-(int)(y*255/trackRect.height+.49);
					intArray[y] = (rgb[0] << 16)+(rgb[1] << 8)+(blue);
				}
			}
		}
		Graphics2D g2 = (Graphics2D)g;
		Rectangle r = new Rectangle(6, trackRect.y, 14, trackRect.height);
		if(slider.hasFocus()) {
			PlafPaintUtils.paintFocus(g2,r,3);
		}
		
		bi.getRaster().setDataElements(0,0,1,trackRect.height,intArray);
		TexturePaint p = new TexturePaint(bi,new Rectangle(0,trackRect.y,1,bi.getHeight()));
		g2.setPaint(p);
		g2.fillRect(r.x,r.y,r.width,r.height);
		
		PlafPaintUtils.drawBevel(g2, r);
	}
	
	@Override
	public void paintFocus(Graphics g) {}

	/** This overrides the default behavior for this slider
	 * and sets the thumb to where the user clicked.
	 * From a design standpoint, users probably don't want to
	 * scroll through several colors to get where they clicked:
	 * they simply want the color they selected.
	 */
	MouseInputAdapter myMouseListener = new MouseInputAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			slider.setValueIsAdjusting(true);
			updateSliderValue(e);
		}
		private void updateSliderValue(MouseEvent e) {
			int v;
			if(slider.getOrientation()==SwingConstants.HORIZONTAL) {
				int x = e.getX();
				v = valueForXPosition(x);
			} else {
				int y = e.getY();
				v = valueForYPosition(y);
			}
			slider.setValue(v);
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			updateSliderValue(e);
			slider.setValueIsAdjusting(false);
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			updateSliderValue(e);
		}
	};

	@Override
	protected void installListeners(JSlider slider) {
		super.installListeners(slider);
		slider.removeMouseListener(trackListener);
		slider.removeMouseMotionListener(trackListener);
		slider.addMouseListener(myMouseListener);
		slider.addMouseMotionListener(myMouseListener);
		slider.setOpaque(false);
	}

	@Override
	protected void uninstallListeners(JSlider slider) {
		super.uninstallListeners(slider);
		slider.removeMouseListener(myMouseListener);
		slider.removeMouseMotionListener(myMouseListener);
	}

	
}
