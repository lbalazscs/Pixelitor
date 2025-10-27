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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.test.RandomGUITest;

/**
 * A {@link DabsStrategy} that places dabs at uniform intervals along
 * straight line segments connecting the input points of a brush stroke.
 */
public class LinearDabsStrategy implements DabsStrategy {
    // Determines how soon the first dab is placed on the first segment
    // for directional brushes. A value of 0.8 means the first dab will
    // be placed after the user moves 20% of the required spacing distance.
    private static final double FIRST_DAB_DELAY_FACTOR = 0.8;

    private final DabsBrush brush;

    // distance accumulated since the last dab was placed
    private double accumulatedDist = 0;

    private Spacing spacing;
    private RotationSettings rotationSettings;
    private final boolean refreshBrushForEachDab;
    private PPoint prev; // the last processed input point

    public LinearDabsStrategy(DabsBrush brush,
                              Spacing spacing,
                              RotationSettings rotationSettings,
                              boolean refreshBrushForEachDab) {
        this.brush = brush;
        this.spacing = spacing;
        this.rotationSettings = rotationSettings;
        this.refreshBrushForEachDab = refreshBrushForEachDab;
    }

    @Override
    public void onStrokeStart(PPoint startPoint) {
        // reset accumulated distance for the new stroke
        accumulatedDist = 0;
        prev = startPoint;

        if (rotationSettings.isDirectional()) {
            // Don't place the first dab immediately, as we have no direction info yet.
            // Distance is artificially set to ensure a dab is placed soon.
            accumulatedDist = spacing.getSpacing(brush.getRadius()) * FIRST_DAB_DELAY_FACTOR;
        } else {
            // non-directional brushes can place the first dab right away
            brush.putDab(startPoint, 0);
        }
    }

    @Override
    public void onNewStrokePoint(PPoint newPoint) {
        double newX = newPoint.getImX();
        double newY = newPoint.getImY();
        double prevX = prev.getImX();
        double prevY = prev.getImY();

        double lineDist = newPoint.imDist(prev);
        if (lineDist < 1.0e-6) {
            return;
        }

        double spacingDist = spacing.getSpacing(brush.getRadius());
        double tForFirstDab = (spacingDist - accumulatedDist) / lineDist;

        double dx = newX - prevX;
        double dy = newY - prevY;
        // the angle of the current line segment
        double baseAngle = rotationSettings.isDirectional() ? Math.atan2(dy, dx) : 0;

        // track the position of the last placed dab on this segment
        double lastDabX = prevX;
        double lastDabY = prevY;
        boolean dabPlacedOnSegment = false;

        int steps = 0;

        double tStep = spacingDist / lineDist;
        for (double t = tForFirstDab; t < 1.0; t += tStep) {
            if (steps++ > 1_000 && RandomGUITest.isRunning()) {
                // crazy big shapes can appear during
                // random GUI testing, don't wait forever
                break;
            }

            // calculate coordinates for the current dab
            double dabX = prevX + t * dx;
            double dabY = prevY + t * dy;
            PPoint dabPoint = PPoint.lazyFromIm(dabX, dabY, newPoint.getView());

            if (refreshBrushForEachDab) {
                // allow brush stamp to update based on the specific dab location
                brush.initBrushStamp(dabPoint);
            }

            double dabAngle = rotationSettings.jitterAngle(baseAngle);
            brush.putDab(dabPoint, dabAngle);

            lastDabX = dabX;
            lastDabY = dabY;
            dabPlacedOnSegment = true;
        }

        // update the accumulated distance for the next segment
        if (dabPlacedOnSegment) {
            double remainingDx = newX - lastDabX;
            double remainingDy = newY - lastDabY;
            accumulatedDist = Math.sqrt(remainingDx * remainingDx
                + remainingDy * remainingDy);
        } else {
            accumulatedDist += lineDist;
        }

        prev = newPoint;
    }

    @Override
    public void settingsChanged() {
        // refresh local references to settings from the brush
        DabsBrushSettings settings = brush.getSettings();
        rotationSettings = settings.getAngleSettings();
        spacing = settings.getSpacingStrategy();
    }

    @Override
    public void setPrevious(PPoint previous) {
        prev = previous;
    }
}
