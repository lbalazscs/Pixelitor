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

package pixelitor.filters.curves;

import com.jhlabs.image.Curve;
import com.jhlabs.image.ImageMath;
import pixelitor.filters.levels.Channel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Represents single tone curve
 * Points coordinates are defined from 0.0F to 1.0F on x/y axis
 * Any point (that comes from user coordinates space) must be first normalized
 * by width, height to curve coordinates space
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurve {
    private static final int MAX_KNOTS = 16;
    private static final int KNOT_RADIUS_PX = 6;
    private static final float KNOT_RADIUS = 0.04F;
    private static final float NEARBY_RADIUS = 0.08F;
    public final Curve curve = new Curve();
    private final Channel channel;
    private int width = 255;
    private int height = 255;
    private int[] curvePlotData;
    private boolean dirty = true;
    private boolean active = false;
    private final BasicStroke curveStroke = new BasicStroke(1);
    private final BasicStroke pointStroke = new BasicStroke(2);

    public ToneCurve(Channel channel) {
        this.channel = channel;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void reset() {
        curve.x = new float[]{0, 1};
        curve.y = new float[]{0, 1};
        dirty = true;
    }

    private void initCurvePlotData() {
        if (dirty) {
            dirty = false;
            curvePlotData = curve.makeTable();
        }
    }

    private static boolean isClose(Point2D p, Point2D q) {
        return Math.abs(p.getX() - q.getX()) < NEARBY_RADIUS
               && Math.abs(p.getY() - q.getY()) < NEARBY_RADIUS;
    }

    private static void clampPoint(Point.Float p) {
        p.x = ImageMath.clamp01(p.x);
        p.y = ImageMath.clamp01(p.y);
    }

    public int addKnot(Point.Float p, boolean allowReplace) {
        // clamp to boundaries [0,1]
        clampPoint(p);

        int lastIndex = curve.x.length - 1;
        int index = curve.findKnotPos(p.x);

        // Can't add knot at first/last position
        if (index <= 0 || index > lastIndex) {
            return -1;
        }

        // if point is too close to next/prev knot -> replace the nearest
        // this protect against placing two knots too close to each other
        if (allowReplace) {
            int prevIndex = index - 1;
            if (isClose(p, new Point.Float(curve.x[prevIndex], curve.y[prevIndex]))) {
                setKnotPosition(prevIndex, p);
                return prevIndex;
            } else if (isClose(p, new Point.Float(curve.x[index], curve.y[index]))) {
                setKnotPosition(index, p);
                return index;
            }
        }

        // check for max knot limit
        if (curve.x.length >= MAX_KNOTS) {
            return -1;
        }

        dirty = true;
        return curve.addKnot(p.x, p.y);
    }

    public void deleteKnot(int index) {
        if (index < 0 || index > curve.x.length - 1) {
            return;
        }

        if (curve.x.length <= 2) {
            return;
        }

        dirty = true;
        curve.removeKnot(index);
    }

    /**
     * Set new knot location
     *
     * @param index point index
     * @param p     normalized point data
     */
    public void setKnotPosition(int index, Point.Float p) {
        int lastIndex = curve.x.length - 1;

        if (index < 0 || index > lastIndex) {
            return;
        }

        // check prev/next index - knots can't change they index
        if (index > 0 && p.x < curve.x[index - 1]) {
            p.x = curve.x[index - 1];
        } else if (index < lastIndex && p.x > curve.x[index + 1]) {
            p.x = curve.x[index + 1];
        }

        curve.x[index] = ImageMath.clamp01(p.x);
        curve.y[index] = ImageMath.clamp01(p.y);
        dirty = true;
    }

    /**
     * Check if point p is out of index range (check prev/next knot)
     *
     * @param index knot index
     * @param p     normalized point data
     */
    public boolean isDraggedOff(int index, Point.Float p) {
        if (index <= 0 || index >= curve.x.length - 1) {
            return false;
        }

        if (p.x > curve.x[index + 1] + 0.02F || p.x < curve.x[index - 1] - 0.02F) {
            return true;
        }

        return false;
    }

    /**
     * Check if point p is allowed to put at given index
     *
     * @param index knot index
     * @param p     normalized point data
     */
    public boolean isDraggedIn(int index, Point.Float p) {
        if (index <= 0 || index > curve.x.length - 1) {
            return false;
        }

        if (p.x < curve.x[index] && p.x > curve.x[index - 1]) {
            return true;
        }

        return false;
    }

    private static boolean isOver(Point.Float p, Point.Float q) {
        if (Math.abs(p.x - q.x) < KNOT_RADIUS) {
            return Math.abs(p.y - q.y) < KNOT_RADIUS;
        }
        return false;
    }

    public boolean isOverKnot(Point.Float p) {
        return getKnotIndexAt(p) >= 0;
    }

    public boolean isOverKnot(int index) {
        var p = new Point.Float(curve.x[index], curve.y[index]);
        for (int i = 0; i < curve.x.length; i++) {
            if (i != index && isOver(p, new Point.Float(curve.x[i], curve.y[i]))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isOverChart(Point.Float p) {
        return p.x >= 0 && p.x <= 1 && p.y >= 0 && p.y <= 1;
    }

    public int getKnotIndexAt(Point.Float p) {
        for (int i = 0; i < curve.x.length; i++) {
            if (isOver(p, new Point.Float(curve.x[i], curve.y[i]))) {
                return i;
            }
        }

        return -1;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void draw(Graphics2D g) {
        drawCurve(g);
        if (active) {
            drawKnots(g);
        }
    }

    private void drawCurve(Graphics2D g) {
        initCurvePlotData();
        Path2D path = new Path2D.Float();
        path.moveTo(0, ((float) curvePlotData[0] / 255) * height);
        for (int i = 0; i < curvePlotData.length; i++) {
            float x = ((float) i / 255) * width;
            float y = ((float) curvePlotData[i] / 255) * height;
            path.lineTo(x, y);
        }

        g.setColor(channel.getDrawColor(active));
        g.setStroke(curveStroke);
        g.draw(path);
    }

    private void drawKnots(Graphics2D g) {
        g.setColor(Color.black);
        g.setStroke(pointStroke);
        int knotSize = 2 * KNOT_RADIUS_PX;
        for (int i = 0; i < curve.x.length; i++) {
            g.drawOval(
                (int) (curve.x[i] * width) - KNOT_RADIUS_PX,
                (int) (curve.y[i] * height) - KNOT_RADIUS_PX,
                knotSize,
                knotSize
            );
        }
    }

    public String toSaveString() {
        int numPoints = curve.x.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numPoints; i++) {
            sb.append(curve.x[i]);
            sb.append(",");
            sb.append(curve.y[i]);
            if (i != numPoints - 1) {
                sb.append("#");
            }
        }
        return sb.toString();
    }

    public void setStateFrom(String savedValue) {
        String[] xyPairs = savedValue.split("#");
        int numPoints = xyPairs.length;
        curve.x = new float[numPoints];
        curve.y = new float[numPoints];
        for (int i = 0; i < numPoints; i++) {
            String pair = xyPairs[i];
            int commaIndex = pair.indexOf(',');
            String pairX = pair.substring(0, commaIndex);
            String pairY = pair.substring(commaIndex + 1);
            curve.x[i] = Float.parseFloat(pairX);
            curve.y[i] = Float.parseFloat(pairY);
        }
        dirty = true;
    }
}