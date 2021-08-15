package pixelitor.utils;

import java.awt.geom.Point2D;

import static net.jafama.FastMath.hypot;

public class Vector2D {

    public float x;
    public float y;

    public Vector2D() {
    }

    public Vector2D(Vector2D v) {
        this(v.x, v.y);
    }

    public Vector2D(Point2D.Float p) {
        this(p.x, p.y);
    }

    public Vector2D(Point2D p) {
        this((float) p.getX(), (float) p.getY());
    }

    public Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(Vector2D v) {
        this.x = v.x;
        this.y = v.y;
    }

    public void add(Vector2D v) {
        this.x += v.x;
        this.y += v.y;
    }

    public void add(float v) {
        this.x += v;
        this.y += v;
    }

    public void subtract(Vector2D v) {
        this.x -= v.x;
        this.y -= v.y;
    }

    public void subtract(float v) {
        this.x -= v;
        this.y -= v;
    }

    public void multiply(Vector2D v) {
        this.x *= v.x;
        this.y *= v.y;
    }

    public void multiply(float v) {
        this.x *= v;
        this.y *= v;
    }

    public void divide(Vector2D v) {
        this.x /= v.x;
        this.y /= v.y;
    }

    public void divide(float v) {
        this.x /= v;
        this.y /= v;
    }

    public void setMagnitude(float magnitude) {
        normalize();
        multiply(magnitude);
    }

    private void normalize() {
        float factor = (float) hypot(x, y);
        divide(factor);
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

    public float lengthSq() {
        return x * x + y * y;
    }

    public float length() {
        return (float) hypot(x, y);
    }

    public void perpendicular() {
        float oy = y;
        y = x;
        x = -oy;
    }
}
