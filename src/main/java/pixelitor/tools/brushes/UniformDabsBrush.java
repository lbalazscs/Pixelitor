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

import java.awt.Graphics2D;

/**
 * A brush which implements drawLine by calling drawPoint at uniform distances
 */
public abstract class UniformDabsBrush implements AngledBrush {
    private double distanceFromLastDab = 0;
    private double spacingRatio = 2.0; // the spacing relative to the radius
    protected boolean angleAware;

    // used only for angle-aware drawings
    private double prevX = 0;
    private double prevY = 0;

    protected UniformDabsBrush(double spacingRatio, boolean angleAware) {
        this.spacingRatio = spacingRatio;
        this.angleAware = angleAware;
    }

    /**
     * Overridden methods should always call super.drawPoint(g, x, y, radius);
     */
    @Override
    public void drawPoint(Graphics2D g, int x, int y, int radius) {
        prevX = x;
        prevY = y;
        if(angleAware) {
            distanceFromLastDab = radius * spacingRatio * 0.8;
        }
    }

    @Override
    public void drawLine(Graphics2D g, int startX, int startY, int endX, int endY, int radius) {
        int diameter = 2 * radius;
        setupBrushStamp(g, diameter);
        int dx = endX - startX;
        int dy = endY - startY;
        double lineDistance =  Math.sqrt(dx * dx + dy * dy);

        double spacing = diameter * spacingRatio / 2.0;
        double relativeSpacingDistance = spacing / lineDistance;
        double initialRelativeSpacingDistance = (spacing - distanceFromLastDab) / lineDistance;

        double x = startX, y = startY;
        boolean drew = false;

        for(double t = initialRelativeSpacingDistance; t < 1.0; t += relativeSpacingDistance) {
            x = startX + t * dx;
            y = startY + t * dy;

            if(angleAware) {
//                System.out.println(String.format("UniformDabsBrush::drawLine: y = %.2f, prevY = %.2f, x = %.2f, prevX = %.2f", y, prevY, x, prevX));

                double theta = Math.atan2(y - prevY, x - prevX);
                drawPointWithAngle(g, (int) x, (int) y, radius, theta);
                prevX = x;
                prevY = y;
            } else {
                drawPoint(g, (int)x, (int)y, radius);
            }
            drew = true;
        }

        if(drew) {
            double remainingDx = (endX - x);
            double remainingDy = (endY - y);
            distanceFromLastDab = Math.sqrt(remainingDx * remainingDx + remainingDy * remainingDy);
        } else {
            distanceFromLastDab += lineDistance;
        }
    }



    /**
     * Called once before each line. An opportunity to setup things (color, image, angle)
     * that will not change during the line, in order to improve performance
     */
    abstract void setupBrushStamp(Graphics2D g, float diameter);

    @Override
    public void reset() {
        distanceFromLastDab = 0;
    }
}
