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

import java.awt.*;
import java.awt.geom.*;

public class ShapeStroke implements Stroke {
	private Shape shapes[];
	private float advance;
	private boolean repeat = true;
	private AffineTransform t = new AffineTransform();
	private static final float FLATNESS = 1;

	public ShapeStroke( Shape shapes, float advance ) {
		this( new Shape[] { shapes }, advance );
	}

	public ShapeStroke( Shape shapes[], float advance ) {
		this.advance = advance;
		this.shapes = new Shape[shapes.length];

		for ( int i = 0; i < this.shapes.length; i++ ) {
			Rectangle2D bounds = shapes[i].getBounds2D();
			t.setToTranslation( -bounds.getCenterX(), -bounds.getCenterY() );
			this.shapes[i] = t.createTransformedShape( shapes[i] );
		}
	}

	public Shape createStrokedShape( Shape shape ) {
		GeneralPath result = new GeneralPath();
		PathIterator it = new FlatteningPathIterator( shape.getPathIterator( null ), FLATNESS );
		float points[] = new float[6];
		float moveX = 0, moveY = 0;
		float lastX = 0, lastY = 0;
		float thisX = 0, thisY = 0;
		int type = 0;
		boolean first = false;
		float next = 0;
		int currentShape = 0;
		int length = shapes.length;

		float factor = 1;

		while ( currentShape < length && !it.isDone() ) {
			type = it.currentSegment( points );
			switch( type ){
			case PathIterator.SEG_MOVETO:
				moveX = lastX = points[0];
				moveY = lastY = points[1];
				result.moveTo( moveX, moveY );
				first = true;
				next = 0;
				break;

			case PathIterator.SEG_CLOSE:
				points[0] = moveX;
				points[1] = moveY;
				// Fall into....

			case PathIterator.SEG_LINETO:
				thisX = points[0];
				thisY = points[1];
				float dx = thisX-lastX;
				float dy = thisY-lastY;
				float distance = (float)Math.sqrt( dx*dx + dy*dy );
				if ( distance >= next ) {
					float r = 1.0f/distance;
					float angle = (float)Math.atan2( dy, dx );
					while ( currentShape < length && distance >= next ) {
						float x = lastX + next*dx*r;
						float y = lastY + next*dy*r;
						t.setToTranslation( x, y );
						t.rotate( angle );
						result.append( t.createTransformedShape( shapes[currentShape] ), false );
						next += advance;
						currentShape++;
						if ( repeat )
							currentShape %= length;
					}
				}
				next -= distance;
				first = false;
				lastX = thisX;
				lastY = thisY;
				break;
			}
			it.next();
		}

		return result;
	}

}
