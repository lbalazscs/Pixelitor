/*
 * @(#)ImageInstruction.java
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
package com.bric.image.transition;

import com.bric.geom.Clipper;
import com.bric.geom.RectangularTransform;
import com.bric.geom.ShapeStringUtils;
import org.jdesktop.swingx.graphics.BlendComposite;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** This is an instruction to render an image.
 */
public class ImageInstruction extends Transition2DInstruction {
	/** If this is true, then this instruction relates to Frame A.  Otherwise this relates to Frame B. */
	public boolean isFirstFrame = true;

	/** This is the clipping to apply.  This may be null, indicating that clipping is not needed.  */
	public Shape clipping = null;

	/** This is the transform to render this graphic through.  This may be null, indicating that no transform is needed.  */
	public AffineTransform transform = null;
	
	/** This is the opacity of this image.  By default this value is 1. */
	public float opacity = 1;
		
	public ImageInstruction(boolean isFirstFrame,float opacity,AffineTransform transform,Shape clipping) {
		this(isFirstFrame,transform,clipping);
		this.opacity = opacity;
	}
	
	public ImageInstruction(boolean isFirstFrame,float opacity) {
		this(isFirstFrame);
		this.opacity = opacity;
	}
	
	public ImageInstruction(boolean isFirstFrame,float opacity,Rectangle2D dest,Dimension frameSize,Shape clipping) {
		this(isFirstFrame,opacity,RectangularTransform.create(new Rectangle2D.Double(0,0,frameSize.width,frameSize.height),dest),clipping);
	}
	
	/** Renders a completely opaque image, anchored at (0,0), at its original
	 * size with no clipping.
	 * @param isFirstFrame indicates whether to use the original image or
	 * the incoming image.
	 */
	public ImageInstruction(boolean isFirstFrame) {
		this(isFirstFrame,null,null);
	}
	
	@Override
	public String toString() {
		String clippingString = (clipping==null) ? "null" : ShapeStringUtils.toString(clipping);
		return "ImageInstruction[ isFirstFrame = "+isFirstFrame+", transform = "+transform+", clipping = "+clippingString+" opacity="+opacity+"]";
	}
	
	/** Creates a shallow clone of the argument */
	public ImageInstruction(ImageInstruction i) {
		this.clipping = i.clipping;
		this.isFirstFrame = i.isFirstFrame;
		this.transform = i.transform;
		this.opacity = i.opacity;
	}
	
	public ImageInstruction(boolean isFirstFrame,AffineTransform transform,Shape clipping) {
		this.isFirstFrame = isFirstFrame;
		if(transform!=null)
			this.transform = new AffineTransform(transform);
		if(clipping!=null) {
			if(clipping instanceof Rectangle) {
				this.clipping = new Rectangle((Rectangle)clipping);
			} else if(clipping instanceof Rectangle2D) {
				Rectangle2D r = new Rectangle2D.Float();
				r.setFrame((Rectangle2D)clipping);
				this.clipping = r;
			} else {
				this.clipping = new GeneralPath(clipping);
			}
		}
	}
	
	public ImageInstruction(boolean isFirstFrame,Rectangle2D dest,Dimension frameSize,Shape clipping) {
		this(isFirstFrame,RectangularTransform.create(new Rectangle2D.Double(0,0,frameSize.width,frameSize.height),dest),clipping);
	}

	@Override
	public void paint(Graphics2D g, BufferedImage frameA, BufferedImage frameB) {
		// Laszlo: In Pixelitor frameB is always transparent, the following
		// code will work only in that case - but the original code used to work
		// only with opaque images, see http://javagraphics.blogspot.hu/2008/06/crossfades-what-is-and-isnt-possible.html


		BufferedImage img = isFirstFrame ? frameA : frameB;

		Composite oldComposite = null;
//		if(opacity!=1) {
			oldComposite = g.getComposite();
//			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
//			g.setComposite(new CrossFadeToTransparentComposite(opacity));
//			g.setComposite(new AddComposite(opacity));
//			g.setComposite(BlendComposite.Src.derive(opacity));

			if(isFirstFrame) {
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
			} else {
//				g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, opacity));
				g.setComposite(BlendComposite.CrossFade.derive(opacity));
			}

		//}

		Shape oldClipping = null;
		if(clipping!=null) {
			oldClipping = g.getClip();
			Clipper.clip(g, clipping);
		}

		g.drawImage(img,transform,null);

		if(clipping!=null)
			g.setClip(oldClipping);

		if(opacity!=1) {
			g.setComposite(oldComposite);
		}
	}
}
