/*
 * @(#)StarsTransition2D.java
 *
 * $Date: 2014-06-06 20:04:49 +0200 (P, 06 jún. 2014) $
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

import com.bric.geom.RectangularTransform;
import com.bric.geom.ShapeBounds;
import com.bric.geom.ShapeStringUtils;
import com.bric.geom.ShapeUtils;
import com.bric.geom.TransformUtils;
import net.jafama.FastMath;

import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;
import java.util.Vector;

/** In this transition the current frame splits apart into
 * shrinking stars that spin off towards a distant point. Here are playback samples:
 * <p><table summary="Sample Animations of StarsTransition2D" cellspacing="50" border="0"><tr>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/StarsTransition2D/StarsLeft.gif" alt="Stars Left">
 * <p>Stars Left
 * </td>
 * <td align="center">
 * <img src="https://javagraphics.java.net/resources/transition/StarsTransition2D/StarsRight.gif" alt="Stars Right">
 * <p>Stars Right
 * </td>
 * </tr></table>
 * 
 */
public class StarsTransition2D extends AbstractClippedTransition2D {
	
	/** This public static method is used by the 
	 * {@link com.bric.image.transition.Transition2DDemoHelper}
	 * class to create sample animations of this transition.
	 * @return the transitions that should be used to demonstrate this
	 * transition.
	 */
	public static Transition[] getDemoTransitions() {
		return new Transition[] {
				new StarsTransition2D(LEFT), 
				new StarsTransition2D(RIGHT)
		};
	}
	
	static GeneralPath[] star = new GeneralPath[] {
		createStar(1.5f),
		createStar(1.6f),
		createStar(1.7f),
		createStar(1.8f),
		createStar(1.9f),
		createStar(2f),
		createStar(2.1f),
		createStar(2.2f),
		createStar(2.3f)
	};
	
	/** Creates a 5-pointed star.
	 * There are 2 distances involved.  The distance from the center to middle edge
	 * is 1, and the distance from the center to the tips of the star is r2.
	 * 
	 * @param r2
	 * @return
	 */
	private static GeneralPath createStar(float r2) {
		GeneralPath p = new GeneralPath();
		double angle = 0;
		double k = Math.PI*2/10;
		p.moveTo( (float)(FastMath.cos(angle)) , (float)(FastMath.sin(angle)) );
		for(int a = 0; a<5; a++) {
			p.lineTo( (float)(r2*FastMath.cos(angle+k)), (float)(r2*FastMath.sin(angle+k)) );
			angle+= Math.PI*2.0/5.0;
			p.lineTo( (float)(FastMath.cos(angle)), (float)(FastMath.sin(angle)) );
		}
		p.closePath();
		return p;
	}
    
    int type = RIGHT;

    /** Create a new stars transition that moves to the right.
     * 
     */
    public StarsTransition2D() {
        this(RIGHT);
    }
    
    /** Create a new stars transition.
     * 
     * @param type must be LEFT or RIGHT.
     */
    public StarsTransition2D(int type) {
        if(!(type==LEFT || type==RIGHT)) {
            throw new IllegalArgumentException("This transition must use type RIGHT or LEFT");
        }
        this.type = type;
    }
    
	@Override
	public String toString() {
        if(type==RIGHT) {
            return "Stars Right";
        } else {
            return "Stars Left";
        }
	}
	
	protected void fit(GeneralPath p,float length,float centerX, float centerY, GeneralPath path, Dimension size, float progress) {
		Rectangle2D r = p.getBounds2D();
		AffineTransform t = new AffineTransform();
		t.translate(-r.getX()-r.getWidth()/2,-r.getY()-r.getHeight()/2);
		t.rotate((1-progress)*1);
		double scaleProgress = Math.pow(progress,3)*.75f;
		t.scale(length/r.getWidth()*(.02+1.8*scaleProgress),length/r.getWidth()*(.02+1.8*scaleProgress));
		p.transform(t);
		
		if(progress>1) progress = 1;
		if(progress<0) progress = 0;
		Point2D endPoint = ShapeUtils.getPoint(path, 1);
		Point2D startPoint = ShapeUtils.getPoint(path, progress);
		Rectangle2D pathBounds = ShapeBounds.getBounds(path);
		AffineTransform pathTransform = RectangularTransform.create(
				pathBounds,
				new Rectangle2D.Float(0,0,size.width+100,size.height)
		);
		pathTransform.transform(endPoint, endPoint);
		pathTransform.transform(startPoint, startPoint);
		r = p.getBounds();
		t.setToTranslation( -r.getCenterX()+centerX-endPoint.getX()+startPoint.getX(), 
				 -r.getCenterY()+centerY-endPoint.getY()+startPoint.getY());
		
		p.transform(t);
	}

	GeneralPath[] paths = new GeneralPath[] {
			ShapeStringUtils.createGeneralPath("m 82.604 6.405 c 81.496 6.405 58.748 5.967 57.234 5.937 c 48.657 5.767 39.783 5.605 30.4 11.819 c 19.367 19.125 9.915 39.783 23.713 50.988 c 35.754 60.766 50.748 54.184 53.807 47.734 c 56.105 42.887 49.464 38.223 45.159 38.223"),
			ShapeStringUtils.createGeneralPath("m 130.936 47.089 c 130.636 46.6 113.679 45.149 103.386 45.364 c 94.251 45.555 88.013 49.832 82.977 54.875 c 75.353 62.51 70.458 72.743 73.292 82.281 c 76.126 91.818 93.239 89.414 93.239 89.414 c 93.239 89.414 101.796 85.728 100.734 78.276 c 99.683 70.903 96.561 71.393 93.124 71.393 c 84.366 71.393 85.661 83.327 94.277 78.651"),
			ShapeStringUtils.createGeneralPath("m 124.379 23.124 c 124.044 24.216 107.26 20.206 97.997 21.225 c 88.734 22.245 74.072 27.614 64.329 38.119 c 54.586 48.624 52.078 53.184 52.683 61.27 c 53.288 69.356 71.622 78.91 77.935 66.901")
	};
	
	@Override
	public Shape[] getShapes(float progress, Dimension size) {
		progress = 1-progress;

		GeneralPath star1 = new GeneralPath(star[8]);
		GeneralPath star2 = new GeneralPath(star[5]);
		GeneralPath star3 = new GeneralPath(star[8]);
		GeneralPath star4 = new GeneralPath(star[5]);
		GeneralPath star5 = new GeneralPath(star[7]);
		GeneralPath star6 = new GeneralPath(star[5]);
		GeneralPath star7 = new GeneralPath(star[8]);
		GeneralPath star8 = new GeneralPath(star[6]);
		
		Random random = new Random(2);
		
		star1.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star2.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star3.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star4.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star5.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star6.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star7.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		star8.transform(AffineTransform.getRotateInstance(random.nextDouble()));
		
		float big = (Math.min(size.width,size.height))*.7f;
		float base1 = (float)(Math.pow(progress,2.2)*.5f+0f/8f*.3f);
		float base2 = (float)(Math.pow(progress,2.2)*.5f+1f/8f*.3f);
		float base3 = (float)(Math.pow(progress,2.2)*.5f+2f/8f*.3f);
		float base4 = (float)(Math.pow(progress,2.2)*.5f+3f/8f*.3f);
		float base5 = (float)(Math.pow(progress,2.2)*.5f+4f/8f*.3f);
		float base6 = (float)(Math.pow(progress,2.2)*.5f+5f/8f*.3f);
		float base7 = (float)(Math.pow(progress,2.2)*.5f+6f/8f*.3f);
		float base8 = (float)(Math.pow(progress,2.2)*.5f+7f/8f*.3f);
		float progress1 = (progress-base1)/(1-base1);
		float progress2 = (progress-base2)/(1-base2);
		float progress3 = (progress-base3)/(1-base3);
		float progress4 = (progress-base4)/(1-base4);
		float progress5 = (progress-base5)/(1-base5);
		float progress6 = (progress-base6)/(1-base6);
		float progress7 = (progress-base7)/(1-base7);
		float progress8 = (progress-base8)/(1-base8);
		Vector<GeneralPath> v = new Vector<GeneralPath>();
		
		if(progress1>0) {
			fit(star1,big, size.width*2f/3f, size.height*3f/4f, paths[0], size, progress1*2);
			v.add(star1);
		}
		if(progress2>0) {
			fit(star2,big, size.width*7f/8f, size.height*1f/5f, paths[1], size, progress2*2);
			v.add(star2);
		}
		if(progress3>0) {
			fit(star3,big, size.width*1f/6f, size.height*2.2f/5f, paths[2], size, progress3*2);
			v.add(star3);
		}
		if(progress4>0) {
			fit(star4,big, size.width*3.1f/6f, size.height*1.2f/5f, paths[0], size, progress4*2);
			v.add(star4);
		}
		if(progress5>0) {
			fit(star5,big, size.width*1.9f/6f, size.height*4.2f/5f, paths[1], size, progress5*2);
			v.add(star5);
		}
		if(progress6>0) {
			fit(star6,big, size.width*13f/15f, size.height*4.3f/5f, paths[2], size, progress6*2);
			v.add(star6);
		}
		if(progress7>0) {
			fit(star7,big, size.width*2f/5f, size.height*2.4f/5f, paths[0], size, progress7*2);
			v.add(star7);
		}
		if(progress8>0) {
			fit(star8,big, size.width*3f/6f, size.height*2f/5f, paths[2], size, progress8*2);
			v.add(star8);
		}
		
		Shape[] shapes = v.toArray(new Shape[v.size()]);
        if(type==LEFT) {
            AffineTransform flipHorizontal = TransformUtils.createAffineTransform(0,0,
                    0,size.height,
                    size.width,0,
                    size.width,0,
                    size.width,size.height,
                    0,0);
            for(int a = 0; a<shapes.length; a++) {
                if(shapes[a] instanceof GeneralPath) {
                    ((GeneralPath)shapes[a]).transform(flipHorizontal);
                } else {
                  shapes[a] = flipHorizontal.createTransformedShape(shapes[a]);
                }
            }
        }
        return shapes;
	}

	@Override
	public float getStrokeWidth(float progress,Dimension size) {
		return 2+7*(1-progress);
	}

    @Override
	public Transition2DInstruction[] getInstructions(float progress, Dimension size) {
        //This transition was written, um, backwards.  So the simplest solution is to
        //meddle with the resulting frames...
        Transition2DInstruction[] instr = super.getInstructions(progress, size);
        for(int a = 0; a<instr.length; a++) {
            if(instr[a] instanceof ImageInstruction) {
                ImageInstruction i = (ImageInstruction)instr[a];
                i.isFirstFrame = !i.isFirstFrame;
            }
        }
        return instr;
    }
    
    
}
