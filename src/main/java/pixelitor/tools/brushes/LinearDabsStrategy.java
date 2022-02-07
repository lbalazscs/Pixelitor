/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * The simplest dabs strategy: it places the dabs along the lines
 * connecting the mouse events with a uniform spacing between them
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
    public void onStrokeStart(PPoint p) {
        distFromLastDab = 0; // moved from reset()

        prev = p;
        if (angleSettings.isAngleAware()) {
            // No dab is drawn for the angle-aware brushes in this method,
            // because there is no angle information yet.
            // However, the distance from the last dab is set
            // artificially, so that a dab is drawn soon.
            distFromLastDab = spacing.getSpacing(brush.getRadius()) * 0.8;
        } else {
            brush.putDab(p, 0);
        }
    }

    @Override
    public void onNewStrokePoint(PPoint end) {
        double endX = end.getImX();
        double endY = end.getImY();
        double prevX = prev.getImX();
        double prevY = prev.getImY();

        double lineDist = end.imDist(prev);
        double spacingDist = spacing.getSpacing(brush.getRadius());
        double initialRelativeSpacingDist = (spacingDist - distFromLastDab) / lineDist;

        double theta = 0;
        double dx = endX - prevX;
        double dy = endY - prevY;
        if (angleSettings.isAngleAware()) {
            theta = Math.atan2(dy, dx);
        }

        double x = prevX, y = prevY;
        boolean drew = false;

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
            PPoint p = PPoint.eagerFromIm(x, y, end.getView());

            if (refreshBrushForEachDab) {
                brush.setupBrushStamp(p);
            }

            if (angleSettings.shouldJitterAngle()) {
                theta = angleSettings.calcJitteredAngle(theta);
            }

            // TODO perhaps this could be optimized if instead of putDab
            // we called a special version that does not update the region
            // and then we updated the region at the end
            brush.putDab(p, theta);
            drew = true;
        }

        if (drew) {
            double remainingDx = endX - x;
            double remainingDy = endY - y;
            distFromLastDab = Math.sqrt(remainingDx * remainingDx
                + remainingDy * remainingDy);
        } else {
            distFromLastDab += lineDist;
        }

        prev = end;
    }

    @Override
    public void settingsChanged() {
        DabsBrushSettings settings = brush.getSettings();
        angleSettings = settings.getAngleSettings();
        spacing = settings.getSpacingStrategy();
    }

    @Override
    public void rememberPrevious(PPoint previous) {
        prev = previous;
    }
}
