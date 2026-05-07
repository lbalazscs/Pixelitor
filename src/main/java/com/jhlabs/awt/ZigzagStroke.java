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
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;

/**
 * A Stroke implementation that modifies a base stroke by applying
 * a continuous zigzag pattern with a given amplitude and wavelength.
 */
public class ZigzagStroke implements Stroke {
    private final float amplitude;
    private final float wavelength;
    private final Stroke baseStroke;

    private static final float FLATNESS = 1;

    public ZigzagStroke(Stroke baseStroke, float amplitude, float wavelength) {
        this.baseStroke = baseStroke;
        this.amplitude = amplitude;
        this.wavelength = wavelength;
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        GeneralPath result = new GeneralPath();
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
        float[] points = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float currentX, currentY;
        int type;
        float next = 0;
        int phase = 0;

        while (!it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    result.moveTo(moveX, moveY);
                    next = wavelength / 2;
                    break;

                case SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // fall through

                case SEG_LINETO:
                    currentX = points[0];
                    currentY = points[1];
                    float dx = currentX - lastX;
                    float dy = currentY - lastY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance >= next) {
                        float r = 1.0f / distance;
                        while (distance >= next) {
                            float x = lastX + next * dx * r;
                            float y = lastY + next * dy * r;
                            if ((phase & 1) == 0) {
                                result.lineTo(x + amplitude * dy * r, y - amplitude * dx * r);
                            } else {
                                result.lineTo(x - amplitude * dy * r, y + amplitude * dx * r);
                            }
                            next += wavelength;
                            phase++;
                        }
                    }
                    next -= distance;
                    lastX = currentX;
                    lastY = currentY;
                    if (type == SEG_CLOSE) {
                        result.closePath();
                    }
                    break;
            }
            it.next();
        }

        return baseStroke.createStrokedShape(result);
    }
}
