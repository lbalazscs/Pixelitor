/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

public class Vector2D {
    public double x;
    public double y;

    public Vector2D() {
    }

    public Vector2D(Vector2D v) {
        this(v.x, v.y);
    }

    public Vector2D(Point2D p) {
        this(p.getX(), p.getY());
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2D createUnitVector(double angle) {
        return new Vector2D(FastMath.cos(angle), FastMath.sin(angle));
    }

    public static Vector2D createFromPolar(double angle, double length) {
        return new Vector2D(length * FastMath.cos(angle), length * FastMath.sin(angle));
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(Vector2D v) {
        this.x = v.x;
        this.y = v.y;
    }

    public void add(Vector2D other) {
        x += other.x;
        y += other.y;
    }

    public void add(float v) {
        this.x += v;
        this.y += v;
    }

    public void add(Point2D point) {
        x += point.getX();
        y += point.getY();
    }

    public void subtract(Vector2D v) {
        this.x -= v.x;
        this.y -= v.y;
    }

    public void subtract(double v) {
        this.x -= v;
        this.y -= v;
    }

    public void multiply(Vector2D v) {
        this.x *= v.x;
        this.y *= v.y;
    }

    public void multiply(double v) {
        this.x *= v;
        this.y *= v;
    }

    public void divide(double v) {
        this.x /= v;
        this.y /= v;
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

    public void normalizeIfNonzero() {
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

    public double distance(Point2D point) {
        return hypot(point.getX() - x, point.getY() - y);
    }

    public void perpendicular() {
        double oy = y;
        y = x;
        x = -oy;
    }

    public Point2D asPoint() {
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
