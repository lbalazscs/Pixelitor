/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.awt;

import net.jafaran.MXSIntSeqRNG;

import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

public class WobbleStroke implements Stroke {
	private float detail = 2;
	private float amplitude = 2;
	private static final float FLATNESS = 1;
    private Random rand;
    private float basicStrokeWidth;

    private long seed;

    public WobbleStroke(float detail, float amplitude, float basicStrokeWidth) {
		this.detail	= detail;
		this.amplitude	= amplitude;
        this.basicStrokeWidth = basicStrokeWidth;

        rand = new MXSIntSeqRNG();
        seed = System.nanoTime();
	}

	public Shape createStrokedShape( Shape shape ) {
        rand.setSeed(seed);

		GeneralPath result = new GeneralPath();
        shape = new BasicStroke(basicStrokeWidth).createStrokedShape( shape );
		PathIterator it = new FlatteningPathIterator( shape.getPathIterator( null ), FLATNESS );
		float points[] = new float[6];
		float moveX = 0, moveY = 0;
		float lastX = 0, lastY = 0;
		float thisX = 0, thisY = 0;
		int type = 0;
//		boolean first = false;
		float next = 0;

		while ( !it.isDone() ) {
			type = it.currentSegment( points );
			switch( type ){
			case PathIterator.SEG_MOVETO:
				moveX = lastX = randomize( points[0] );
				moveY = lastY = randomize( points[1] );
				result.moveTo( moveX, moveY );
//				first = true;
				next = 0;
				break;

			case PathIterator.SEG_CLOSE:
				points[0] = moveX;
				points[1] = moveY;
				// Fall into....

			case PathIterator.SEG_LINETO:
				thisX = randomize( points[0] );
				thisY = randomize( points[1] );
				float dx = thisX-lastX;
				float dy = thisY-lastY;
				float distance = (float)Math.sqrt( dx*dx + dy*dy );
				if ( distance >= next ) {
					float r = 1.0f/distance;
//					float angle = (float)Math.atan2( dy, dx );
					while ( distance >= next ) {
						float x = lastX + next*dx*r;
						float y = lastY + next*dy*r;
						result.lineTo( randomize( x ), randomize( y ) );
						next += detail;
					}
				}
				next -= distance;
//				first = false;
				lastX = thisX;
				lastY = thisY;
				break;
			}
			it.next();
		}

		return result;
	}

    private float randomize( float x ) {
//        return x +(float)Math.random()*amplitude*2-1;

        float delta = 2 * (amplitude * (rand.nextFloat() - 0.5f));
        return x + delta;
    }



}
