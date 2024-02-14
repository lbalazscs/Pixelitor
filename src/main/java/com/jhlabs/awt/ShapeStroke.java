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

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.*;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;

/**
 * A {@link Stroke} which takes an array of existing {@link Shape}s,
 * and draws them along the path to be drawn.
 * See http://jhlabs.com/java/java2d/strokes/
 */
public class ShapeStroke implements Stroke {
    private final Shape[] shapes;
    private final float advance;
    private final boolean repeat = true;
    private final AffineTransform t = new AffineTransform();
    private static final float FLATNESS = 1;

    public ShapeStroke(Shape shape, float advance) {
        this(new Shape[]{shape}, advance);
    }

    public ShapeStroke(Shape[] shapes, float advance) {
        this.advance = advance;
        this.shapes = new Shape[shapes.length];

        // Move each shape to be centered at the origin
        for (int i = 0; i < this.shapes.length; i++) {
            Rectangle2D bounds = shapes[i].getBounds2D();
            t.setToTranslation(-bounds.getCenterX(), -bounds.getCenterY());
            this.shapes[i] = t.createTransformedShape(shapes[i]);
        }
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        GeneralPath result = new GeneralPath();
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
        float[] points = new float[6];

        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int type = 0;
        float thresholdDist = 0;

        int shapeIndex = 0;
        int numShapes = shapes.length;

        while (shapeIndex < numShapes && !it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    result.moveTo(moveX, moveY);
                    thresholdDist = 0;
                    break;

                case SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                // fall through

                case SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    float dx = thisX - lastX;
                    float dy = thisY - lastY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance >= thresholdDist) {
                        float angle = (float) Math.atan2(dy, dx);
                        // handles segments which are long enough
                        // to require several shapes along their length
                        while (shapeIndex < numShapes && distance >= thresholdDist) {
                            float x = lastX + thresholdDist * dx / distance;
                            float y = lastY + thresholdDist * dy / distance;
                            t.setToTranslation(x, y);
                            t.rotate(angle);
                            result.append(t.createTransformedShape(shapes[shapeIndex]), false);
                            thresholdDist += advance;
                            shapeIndex++;
                            if (repeat) {
                                shapeIndex %= numShapes;
                            }
                        }
                    }
                    thresholdDist -= distance;
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }

        return result;
    }
}
