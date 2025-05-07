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

package pixelitor.filters.curves;

import com.jhlabs.image.Curve;
import com.jhlabs.image.ImageMath;
import pixelitor.filters.levels.Channel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Represents a single tone curve for a color channel,
 * allowing points to be added, moved, or deleted.
 *
 * Point coordinates are defined from 0.0 to 1.0 on the x and y axes.
 * Any point that comes from user coordinates must be first
 * normalized to the curve coordinates space.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurve {
    // Maximum allowed number of control points on the curve
    private static final int MAX_CONTROL_POINTS = 16;

    // Radius for visual representation and hit detection
    private static final int KNOT_RADIUS_PIXELS = 6;
    private static final float KNOT_RADIUS = 0.04F;
    private static final float KNOT_DETECTION_RADIUS = 0.08F;

    public final Curve curveData = new Curve();
    private final Channel channel;
    private int width = 255;
    private int height = 255;
    private int[] curvePlotData;

    private boolean curveUpdated = true;
    private boolean active = false;

    // Stroke styles for drawing the curve and points
    private static final BasicStroke CURVE_STROKE = new BasicStroke(1);
    private static final BasicStroke POINT_STROKE = new BasicStroke(2);

    public ToneCurve(Channel channel) {
        this.channel = channel;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Resets the curve to a straight diagonal line from (0,0) to (1,1).
     */
    public void reset() {
        curveData.x = new float[]{0, 1};
        curveData.y = new float[]{0, 1};
        curveUpdated = true;
    }

    /**
     * Precomputes the curve data for rendering,
     * if changes were made since the last render.
     */
    private void updateCurvePlotData() {
        if (curveUpdated) {
            curveUpdated = false;
            curvePlotData = curveData.makeTable();
        }
    }

    /**
     * Adds a new, normalized point (knot) to the curve at the given position.
     * If allowReplace is true, replaces nearby points instead of adding.
     * Returns the index of the added or replaced knot, or -1 if not added.
     */
    public int addKnot(Point2D.Float p, boolean allowReplace) {
        // clamp to boundaries [0,1]
        clampToBoundary(p);

        int lastIndex = curveData.x.length - 1;
        int index = curveData.findKnotPos(p.x);

        // Prevent adding knots at the edges
        if (index <= 0 || index > lastIndex) {
            return -1;
        }

        // If allowReplace is true, replace a nearby knot if is's too close
        if (allowReplace) {
            int prevIndex = index - 1;
            if (isClose(p, new Point2D.Float(curveData.x[prevIndex], curveData.y[prevIndex]))) {
                setKnotPosition(prevIndex, p);
                return prevIndex;
            } else if (isClose(p, new Point2D.Float(curveData.x[index], curveData.y[index]))) {
                setKnotPosition(index, p);
                return index;
            }
        }

        if (curveData.x.length >= MAX_CONTROL_POINTS) {
            return -1;  // can't add because the limit is reached
        }

        curveUpdated = true;
        return curveData.addKnot(p.x, p.y); // adds the new knot
    }

    /**
     * Deletes a knot at the given index if within bounds.
     */
    public void deleteKnot(int index) {
        if (index < 0 || index > curveData.x.length - 1) {
            return;
        }

        if (curveData.x.length <= 2) {
            return;
        }

        curveUpdated = true;
        curveData.removeKnot(index);
    }

    /**
     * Sets the position of an existing knot at the given index.
     *
     * @param index the knot index
     * @param point the new position for the knot, normalized to [0,1] bounds
     */
    public void setKnotPosition(int index, Point2D.Float point) {
        int lastIndex = curveData.x.length - 1;

        if (index < 0 || index > lastIndex) {
            return;
        }

        // check prev/next index - knots can't change their index
        if (index > 0 && point.x < curveData.x[index - 1]) {
            point.x = curveData.x[index - 1];
        } else if (index < lastIndex && point.x > curveData.x[index + 1]) {
            point.x = curveData.x[index + 1];
        }

        curveData.x[index] = ImageMath.clamp01(point.x);
        curveData.y[index] = ImageMath.clamp01(point.y);
        curveUpdated = true;
    }

    /**
     * Checks if a point is within the draggable range of the given knot index.
     *
     * @param index the knot index
     * @param point the point to check
     * @return true if the point is out of range, false otherwise
     */
    public boolean isDraggedOutOfRange(int index, Point2D.Float point) {
        if (index <= 0 || index >= curveData.x.length - 1) {
            return false;
        }

        return point.x > curveData.x[index + 1] + 0.02f || point.x < curveData.x[index - 1] - 0.02f;
    }

    /**
     * Checks if the given point can be positioned at the given index.
     *
     * @param index the knot index
     * @param point the normalized point to check
     * @return true if the point is within the draggable range, false otherwise
     */
    public boolean isDraggedIn(int index, Point2D.Float point) {
        if (index <= 0 || index > curveData.x.length - 1) {
            return false;
        }

        return point.x < curveData.x[index] && point.x > curveData.x[index - 1];
    }

    private static boolean isOver(Point2D.Float p, Point2D.Float q) {
        if (Math.abs(p.x - q.x) < KNOT_RADIUS) {
            return Math.abs(p.y - q.y) < KNOT_RADIUS;
        }
        return false;
    }

    public boolean isOverKnot(Point2D.Float p) {
        return getKnotIndexAt(p) >= 0;
    }

    public boolean isOverKnot(int index) {
        var p = new Point2D.Float(curveData.x[index], curveData.y[index]);
        for (int i = 0; i < curveData.x.length; i++) {
            if (i != index && isOver(p, new Point2D.Float(curveData.x[i], curveData.y[i]))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isOverChart(Point2D.Float p) {
        return p.x >= 0 && p.x <= 1 && p.y >= 0 && p.y <= 1;
    }

    public int getKnotIndexAt(Point2D.Float p) {
        for (int i = 0; i < curveData.x.length; i++) {
            if (isOver(p, new Point2D.Float(curveData.x[i], curveData.y[i]))) {
                return i;
            }
        }

        return -1;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Draws the tone curve and, if active, the knots on the curve.
     */
    public void draw(Graphics2D g, boolean darkTheme) {
        drawCurve(g, darkTheme);
        if (active) {
            drawKnots(g, darkTheme);
        }
    }

    /**
     * Draws the curve based on precomputed curve plot data.
     */
    private void drawCurve(Graphics2D g, boolean darkTheme) {
        updateCurvePlotData();
        Path2D path = new Path2D.Double();
        path.moveTo(0, ((float) curvePlotData[0] / 255) * height);
        for (int i = 0; i < curvePlotData.length; i++) {
            double x = (i / 255.0) * width;
            double y = (curvePlotData[i] / 255.0) * height;
            path.lineTo(x, y);
        }

        g.setColor(channel.getDrawColor(active, darkTheme));
        g.setStroke(CURVE_STROKE);
        g.draw(path);
    }

    /**
     * Draws the knots on the curve if the curve is active,
     * highlighting control points.
     */
    private void drawKnots(Graphics2D g, boolean darkTheme) {
        g.setColor(darkTheme ? Color.WHITE : Color.BLACK);
        g.setStroke(POINT_STROKE);
        int knotDiameter = 2 * KNOT_RADIUS_PIXELS;
        for (int i = 0; i < curveData.x.length; i++) {
            g.drawOval(
                (int) (curveData.x[i] * width) - KNOT_RADIUS_PIXELS,
                (int) (curveData.y[i] * height) - KNOT_RADIUS_PIXELS,
                knotDiameter, knotDiameter);
        }
    }

    /**
     * Converts the curve data to a saveable string format.
     */
    public String toSaveString() {
        int numPoints = curveData.x.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numPoints; i++) {
            sb.append(curveData.x[i]);
            sb.append(",");
            sb.append(curveData.y[i]);
            if (i != numPoints - 1) {
                sb.append("#");
            }
        }
        return sb.toString();
    }

    /**
     * Restores the curve state from a previously saved string representation.
     */
    public void setStateFrom(String savedValue) {
        String[] xyPairs = savedValue.split("#");
        int numPoints = xyPairs.length;
        curveData.x = new float[numPoints];
        curveData.y = new float[numPoints];
        for (int i = 0; i < numPoints; i++) {
            String pair = xyPairs[i];
            int commaIndex = pair.indexOf(',');
            String pairX = pair.substring(0, commaIndex);
            String pairY = pair.substring(commaIndex + 1);
            curveData.x[i] = Float.parseFloat(pairX);
            curveData.y[i] = Float.parseFloat(pairY);
        }
        curveUpdated = true;
    }

    /**
     * Clamps a point to within the [0,1] bounds for the curve.
     */
    private static void clampToBoundary(Point2D.Float p) {
        p.x = ImageMath.clamp01(p.x);
        p.y = ImageMath.clamp01(p.y);
    }

    /**
     * Checks if two points are close enough based on defined detection radius.
     */
    private static boolean isClose(Point2D p, Point2D q) {
        return Math.abs(p.getX() - q.getX()) < KNOT_DETECTION_RADIUS
            && Math.abs(p.getY() - q.getY()) < KNOT_DETECTION_RADIUS;
    }
}