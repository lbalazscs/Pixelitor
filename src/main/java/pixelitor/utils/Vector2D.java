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
        return new Vector2D(FastMath.cos(angle), FastMath.sin(angle));
    }

    public static Vector2D createFromPolar(double angle, double length) {
        return new Vector2D(length * FastMath.cos(angle), length * FastMath.sin(angle));
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

    public void add(float scalar) {
        this.x += scalar;
        this.y += scalar;
    }

    public void add(Point2D point) {
        x += point.getX();
        y += point.getY();
    }

    public void subtract(Vector2D other) {
        this.x -= other.x;
        this.y -= other.y;
    }

    public void subtract(double scalar) {
        this.x -= scalar;
        this.y -= scalar;
    }

    public void multiply(Vector2D other) {
        this.x *= other.x;
        this.y *= other.y;
    }

    public void multiply(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
    }

    public void divide(double scalar) {
        this.x /= scalar;
        this.y /= scalar;
    }

    public void setMagnitude(double magnitude) {
        normalize();
        multiply(magnitude);
    }

    public void setMagnitudeOfUnitVector(double magnitude) {
        multiply(magnitude);
    }

    private void normalize() {
        divide(length());
    }

    private void normalizeQuick() {
        multiply(FastMath.invSqrtQuick(x * x + y * y));
    }

    public void normalizeIfNonZero() {
        if (x == 0 && y == 0) {
            return;
        }
        normalize();
    }

    public static Vector2D add(Vector2D... vectors) {
        Vector2D res = new Vector2D();
        for (Vector2D vector : vectors) {
            res.add(vector);
        }
        return res;
    }

    public static double cross(Vector2D a, Vector2D b) {
        return a.x * b.y - a.y * b.x;
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

    public void rotateBy90Degrees() {
        double tmp = y;
        y = x;
        x = -tmp;
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
