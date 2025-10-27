/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.BrushType;
import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.util.PPoint;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_BEVEL;
import static java.awt.BasicStroke.JOIN_ROUND;

public class OutlineBrush extends StrokeBrush {
    private final BrushType brushType;
    private final OutlineBrushSettings settings;
    private double origRadius;
    private long prevTime;
    private static final double MIN_SPEED_THRESHOLD = 100;
    private static final double MAX_SPEED_THRESHOLD = 2000;
    private static final double THRESHOLD_DIFF = MAX_SPEED_THRESHOLD - MIN_SPEED_THRESHOLD;

    public OutlineBrush(BrushType brushType, double radius, OutlineBrushSettings settings) {
        super(radius, StrokeType.OUTLINE,
            // can't be easiy generalized to arbitrary shape types because these
            // cap/join settings are crucial for the look of the outline brush
            brushType == BrushType.OUTLINE_SQUARE ? CAP_SQUARE : CAP_ROUND,
            brushType == BrushType.OUTLINE_SQUARE ? JOIN_BEVEL : JOIN_ROUND);
        this.brushType = brushType;
        this.settings = settings;
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);
        origRadius = radius;
    }

    @Override
    public void startStrokeAt(PPoint p) {
        super.startStrokeAt(p);
        prevTime = System.nanoTime();
    }

    @Override
    public void drawStartShape(PPoint p) {
        double x = p.getImX();
        double y = p.getImY();
        Stroke savedStroke = targetG.getStroke();

        targetG.setStroke(StrokeType.OUTLINE.getInnerStroke());

        //noinspection EnumSwitchStatementWhichMissesCases
        Shape shape = switch (brushType) {
            case OUTLINE_CIRCLE -> new Ellipse2D.Double(x - radius, y - radius, diameter, diameter);
            case OUTLINE_SQUARE -> new Rectangle2D.Double(x - radius, y - radius, diameter, diameter);
            default -> throw new IllegalStateException();
        };
        targetG.draw(shape);

        targetG.setStroke(savedStroke);
    }

    @Override
    public void continueTo(PPoint p) {
        if (settings.dependsOnSpeed()) {
            double dist = previous.coDist(p);

            long timeNow = System.nanoTime();
            long timeDiff = timeNow - prevTime;
            if (timeDiff == 0) {
                // unlikely to happen, but check it in order to
                // make sure we don't divide by zero
                return;
            }

            radius = calcScaledRadius(dist, timeDiff);

            // don't set the diameter according to the scale
            // because the brush outline still has the full size,
            // and the repaint region is based on the diameter
//            diameter = 2 * scaledRadius;

            currentStroke = createStroke((float) (2 * radius));
            prevTime = timeNow;
        }

        super.continueTo(p);
    }

    private double calcScaledRadius(double dist, long timeDiff) {
        // pixels/second
        double speed = 1_000_000_000.0 * dist / timeDiff;

        double scaledRadius;
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
        return scaledRadius;
    }

    @Override
    public double getMaxEffectiveRadius() {
        // use the maximum radius for the undo, even if the
        // speed-dependent drawing radius is smaller, see issue #57
        // A +1 safety is also necessary (rounding errors?)
        return brushType == BrushType.OUTLINE_CIRCLE
            ? origRadius + 1
            : (origRadius * 1.42) + 1; // multiply with sqrt 2
    }
}
