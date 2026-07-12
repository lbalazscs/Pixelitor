/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import net.jafama.FastMath;

import java.awt.geom.Point2D;
import java.util.StringJoiner;

import static net.jafama.FastMath.hypot;

/**
 * Represents a mutable 2D vector.
 */
public class Vector2D {
    public double x;
    public double y;

    public Vector2D() {
    }

    public Vector2D(Vector2D source) {
        this(source.x, source.y);
    }

    public Vector2D(Point2D point) {
        this(point.getX(), point.getY());
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2D createUnitFromAngle(double angle) {
        return createFromPolar(angle, 1);
    }

    public static Vector2D createFromPolar(double angle, double length) {
        return new Vector2D(length * FastMath.cos(angle), length * FastMath.sin(angle));
    }

    public static Vector2D between(Point2D from, Point2D to) {
        return new Vector2D(to.getX() - from.getX(), to.getY() - from.getY());
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(Vector2D source) {
        this.x = source.x;
        this.y = source.y;
    }

    public void add(Vector2D other) {
        x += other.x;
        y += other.y;
    }

    public void add(double scalar) {
        this.x += scalar;
        this.y += scalar;
    }

    public void add(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    public void add(Point2D point) {
        x += point.getX();
        y += point.getY();
    }

    // translates the given point (not this vector!)
    public void translatePoint(Point2D point) {
        point.setLocation(point.getX() + x, point.getY() + y);
    }

    public void subtract(Vector2D other) {
        this.x -= other.x;
        this.y -= other.y;
    }

    public void subtract(double scalar) {
        this.x -= scalar;
        this.y -= scalar;
    }

    public void multiply(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
    }

    public void divide(double scalar) {
        assert scalar != 0;

        this.x /= scalar;
        this.y /= scalar;
    }

    public void setMagnitude(double magnitude) {
        double len = length();
        if (len != 0) {
            multiply(magnitude / len);
        } else {
            set(0, 0);
        }
    }

    public void normalizeQuick() {
        multiply(FastMath.invSqrtQuick(x * x + y * y));
    }

    public void normalizeIfNonZero() {
        setMagnitude(1);
    }

    /**
     * Avoids the varargs array allocation of {@link #add(Vector2D...)}
     * for the common case of summing exactly three vectors.
     */
    public static Vector2D add(Vector2D v1, Vector2D v2, Vector2D v3) {
        Vector2D res = new Vector2D();
        res.add(v1);
        res.add(v2);
        res.add(v3);
        return res;
    }

    public static Vector2D add(Vector2D... vectors) {
        Vector2D res = new Vector2D();
        for (Vector2D vector : vectors) {
            res.add(vector);
        }
        return res;
    }

    /**
     * Returns the 2D cross product of two vectors.
     * Its magnitude is the area of the parallelogram they span; its sign
     * indicates whether {@code b} is clockwise or counterclockwise from {@code a}.
     */
    public static double cross(Vector2D a, Vector2D b) {
        return a.x * b.y - a.y * b.x;
    }

    /**
     * Returns the dot product of two vectors. It's positive if they
     * point generally the same way, negative if they point in
     * opposite directions, and zero if they are perpendicular.
     */
    public static double dot(Vector2D a, Vector2D b) {
        return a.x * b.x + a.y * b.y;
    }

    public double lengthSq() {
        return x * x + y * y;
    }

    public double length() {
        return hypot(x, y);
    }

    public double distanceTo(Point2D point) {
        return hypot(point.getX() - x, point.getY() - y);
    }

    /**
     * Rotates this vector by 90 degrees clockwise (if y is pointing down).
     */
    public void rotateBy90Degrees() {
        double tmp = x;
        x = -y;
        y = tmp;
    }

    public Point2D toPoint() {
        return new Point2D.Double(x, y);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "Vector2D[", "]")
            .add("x=" + x)
            .add("y=" + y)
            .toString();
    }
}
