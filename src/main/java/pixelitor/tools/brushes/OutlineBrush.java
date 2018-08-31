/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.tools.brushes;

import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

public abstract class OutlineBrush extends StrokeBrush {
    private final OutlineBrushSettings settings;
    private double origRadius;
    private long prevTime;
    private static final double MIN_SPEED_THRESHOLD = 100;
    private static final double MAX_SPEED_THRESHOLD = 2000;
    private static final double THRESHOLD_DIFF = MAX_SPEED_THRESHOLD - MIN_SPEED_THRESHOLD;

    protected OutlineBrush(OutlineBrushSettings settings, double radius, int cap, int join) {
        super(radius, StrokeType.OUTLINE, cap, join);
        this.settings = settings;
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);
        origRadius = radius;
    }

    @Override
    public void startAt(PPoint p) {
        super.startAt(p);
        prevTime = System.nanoTime();
    }

    @Override
    public void continueTo(PPoint p) {
        if (settings.dependsOnSpeed()) {
            double dist = previous.coDist(p);

//            double scaledRadius = origRadius / Math.sqrt(dist);
//            double scaledRadius = origRadius + 1 - dist;
//            if (scaledRadius < 1) {
//                scaledRadius = 1;
//            }
            double scaledRadius;

            long timeNow = System.nanoTime();
            long timeDiff = timeNow - prevTime;
            if (timeDiff == 0) {
                // unlikely to occur, but check it in order to
                // make sure we dont't divide by zero
                return;
            }

            // pixels/second
            double speed = 1_000_000_000.0 * dist / timeDiff;

            if (speed < MIN_SPEED_THRESHOLD) {
                scaledRadius = origRadius;
            } else if (speed < MAX_SPEED_THRESHOLD) {
                // between the two thresholds, the radius decreases linearly
                double a = (1 - origRadius) / THRESHOLD_DIFF;
                double b = (MAX_SPEED_THRESHOLD * origRadius - MIN_SPEED_THRESHOLD) / THRESHOLD_DIFF;
                scaledRadius = a * speed + b;
            } else {
                scaledRadius = 1;
            }

            radius = scaledRadius;
            diameter = 2 * scaledRadius;

            currentStroke = createStroke((float) (2 * scaledRadius));
            prevTime = timeNow;
        }

        super.continueTo(p);
    }
}
