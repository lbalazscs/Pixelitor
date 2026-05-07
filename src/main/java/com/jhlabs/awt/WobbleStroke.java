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

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;

public class WobbleStroke implements Stroke {
    private static final float FLATNESS = 1;

    private final float detail;
    private final float amplitude;
    private final Random rand;
    private final float basicStrokeWidth;

    public WobbleStroke(float detail, float amplitude, float basicStrokeWidth) {
        this.detail = detail;
        this.amplitude = amplitude;
        this.basicStrokeWidth = basicStrokeWidth;

        //noinspection SharedThreadLocalRandom
        rand = ThreadLocalRandom.current();
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        GeneralPath result = new GeneralPath();
        shape = new BasicStroke(basicStrokeWidth).createStrokedShape(shape);
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
        float[] points = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float currentX, currentY;
        int type;
        float next = 0;

        while (!it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case SEG_MOVETO:
                    moveX = lastX = applyJitter(points[0]);
                    moveY = lastY = applyJitter(points[1]);
                    result.moveTo(moveX, moveY);
                    next = 0;
                    break;

                case SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // fall through

                case SEG_LINETO:
                    currentX = applyJitter(points[0]);
                    currentY = applyJitter(points[1]);
                    float dx = currentX - lastX;
                    float dy = currentY - lastY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance >= next) {
                        float r = 1.0f / distance;
                        while (distance >= next) {
                            float x = lastX + next * dx * r;
                            float y = lastY + next * dy * r;
                            result.lineTo(applyJitter(x), applyJitter(y));
                            next += detail;
                        }
                    }
                    next -= distance;
                    lastX = currentX;
                    lastY = currentY;
                    break;
            }
            it.next();
        }

        return result;
    }

    private float applyJitter(float x) {
        float delta = 2 * (amplitude * (rand.nextFloat() - 0.5f));
        return x + delta;
    }
}
