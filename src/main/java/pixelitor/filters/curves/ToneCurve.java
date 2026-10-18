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
 * <p>
 * Point coordinates are defined from 0.0 to 1.0 on the x and y axes.
 * Any point that comes from user-space coordinates must
 * first be normalized to the curve coordinate space.
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurve {
    // maximum allowed number of knots (control points) on the curve
    private static final int MAX_KNOTS = 16;

    // radius of the drawn knot handles, in pixels
    private static final int KNOT_RADIUS_PX = 6;
    // radius for hit-testing an existing knot, in normalized coordinates
    private static final float KNOT_HOVER_RADIUS = 0.04f;
    // distance under which a click replaces a neighbor instead of adding a knot, in normalized coordinates
    private static final float KNOT_PROXIMITY_RADIUS = 0.08f;
    // distance beyond a neighboring knot to trigger deletion when dragging, in normalized coordinates
    private static final float DELETE_DRAG_MARGIN = 0.02f;

    // stroke styles for drawing the curve and control points
    private static final BasicStroke CURVE_STROKE = new BasicStroke(1);
    private static final BasicStroke KNOT_STROKE = new BasicStroke(2);

    public final Curve curveData = new Curve();
    private final Channel channel;
    private int[] plotTable;

    private boolean plotTableStale = true;

    public ToneCurve(Channel channel) {
        this.channel = channel;
    }

    /**
     * Resets the curve to a straight diagonal line from (0,0) to (1,1).
     */
    public void reset() {
        curveData.reset();
        plotTableStale = true;
    }

    public void randomize() {
        // the curve is assumed to be in its reset state

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        float x1 = (float) rnd.nextDouble(0.1, 0.4);
        float y1 = (float) rnd.nextDouble(0.1, 0.4);
        addKnot(x1, y1, false);

        float x2 = (float) rnd.nextDouble(0.6, 0.9);
        float y2 = (float) rnd.nextDouble(0.6, 0.9);
        addKnot(x2, y2, false);
    }

    /**
     * Precomputes the curve data for rendering,
     * if changes were made since the last render.
     */
    private void updateCurvePlotData() {
        if (plotTableStale) {
            plotTable = curveData.makeTable();
            plotTableStale = false;
        }
    }

    /**
     * Adds a new, normalized point (knot) to the curve at the given coordinates.
     * If replaceNearby is true, replaces a nearby knot instead of adding.
     * Returns the index of the added or replaced knot, or -1 if not added.
     */
    public int addKnot(float x, float y, boolean replaceNearby) {
        float clampedX = ImageMath.clamp01(x);
        float clampedY = ImageMath.clamp01(y);

        int knotCount = curveData.x.length;
        int insertionIndex = curveData.findInsertionIndex(clampedX);

        // prevent adding knots at the edges
        if (insertionIndex <= 0 || insertionIndex >= knotCount) {
            return -1;
        }

        if (replaceNearby) {
            // replace a nearby knot if it's too close
            int prevIndex = insertionIndex - 1;
            if (isClose(clampedX, clampedY, curveData.x[prevIndex], curveData.y[prevIndex])) {
                setKnotPosition(prevIndex, clampedX, clampedY);
                return prevIndex;
            } else if (isClose(clampedX, clampedY, curveData.x[insertionIndex], curveData.y[insertionIndex])) {
                setKnotPosition(insertionIndex, clampedX, clampedY);
                return insertionIndex;
            }
        }

        if (knotCount >= MAX_KNOTS) {
            return -1; // cannot add more knots
        }

        plotTableStale = true;
        return curveData.addKnot(clampedX, clampedY);
    }

    /**
     * Deletes a knot at the given index.
     */
    public void deleteKnot(int index) {
        // do not allow deleting the start and end knots
        if (index <= 0 || index >= curveData.x.length - 1) {
            return;
        }

        plotTableStale = true;
        curveData.removeKnot(index);
    }

    /**
     * Sets the position of an existing knot at the given index.
     *
     * @param index the knot index
     * @param x the new x-coordinate for the knot, normalized to the [0, 1] range
     * @param y the new y-coordinate for the knot, normalized to the [0, 1] range
     */
    public void setKnotPosition(int index, float x, float y) {
        // do not allow moving the start and end knots
        if (index <= 0 || index >= curveData.x.length - 1) {
            return;
        }

        curveData.x[index] = Math.clamp(x, curveData.x[index - 1], curveData.x[index + 1]);
        curveData.y[index] = ImageMath.clamp01(y);
        plotTableStale = true;
    }

    /**
     * Checks if a knot is dragged far enough outside the neighbors' x-range to delete.
     */
    public boolean isDraggedOutOfRange(int index, float x, float y) {
        if (index <= 0 || index >= curveData.x.length - 1) {
            return false;
        }

        return x > curveData.x[index + 1] + DELETE_DRAG_MARGIN
            || x < curveData.x[index - 1] - DELETE_DRAG_MARGIN;
    }

    /**
     * Checks whether a deleted knot can be restored
     * at the given index using the given mouse coordinates.
     *
     * @param index the knot index
     * @param x the normalized mouse x-coordinate to check
     * @param y the normalized mouse y-coordinate to check
     * @return true if the position is within the draggable range, false otherwise
     */
    public boolean canRestoreKnotAt(int index, float x, float y) {
        if (index <= 0 || index >= curveData.x.length) {
            return false;
        }

        return x < curveData.x[index] && x > curveData.x[index - 1];
    }

    private static boolean isWithinHoverRadius(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2) < KNOT_HOVER_RADIUS && Math.abs(y1 - y2) < KNOT_HOVER_RADIUS;
    }

    public boolean isOverKnot(Point2D.Float pos) {
        return getKnotIndexAt(pos) >= 0;
    }

    /**
     * Checks if the knot at a given index overlaps with any other knot.
     */
    public boolean overlapsOtherKnot(int index) {
        float x = curveData.x[index];
        float y = curveData.y[index];
        for (int i = 0; i < curveData.x.length; i++) {
            if (i != index && isWithinHoverRadius(x, y, curveData.x[i], curveData.y[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInsideCurveArea(Point2D.Float pos) {
        return pos.x >= 0 && pos.x <= 1 && pos.y >= 0 && pos.y <= 1;
    }

    public int getKnotIndexAt(Point2D.Float pos) {
        for (int i = 0; i < curveData.x.length; i++) {
            if (isWithinHoverRadius(pos.x, pos.y, curveData.x[i], curveData.y[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Draws the tone curve and, if active, its knots.
     */
    public void draw(Graphics2D g, boolean active, boolean darkTheme) {
        drawCurve(g, active, darkTheme);
        if (active) {
            drawKnots(g, darkTheme);
        }
    }

    /**
     * Draws the curve line based on precomputed plot data.
     */
    private void drawCurve(Graphics2D g, boolean active, boolean darkTheme) {
        updateCurvePlotData();
        Path2D path = new Path2D.Double();
        path.moveTo(0, (plotTable[0] / 255.0) * ToneCurves.CURVE_SIZE);
        for (int i = 1; i < plotTable.length; i++) {
            double x = (i / 255.0) * ToneCurves.CURVE_SIZE;
            double y = (plotTable[i] / 255.0) * ToneCurves.CURVE_SIZE;
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
        g.setStroke(KNOT_STROKE);
        int knotDiameter = 2 * KNOT_RADIUS_PX;
        for (int i = 0; i < curveData.x.length; i++) {
            g.drawOval(
                (int) (curveData.x[i] * ToneCurves.CURVE_SIZE) - KNOT_RADIUS_PX,
                (int) (curveData.y[i] * ToneCurves.CURVE_SIZE) - KNOT_RADIUS_PX,
                knotDiameter, knotDiameter);
        }
    }

    /**
     * Converts the curve data to a savable string format.
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
    public void setStateFrom(String savedState) {
        if (savedState == null || savedState.isEmpty()) {
            return;
        }

        String[] xyPairs = savedState.split("#");
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

        curveData.setKnots(newX, newY);
        plotTableStale = true;
    }

    /**
     * Checks if two points are close based on the proximity radius.
     */
    private static boolean isClose(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2) < KNOT_PROXIMITY_RADIUS
            && Math.abs(y1 - y2) < KNOT_PROXIMITY_RADIUS;
    }
}
