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
import pixelitor.filters.util.Channel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.concurrent.ThreadLocalRandom;

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
    // maximum allowed number of control points on the curve
    private static final int MAX_CONTROL_POINTS = 16;

    // radius for visual representation and hit detection of knots
    private static final int KNOT_RADIUS_PIXELS = 6;
    // radius for knot hover detection, in normalized coordinates
    private static final float KNOT_HOVER_RADIUS = 0.04f;
    // radius for detecting proximity to an existing knot, in normalized coordinates
    private static final float KNOT_PROXIMITY_RADIUS = 0.08f;

    public final Curve curveData = new Curve();
    private final Channel channel;
    private int width = 255;
    private int height = 255;
    private int[] curvePlotData;

    private boolean curveUpdated = true;
    private boolean active = false;

    // stroke styles for drawing the curve and points
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

    public void randomize() {
        // the curve is assumed to be in its reset state

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        float x1 = (float) rnd.nextDouble(0.1, 0.4);
        float y1 = (float) rnd.nextDouble(0.1, 0.4);
        addKnot(new Point2D.Float(x1, y1), false);

        float x2 = (float) rnd.nextDouble(0.6, 0.9);
        float y2 = (float) rnd.nextDouble(0.6, 0.9);
        addKnot(new Point2D.Float(x2, y2), false);
    }

    /**
     * Precomputes the curve data for rendering,
     * if changes were made since the last render.
     */
    private void updateCurvePlotData() {
        if (curveUpdated) {
            curvePlotData = curveData.makeTable();
            curveUpdated = false;
        }
    }

    /**
     * Adds a new, normalized point (knot) to the curve at the given position.
     * If allowReplace is true, replaces nearby points instead of adding.
     * Returns the index of the added or replaced knot, or -1 if not added.
     */
    public int addKnot(Point2D.Float p, boolean allowReplace) {
        clampToBoundary(p);

        int knotCount = curveData.x.length;
        int insertionIndex = curveData.findKnotPos(p.x);

        // prevent adding knots at the edges
        if (insertionIndex <= 0 || insertionIndex >= knotCount) {
            return -1;
        }

        // if allowReplace is true, replace a nearby knot if it's too close
        if (allowReplace) {
            int prevIndex = insertionIndex - 1;
            if (isClose(p, curveData.x[prevIndex], curveData.y[prevIndex])) {
                setKnotPosition(prevIndex, p);
                return prevIndex;
            } else if (isClose(p, curveData.x[insertionIndex], curveData.y[insertionIndex])) {
                setKnotPosition(insertionIndex, p);
                return insertionIndex;
            }
        }

        if (knotCount >= MAX_CONTROL_POINTS) {
            return -1; // cannot add more knots
        }

        curveUpdated = true;
        return curveData.addKnot(p.x, p.y);
    }

    /**
     * Deletes a knot at the given index.
     */
    public void deleteKnot(int index) {
        // do not allow deleting the start and end knots
        if (index <= 0 || index >= curveData.x.length - 1) {
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
        // do not allow moving the start and end knots
        if (index <= 0 || index >= curveData.x.length - 1) {
            return;
        }

        // prevent knots from crossing over each other on the x-axis
        if (point.x < curveData.x[index - 1]) {
            point.x = curveData.x[index - 1];
        } else if (point.x > curveData.x[index + 1]) {
            point.x = curveData.x[index + 1];
        }

        curveData.x[index] = ImageMath.clamp01(point.x);
        curveData.y[index] = ImageMath.clamp01(point.y);
        curveUpdated = true;
    }

    /**
     * Checks if a knot is dragged far enough to be deleted.
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
    public boolean canBeDraggedIn(int index, Point2D.Float point) {
        if (index <= 0 || index >= curveData.x.length) {
            return false;
        }

        return point.x < curveData.x[index] && point.x > curveData.x[index - 1];
    }

    private static boolean isOver(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2) < KNOT_HOVER_RADIUS && Math.abs(y1 - y2) < KNOT_HOVER_RADIUS;
    }

    public boolean isOverKnot(Point2D.Float p) {
        return getKnotIndexAt(p) >= 0;
    }

    /**
     * Checks if the knot at a given index overlaps with any other knot.
     */
    public boolean isOverKnot(int index) {
        float x = curveData.x[index];
        float y = curveData.y[index];
        for (int i = 0; i < curveData.x.length; i++) {
            if (i != index && isOver(x, y, curveData.x[i], curveData.y[i])) {
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
            if (isOver(p.x, p.y, curveData.x[i], curveData.y[i])) {
                return i;
            }
        }
        return -1;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Draws the tone curve and, if active, its knots.
     */
    public void draw(Graphics2D g, boolean darkTheme) {
        drawCurve(g, darkTheme);
        if (active) {
            drawKnots(g, darkTheme);
        }
    }

    /**
     * Draws the curve line based on precomputed plot data.
     */
    private void drawCurve(Graphics2D g, boolean darkTheme) {
        updateCurvePlotData();
        Path2D path = new Path2D.Double();
        path.moveTo(0, (curvePlotData[0] / 255.0) * height);
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
     * Draws the circular handles for the knots on the curve.
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
            if (i < numPoints - 1) {
                sb.append("#");
            }
        }
        return sb.toString();
    }

    /**
     * Restores the curve state from a saved string representation.
     */
    public void setStateFrom(String savedValue) {
        if (savedValue == null || savedValue.isEmpty()) {
            return;
        }

        String[] xyPairs = savedValue.split("#");
        int numPoints = xyPairs.length;
        if (numPoints == 0) {
            return;
        }

        float[] newX = new float[numPoints];
        float[] newY = new float[numPoints];

        for (int i = 0; i < numPoints; i++) {
            String[] pair = xyPairs[i].split(",");
            if (pair.length != 2) {
                return; // malformed data, abort
            }
            try {
                newX[i] = Float.parseFloat(pair[0]);
                newY[i] = Float.parseFloat(pair[1]);
            } catch (NumberFormatException e) {
                return; // malformed data, abort
            }
        }

        curveData.x = newX;
        curveData.y = newY;
        curveUpdated = true;
    }

    /**
     * Clamps a point's coordinates to the [0,1] range.
     */
    private static void clampToBoundary(Point2D.Float p) {
        p.x = ImageMath.clamp01(p.x);
        p.y = ImageMath.clamp01(p.y);
    }

    /**
     * Checks if two points are close based on the proximity radius.
     */
    private static boolean isClose(Point2D.Float p, float qx, float qy) {
        return Math.abs(p.x - qx) < KNOT_PROXIMITY_RADIUS
            && Math.abs(p.y - qy) < KNOT_PROXIMITY_RADIUS;
    }
}