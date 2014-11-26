/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.shapes;

import net.jafama.FastMath;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * A random star, inspired by http://tips4java.wordpress.com/2013/05/13/playing-with-shapes/
 */
public class RandomStar implements Shape {
    // Warning: don't extend Polygon, because bizarre things (the process starts
    // allocating all the memory, no matter what -Xmx says)
    // can happen when the shape is used for selections - probably some JDK bug
    // Things are OK with this delegate
    GeneralPath delegate = null;

    static Random random = new Random();

    static int numPoints;
    static int numRadius;
    static double[] radiusRatios;

    static double unitAngle;
    static double initialAngle;

    public static void randomize() {
        numPoints = 2*(4 + random.nextInt(6));
        numRadius = 2; // if higher than 2 then sometimes nice dancing starts are produced, but often ugly ones
        radiusRatios = new double[numRadius];
        radiusRatios[0] = 1.0;
        for (int i = 1; i < numRadius; i++) {
            radiusRatios[i] = 0.1 + random.nextDouble() / 2.5;
        }

        unitAngle = (2 * Math.PI) / numPoints;

        // a random value between 0 and unitAngle
        initialAngle = 2 * random.nextDouble() * unitAngle;
//        System.out.println(String.format("RandomStar::randomize: unitAngle = %.2f, initialAngle = %.2f", unitAngle, initialAngle));
    }

    static {
        randomize();
    }

    public RandomStar(int x, int y, int width, int height) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        int[] radii = new int[numRadius];
        int maxRadius = 1 + width / 2;

        radii[0] = maxRadius;
        for (int i = 1; i < radii.length; i++) {
            radii[i] = (int) (maxRadius * radiusRatios[i]);
        }
        double heightToWidthRatio = height / (double)width;

        for (int i = 0; i < numPoints; i++) {
            double angle = initialAngle + i * unitAngle;
            int radiusIndex = i % radii.length;
            int radius = radii[radiusIndex];
            double circleX = FastMath.cos(angle) * radius;
            double circleY = FastMath.sin(angle) * radius;
            double relX = centerX + circleX;
            double relY = centerY + heightToWidthRatio * circleY;

            if(delegate == null) {
                delegate = new GeneralPath();
                delegate.moveTo(relX, relY);
            } else {
                delegate.lineTo(relX, relY);
            }
        }
        delegate.closePath();

    }

    @Override
    public Rectangle getBounds() {
        return delegate.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return delegate.getBounds2D();
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return delegate.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return delegate.getPathIterator(at, flatness);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return delegate.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return delegate.intersects(r);
    }

    @Override
    public boolean contains(double x, double y) {
        return delegate.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return delegate.contains(p);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return delegate.contains(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return delegate.contains(x, y, w, h);
    }
}
