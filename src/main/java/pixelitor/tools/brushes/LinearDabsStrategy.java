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

public class LinearDabsStrategy implements DabsStrategy {
    private final DabsBrush brush;
    private double distanceFromLastDab = 0;
    private double spacingRatio = 2.0; // the spacing relative to the radius
    private final boolean angleAware;
    private final boolean refreshBrushForEachDab;

    private double prevX = 0;
    private double prevY = 0;

    private int radius;
//    private int diameter;

    public LinearDabsStrategy(DabsBrush brush, double spacingRatio, boolean angleAware, boolean refreshBrushForEachDab) {
        this.brush = brush;
        this.spacingRatio = spacingRatio;
        this.angleAware = angleAware;
        this.refreshBrushForEachDab = refreshBrushForEachDab;
    }

    @Override
    public void setRadius(int radius) {
        this.radius = radius;
//        this.diameter = 2 * radius;
    }

    @Override
    public void onDragStart(int x, int y) {
        brush.setupBrushStamp(x, y);
        distanceFromLastDab = 0; // moved from reset()

        prevX = x;
        prevY = y;
        if(angleAware) {
            // TODO why?? - check original implementation
            distanceFromLastDab = radius * spacingRatio * 0.8;
        } else {
            // for angle-aware brushes looks better if the first point is not drawn
            // because the correct angle cannot be calculated
            brush.putDab(x, y, 0);
        }
    }

    @Override
    public void onNewMousePoint(int endX, int endY) {
        double dx = endX - prevX;
        double dy = endY - prevY;
        double lineDistance = Math.sqrt(dx * dx + dy * dy);

        double spacing = radius * spacingRatio;
        double relativeSpacingDistance = spacing / lineDistance;
        double initialRelativeSpacingDistance = (spacing - distanceFromLastDab) / lineDistance;

        double theta = 0;
        if(angleAware) {
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
}
