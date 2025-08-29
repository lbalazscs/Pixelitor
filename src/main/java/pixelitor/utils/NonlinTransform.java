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

package pixelitor.utils;

import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import static java.lang.Math.PI;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.sin;

/**
 * Nonlinear transformations that can be applied to points.
 */
public enum NonlinTransform {
    NONE("None", false) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            throw new UnsupportedOperationException();
        }
    }, INVERT("Circle Inversion", true) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            double circleRadius2 = (width * width + height * height) / 20.0;
            double horOffset = amount * width / 500.0;
            double cx = center.getX();
            double cy = center.getY();
            return (x, y) -> {
                x -= horOffset;
                Polar polar = toPolar(center, x, y);
                double invertedR;

                // the threshold of 1 ensures continuity in the
                // inverted radius function at the boundary
                if (polar.r() > 1) { // the normal case
                    invertedR = circleRadius2 / polar.r();
                } else {
                    // points that are too close to the center would be inverted
                    // to a very large distance, which can cause rendering issues
                    invertedR = circleRadius2;
                }

                // inverted point: same angle, but inverted distance
                double newX = cx + invertedR * cos(polar.angle());
                double newY = cy + invertedR * sin(polar.angle());
                return new Point2D.Double(newX, newY);
            };
        }
    }, SWIRL("Swirl", true) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            double cx = center.getX();
            double cy = center.getY();
            return (x, y) -> {
                Polar polar = toPolar(center, x, y);
                double newAngle = polar.angle() + amount * polar.r() / 20_000;

                double newX = cx + polar.r() * cos(newAngle);
                double newY = cy + polar.r() * sin(newAngle);
                return new Point2D.Double(newX, newY);
            };
        }
    }, BULGE("Pinch-Bulge", true) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            double maxR = Math.sqrt(width * width + height * height) / 2.0;
            double cx = center.getX();
            double cy = center.getY();
            return (x, y) -> {
                Polar polar = toPolar(center, x, y);
                double normalizedR = polar.r() / maxR;
                double newR = maxR * Math.pow(normalizedR, 1 - amount / 100);

                double newX = cx + newR * cos(polar.angle());
                double newY = cy + newR * sin(polar.angle());
                return new Point2D.Double(newX, newY);
            };
        }
    }, RECT_TO_POLAR("Rectangular to Polar", false) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            double maxR = Math.min(width, height) / 2.0;
            return (x, y) -> {
                double r = x * maxR / width;
                double angle = y * 2 * PI / height;

                double newX = center.getX() + r * cos(angle);
                double newY = center.getY() + r * sin(angle);
                return new Point2D.Double(newX, newY);
            };
        }
    }, WAVE("Wave", true) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            double cx = center.getX();
            double cy = center.getY();

            // make the effect size-independent
            double diagonal = Math.sqrt(width * width + height * height);
            double waveConstant = diagonal / 10.0;
            double adjustedAmount = amount * diagonal / 800;

            return (x, y) -> {
                double dx = x - cx;
                double dy = y - cy;

                double newX = x + adjustedAmount * sin(dy / waveConstant);
                double newY = y + adjustedAmount * cos(dx / waveConstant);

                return new Point2D.Double(newX, newY);
            };
        }
    }, VORTEX("Vortex", true) {
        @Override
        public PointMapper createMapper(Point2D center, double amount, int width, int height) {
            double cx = center.getX();
            double cy = center.getY();
            int numBranches = 5;

            // make the effect size-independent
            double diagonal = Math.sqrt(width * width + height * height);
            double divisor = diagonal / 30.0;
            double adjustedAmount = amount * diagonal / 2000;

            return (x, y) -> {
                Polar polar = toPolar(center, x, y);
                double displacement = adjustedAmount * sin(numBranches * polar.angle() + polar.r() / divisor);
                double newDist = polar.r() + displacement;

                double newX = cx + newDist * cos(polar.angle());
                double newY = cy + newDist * sin(polar.angle());

                return new Point2D.Double(newX, newY);
            };
        }
    };

    private record Polar(double r, double angle) {
    }

    private static Polar toPolar(Point2D center, double x, double y) {
        double cx = center.getX();
        double cy = center.getY();
        double r = center.distance(x, y);
        double angle = atan2(y - cy, x - cx);
        return new Polar(r, angle);
    }

    private final String displayName;

    // whether this transform can be tuned by changing an amount slider
    private final boolean hasAmount;

    NonlinTransform(String displayName, boolean hasAmount) {
        this.displayName = displayName;
        this.hasAmount = hasAmount;
    }

    /**
     * Transforms the given {@link Shape} into another
     * {@link Shape} using the given {@link PointMapper}.
     */
    private static Path2D transformShape(Shape shape, PointMapper mapper) {
        Path2D transformedShape = shape instanceof Path2D inputPath
            ? new Path2D.Double(inputPath.getWindingRule())
            : new Path2D.Double();

        double[] coords = new double[6];
        Point2D target;
        Point2D cp1;
        Point2D cp2;
        PathIterator pathIterator = shape.getPathIterator(null);
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);
            switch (type) {
                case SEG_MOVETO:
                    target = mapper.map(coords[0], coords[1]);
                    transformedShape.moveTo(target.getX(), target.getY());
                    break;
                case SEG_LINETO:
                    target = mapper.map(coords[0], coords[1]);
                    transformedShape.lineTo(target.getX(), target.getY());
                    break;
                case SEG_QUADTO:
                    cp1 = mapper.map(coords[0], coords[1]);
                    target = mapper.map(coords[2], coords[3]);
                    transformedShape.quadTo(cp1.getX(), cp1.getY(),
                        target.getX(), target.getY());
                    break;
                case SEG_CUBICTO:
                    cp1 = mapper.map(coords[0], coords[1]);
                    cp2 = mapper.map(coords[2], coords[3]);
                    target = mapper.map(coords[4], coords[5]);
                    transformedShape.curveTo(cp1.getX(), cp1.getY(),
                        cp2.getX(), cp2.getY(),
                        target.getX(), target.getY());
                    break;
                case SEG_CLOSE:
                    transformedShape.closePath();
                    break;
            }
            pathIterator.next();
        }
        return transformedShape;
    }

    public Path2D transform(Shape shape, Point2D pivotPoint, double amount, int width, int height) {
        PointMapper mapper = createMapper(pivotPoint, amount, width, height);
        return transformShape(shape, mapper);
    }

    /**
     * Creates a point mapper for this transformation.
     */
    public abstract PointMapper createMapper(Point2D center, double amount, int width, int height);

    public static EnumParam<NonlinTransform> asParam() {
        return new EnumParam<>("Distortion", NonlinTransform.class);
    }

    public static RangeParam createAmountParam() {
        return new RangeParam("Distortion Amount", -100, 0, 100);
    }

    public boolean hasAmount() {
        return hasAmount;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Maps a coordinate to another coordinate.
     */
    public interface PointMapper {
        Point2D map(double x, double y);
    }
}
