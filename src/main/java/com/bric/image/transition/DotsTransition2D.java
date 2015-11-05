/*
 * @(#)DotsTransition2D.java
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

import net.jafama.FastMath;

import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.Random;
import java.util.Vector;

/** This is a playful series of dots bubbling up from nowhere to
 * produce the incoming image. Here is a playback sample:
 * <p><img src="https://javagraphics.java.net/resources/transition/DotsTransition2D/Dots.gif" alt="DotsTransition2D Demo">
 * 
 */
public class DotsTransition2D extends AbstractClippedTransition2D {
	
	Ellipse2D[] bubbles = new Ellipse2D[] {
			make( 0.680542285974303, 0.18889704343632806, 0.19951594932207756 ),
			make( 0.026909280976121663, 0.6901511430360959, 0.1906895473555708 ),
			make( 199.0/200, 88.0/200, 0.03 ),
			make( 0.29737678359980557, 0.3283705267534811, 0.0912077460942279 ),
			make( 0.4940590401790048, 0.02553734599175539, 0.16311957162512475 ),
			make( 0.43494816182266074, 0.25633754562508044, 0.1165154509890553 ),
			make( 0.9809361717371541, 0.30209191539494906, 0.13332448677092352 ),
			make( 0.94840475449474, 0.9709666965127903, 0.1916617398526077 ),
			make( 0.20824174526117323, 0.6000480763996424, 0.16450145156027104 ),
			make( 49.0/200, 6.0/200, 0.1264350652695335 ),
			make( 0.63129647161756, 0.7748508554123787, 0.13777349084037474 ),
			make( 24.0/200.0, 23.0/200.0, 0.05665589165107964 ),
			make( 0.04541801270979995, 0.31964490650135213, 0.19585902380194273 ),
			make( 0.568725862339568, 0.5659780448945103, 0.10445627774737297 ),
			make( 113.0/200.0, 76.0/200.0 , 0.10292414822016213 ),
			make( 0.9407853703745508, 0.7099945865685147, 0.14021507084018198 ),
			make( 0.8430323086248085, 0.5875060184753639, 0.19975669996812462 ),
			make( 0.7908761878447338, 0.9138289915660384, 0.14630462931216134 ),
			make( 0.7885669941774602, 0.45314728864907283, 0.19542608515347965 ),
			make( 0.3908460558351622, 0.4880566885333508, 0.19958992150674115 ),
			make( 0.2543162939332376, 0.16250491418006419, 0.13575009482043436 ),
			make( 0.3009613498958983, 0.7949264449270182, 0.16819513825380095 ),
			make( 0.007611521495801465, 0.015461107683142572, 0.13790660268586205 ),
			make( 0.18134036468339323, 0.9533112934957934, 0.19875969340753744 ),
			make( 0.43773209673997937, 0.6987587742834488, 0.1450244188942148 ),
			make( 0.5586433953881341, 0.9556322415516078, 0.18462455447167833 ),
			make( 0.9464285339584122, 0.034394073749900445, 0.21788008758584236 )
	};
	
	/** Creates a new dots transition.
	 * 
	 */
	public DotsTransition2D() {
		//here's the code that helps generate the list of bubbles:
		//note the final list was then pruned by hand and tweaked to really cut back
		//on the number of bubbles.
		/*int bubbleCount = 20;
		long seed = System.currentTimeMillis();
		boolean passed = false;
		int ctr = 0;
		while(passed==false) {
			ctr++;
			bubbles = new Ellipse2D[bubbleCount+ctr/5000];
			System.out.println(seed+", "+bubbles.length);
			Random r = new Random(seed);
			for(int a = 0; a<bubbles.length; a++) {
				bubbles[a] = make(
						r.nextDouble(),
						r.nextDouble(),
						.1+.1*r.nextDouble()
						);
			}
			BasicShape sum = new BasicShape();
			for(int a = 0; a<bubbles.length; a++) {
				Ellipse2D e = bubbles[a];
				AffineTransform t = new AffineTransform();
				t.rotate(2,e.getCenterX(),e.getCenterY());
				sum.add(new BasicShape(t.createTransformedShape(e)));
			}
			passed = sum.contains(new Rectangle(0,0,1,1));
			seed++;
		}
		for(int a = 0; a<bubbles.length; a++) {
			System.out.println("make( "+bubbles[a].getCenterX()+", "+bubbles[a].getCenterY()+", "+bubbles[a].getWidth()/2+" )");
		}
		
		*/
	}
	
	protected Ellipse2D.Double make(double x,double y,double r) {
		return new Ellipse2D.Double(x-r,y-r,r*2,r*2);
	}
	
	@Override
	public Shape[] getShapes(float progress,Dimension size) {
		Vector<Shape> v = new Vector<Shape>();
		float domain = .9f;
		float span = 1-domain;
		Random random = new Random();
		
		for(int a = 0; a<bubbles.length; a++) {
			float k = (a)/(bubbles.length-1f);
			float base = (float)(Math.sqrt(k)*.25+k*.75)*domain;
			
			float p = (progress-base)/span;
			if(p>0.01) {
				if(p>1) p = 1;
				
				//pop it over 1:
				p = (float)(-1.6666666666666186*p*p+2.6666666666666203*p);
				
				float r = (float)Math.max(bubbles[a].getWidth()*size.width/2,
						bubbles[a].getHeight()*size.height/2);
				r = r*p;
				
				random.setSeed(10*a);
				float dx = (1-p)*(2*random.nextFloat()-1);
				float dy = (1-p)*(2*random.nextFloat()-1);
				dx = (1-p)*(float)(r* FastMath.cos(random.nextFloat()*10+8*(1-p)));
				dy = (1-p)*(float)(r*FastMath.sin(random.nextFloat()*10+8*(1-p)));
				
				v.add( new Ellipse2D.Double(
						bubbles[a].getCenterX()*size.width-r+dx,
						bubbles[a].getCenterY()*size.height-r+dy,
						2*r,
						2*r
						) );
			}
		}
		
		return v.toArray(new Shape[v.size()]);
	}
	
	@Override
	public float getStrokeWidth(float progress,Dimension size) {
		float f = Math.max(size.width - 300f, 0);
		f = f/700f+.3f;
		return (float)(10*(1-Math.pow(progress,5)))*f;
	}

	@Override
	public String toString() {
		return "Dots";
	}
}
