/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * A dabs strategy that places dabs along the lines connecting
 * mouse events with uniform spacing between them.
 */
public class LinearDabsStrategy implements DabsStrategy {
    private final DabsBrush brush;
    private double distFromLastDab = 0;
    private Spacing spacing;
    private AngleSettings angleSettings;
    private final boolean refreshBrushForEachDab;
    private PPoint prev;

    public LinearDabsStrategy(DabsBrush brush,
                              Spacing spacing,
                              AngleSettings angleSettings,
                              boolean refreshBrushForEachDab) {
        this.brush = brush;
        this.spacing = spacing;
        this.angleSettings = angleSettings;
        this.refreshBrushForEachDab = refreshBrushForEachDab;
    }

    @Override
    public void onStrokeStart(PPoint startPoint) {
        distFromLastDab = 0; // moved from reset()

        prev = startPoint;
        if (angleSettings.isAngled()) {
            // Angled brushes don't draw a dab initially as there is no angle information yet.
            // Distance is artificially set to ensure a dab is drawn soon.
            distFromLastDab = spacing.getSpacing(brush.getRadius()) * 0.8;
        } else {
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
        double spacingDist = spacing.getSpacing(brush.getRadius());
        double initialRelativeSpacingDist = (spacingDist - distFromLastDab) / lineDist;

        double angle = 0;
        double dx = newX - prevX;
        double dy = newY - prevY;
        if (angleSettings.isAngled()) {
            angle = Math.atan2(dy, dx);
        }

        double x = prevX, y = prevY;
        boolean dabDrawn = false;

        int steps = 0;

        double relativeSpacingDist = spacingDist / lineDist;
        for (double t = initialRelativeSpacingDist; t < 1.0; t += relativeSpacingDist) {
            if (steps++ > 1_000 && RandomGUITest.isRunning()) {
                // crazy big shapes can appear during
                // random GUI testing, don't wait forever
                break;
            }
            x = prevX + t * dx;
            y = prevY + t * dy;
            PPoint dabPoint = PPoint.fromIm(x, y, newPoint.getView());

            if (refreshBrushForEachDab) {
                brush.initBrushStamp(dabPoint);
            }

            if (angleSettings.isJitterEnabled()) {
                angle = angleSettings.calcJitteredAngle(angle);
            }

            brush.putDab(dabPoint, angle);
            dabDrawn = true;
        }

        if (dabDrawn) {
            double remainingDx = newX - x;
            double remainingDy = newY - y;
            distFromLastDab = Math.sqrt(remainingDx * remainingDx
                + remainingDy * remainingDy);
        } else {
            distFromLastDab += lineDist;
        }

        prev = newPoint;
    }

    @Override
    public void settingsChanged() {
        DabsBrushSettings settings = brush.getSettings();
        angleSettings = settings.getAngleSettings();
        spacing = settings.getSpacingStrategy();
    }

    @Override
    public void setPrevious(PPoint previous) {
        prev = previous;
    }
}
