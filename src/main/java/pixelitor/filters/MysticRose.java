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

package pixelitor.filters;

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Mystic Rose
 */
public class MysticRose extends ShapeFilter {
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam nrPoints = new RangeParam("Number of Points", 3, 10, 42);
    private final RangeParam radius = new RangeParam("Radius (Height %)", 1, 45, 200);
    private final RangeParam rotate = new RangeParam("Rotate", 0, 0, 100);

    public MysticRose() {
        addParamsToFront(
                nrPoints,
                rotate,
                center,
                radius
        );
    }

    @Override
    protected Path2D createShape(int width, int height) {
        Point2D[] points = calcPoints(width, height);

        Path2D shape = new Path2D.Double();
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                if (i > j) { // draw only in one direction
                    shape.moveTo(points[i].getX(), points[i].getY());
                    shape.lineTo(points[j].getX(), points[j].getY());
                }
            }
        }
        return shape;
    }

    private Point2D[] calcPoints(int width, int height) {
        int numPoints = nrPoints.getValue();
        Point2D[] points = new Point2D[numPoints];
        double r = radius.getValueAsPercentage() * height;
        double startAngle = 2 * Math.PI / numPoints * rotate.getValueAsPercentage();
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        for (int i = 0; i < points.length; i++) {
            double theta = startAngle + 2 * Math.PI * i / numPoints;
            points[i] = new Point2D.Double((cx + r * Math.cos(theta)), (cy + r * Math.sin(theta)));
        }
        return points;
    }
}