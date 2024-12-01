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

package pixelitor.tools.shapes.custom;

import net.jafama.FastMath;
import pixelitor.utils.CachedFloatRandom;
import pixelitor.utils.Rnd;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;

/**
 * A randomly generated star shape.
 */
public class RandomStarShape implements Shape {
    private static final CachedFloatRandom random = new CachedFloatRandom();

    private static int numPoints; // number of points in the star (must be even)
    private static int numRadiusVariants;
    private static double[] radiusRatios;

    private static double angleIncrement;
    private static double initialRotation;

    static {
        randomizeStarParameters();
    }

    private GeneralPath path = null;

    public static void randomizeStarParameters() {
        numPoints = 2 * (4 + Rnd.nextInt(6));
        numRadiusVariants = 2;
        radiusRatios = new double[numRadiusVariants];
        radiusRatios[0] = 1.0; // first radius is always full scale
        for (int i = 1; i < numRadiusVariants; i++) {
            // random scale between 0.1 and 0.5
            radiusRatios[i] = 0.1 + random.nextFloat() / 2.5;
        }

        angleIncrement = (2 * Math.PI) / numPoints;
        initialRotation = 2 * random.nextFloat() * angleIncrement;
    }

    public RandomStarShape(double x, double y, double width, double height) {
        double[] radii = new double[numRadiusVariants];
        double maxRadius = 1 + width / 2;

        radii[0] = maxRadius;
        for (int i = 1; i < radii.length; i++) {
            radii[i] = maxRadius * radiusRatios[i];
        }
        double heightToWidthRatio = height / width;

        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;

        for (int i = 0; i < numPoints; i++) {
            double angle = initialRotation + i * angleIncrement;
            int radiusIndex = i % radii.length;
            double radius = radii[radiusIndex];

            double offsetX = radius * FastMath.cos(angle);
            double offsetY = radius * FastMath.sin(angle);

            double pointX = centerX + offsetX;
            double pointY = centerY + heightToWidthRatio * offsetY;

            if (path == null) {
                path = new GeneralPath();
                path.moveTo(pointX, pointY);
            } else {
                path.lineTo(pointX, pointY);
            }
        }

        path.closePath();
    }

    @Override
    public Rectangle getBounds() {
        return path.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return path.getBounds2D();
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return path.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return path.getPathIterator(at, flatness);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return path.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return path.intersects(r);
    }

    @Override
    public boolean contains(double x, double y) {
        return path.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return path.contains(p);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return path.contains(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return path.contains(x, y, w, h);
    }
}
