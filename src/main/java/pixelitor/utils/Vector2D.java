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

    public void add(float value) {
        this.x += value;
        this.y += value;
    }

    public void subtract(Vector2D v) {
        this.x -= v.x;
        this.y -= v.y;
    }

    public void setMagnitude(float magnitude) {
        normalize();
        scale(magnitude);
    }

    public void scale(float magnitude) {
        x *= magnitude;
        y *= magnitude;
    }

    private void normalize() {
        float factor = (float) hypot(x, y);
        x /= factor;
        y /= factor;
    }

    public void normalizeIfNonzero() {
        if (x == 0 && y == 0) return;
        float factor = (float) hypot(x, y);
        x /= factor;
        y /= factor;
    }

    public static Vector2D add(Vector2D... forces) {
        Vector2D res = new Vector2D();
        for (Vector2D force : forces) {
            res.add(force);
        }
        return res;
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
