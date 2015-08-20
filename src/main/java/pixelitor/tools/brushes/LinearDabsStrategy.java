/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

/**
 * The simplest dabs strategy: it places the dabs along the lines
 * connecting the mouse events with an uniform spacing between them
 */
public class LinearDabsStrategy implements DabsStrategy {
    private final DabsBrush brush;
    private double distanceFromLastDab = 0;
    private SpacingStrategy spacingStrategy;
    private AngleSettings angleSettings;
    private final boolean refreshBrushForEachDab;

    private double prevX = 0;
    private double prevY = 0;

    public LinearDabsStrategy(DabsBrush brush, SpacingStrategy spacingStrategy, AngleSettings angleSettings, boolean refreshBrushForEachDab) {
        this.brush = brush;
        this.spacingStrategy = spacingStrategy;
        this.angleSettings = angleSettings;
        this.refreshBrushForEachDab = refreshBrushForEachDab;
    }

    @Override
    public void onDragStart(double x, double y) {
        brush.setupBrushStamp(x, y);
        distanceFromLastDab = 0; // moved from reset()

        prevX = x;
        prevY = y;
        if (angleSettings.isAngleAware()) {
            // For angle-aware brushes we don't draw a dab in this
            // method because we have no angle information.
            // However, we manipulate the distance from the last dab
            // so that a dab is drawn soon
            distanceFromLastDab = spacingStrategy.getSpacing(brush.getRadius()) * 0.8;
        } else {
            brush.putDab(x, y, 0);
        }
    }

    @Override
    public void onNewMousePoint(double endX, double endY) {
        double dx = endX - prevX;
        double dy = endY - prevY;
        double lineDistance = Math.sqrt(dx * dx + dy * dy);

        double spacing = spacingStrategy.getSpacing(brush.getRadius());
        double relativeSpacingDistance = spacing / lineDistance;
        double initialRelativeSpacingDistance = (spacing - distanceFromLastDab) / lineDistance;

        double theta = 0;
        if (angleSettings.isAngleAware()) {
            theta = Math.atan2(dy, dx);
        }

        double x = prevX, y = prevY;
        boolean drew = false;

        for(double t = initialRelativeSpacingDistance; t < 1.0; t += relativeSpacingDistance) {
            x = prevX + t * dx;
            y = prevY + t * dy;

            if(refreshBrushForEachDab) {
                brush.setupBrushStamp(x, y);
            }

            if (angleSettings.shouldJitterAngle()) {
                theta = angleSettings.calculatePerturbedAngle(theta);
            }

            brush.putDab(x, y, theta);
            drew = true;
        }

        if(drew) {
            double remainingDx = (endX - x);
            double remainingDy = (endY - y);
            distanceFromLastDab = Math.sqrt(remainingDx * remainingDx + remainingDy * remainingDy);
        } else {
            distanceFromLastDab += lineDistance;
        }

        prevX = endX;
        prevY = endY;
    }

    @Override
    public void settingsChanged() {
        DabsBrushSettings settings = brush.getSettings();
        angleSettings = settings.getAngleSettings();
        spacingStrategy = settings.getSpacingStrategy();
//        assert spacingStrategy.isValid();
    }
}
