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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.random.RandomGenerator;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.geom.PathIterator.*;

/**
 * Static shape-related utility methods.
 */
public class Shapes {
    private static final Stroke BIG_STROKE = new BasicStroke(3);
    private static final Stroke SMALL_STROKE = new BasicStroke(1);
    private static final int ELASTIC_LINE_SEGMENTS = 26;

    private Shapes() {
        // do not instantiate
    }

    /**
     * Converts the given {@link Shape}, assumed to be
     * in image coordinates, to a {@link Path}.
     */
    public static Path shapeToPath(Shape shape, View view) {
        Composition comp = view == null
            ? null
            : view.getComp();
        Path path = new Path(comp, comp != null);

        PathIterator pathIterator = shape.getPathIterator(null);
        double[] coords = new double[6];

        SubPath lastSubPath = null;

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            // ignore malformed segments that occur before the first SEG_MOVETO
            if (lastSubPath == null && type != SEG_MOVETO) {
                pathIterator.next();
                continue;
            }

            switch (type) {
                case SEG_MOVETO -> lastSubPath = path.startNewSubpath(coords[0], coords[1], view);
                case SEG_LINETO -> lastSubPath.addLine(coords[0], coords[1], view);
                case SEG_QUADTO -> lastSubPath.addQuadCurve(coords[0], coords[1], coords[2], coords[3], view);
                case SEG_CUBICTO ->
                    lastSubPath.addCubicCurve(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], view);
                case SEG_CLOSE -> lastSubPath.close(false);
                default -> throw new IllegalArgumentException("type = " + type);
            }

            pathIterator.next();
        }

        path.mergeCloseAnchors();
        path.setHeuristicTypes();
        assert path.checkInvariants();

        return path;
    }

    /**
     * Draws the given Shape on the given Graphics2D so that
     * it is clearly visible over any background.
     */
    public static void drawVisibly(Graphics2D g, Shape shape) {
        assert shape != null;

        // draw with a thick black outline
        g.setStroke(BIG_STROKE);
        g.setColor(BLACK);
        g.draw(shape);

        // draw with a thinner white stroke inside
        g.setStroke(SMALL_STROKE);
        g.setColor(WHITE);
        g.draw(shape);
    }

    /**
     * Fills the given Shape with the given color and also adds a black outline.
     */
    public static void fillVisibly(Graphics2D g, Shape shape, Color fillColor) {
        assert shape != null;
        assert !fillColor.equals(BLACK);

        // black at the edges
        g.setStroke(BIG_STROKE);
        g.setColor(BLACK);
        g.draw(shape);

        // fill with the given color in the middle
        g.setColor(fillColor);
        g.fill(shape);
    }

    /**
     * Draws the given shape with the given color,
     * and then restores the previous color.
     */
    public static void draw(Shape shape, Color c, Graphics2D g) {
        Color prevColor = g.getColor();
        g.setColor(c);
        g.draw(shape);
        g.setColor(prevColor);
    }

    /**
     * Ensures that the returned rectangle has positive width and height.
     */
    public static Rectangle posRectFromCorners(int x1, int y1, int x2, int y2) {
        int x = Math.min(x1, x2);
        int y = Math.min(y1, y2);
        int width = Math.abs(x1 - x2);
        int height = Math.abs(y1 - y2);
        return new Rectangle(x, y, width, height);
    }

    /**
     * Ensures that the returned rectangle has positive width and height.
     */
    public static Rectangle2D toPositiveRect(Rectangle2D input) {
        double width = input.getWidth();
        double height = input.getHeight();

        if (width >= 0 && height >= 0) {
            return input;
        }

        double x = input.getX();
        double y = input.getY();

        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }
        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Ensures that the returned rectangle has positive width and height.
     */
    public static Rectangle toPositiveRect(Rectangle input) {
        if (input.width >= 0 && input.height >= 0) {
            return input;
        }

        int x = input.x;
        int y = input.y;
        int width = input.width;
        int height = input.height;

        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }
        return new Rectangle(x, y, width, height);
    }

    public static String toSvgPath(Shape shape) {
        StringBuilder sb = new StringBuilder();
        PathIterator pathIterator = shape.getPathIterator(null);
        double[] coords = new double[6];

        StringBuilder subpathBuilder = new StringBuilder();
        boolean subpathIsValid = true;

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            if (type == SEG_MOVETO) {
                // if the previous subpath was fully valid, commit it to the main builder
                if (subpathIsValid && !subpathBuilder.isEmpty()) {
                    sb.append(subpathBuilder);
                }
                subpathBuilder.setLength(0);
                subpathIsValid = true;
            }

            if (subpathIsValid) {
                // if any segment fails the check, mark the whole subpath as invalid
                if (!appendSvgPathSegment(subpathBuilder, type, coords)) {
                    subpathIsValid = false;
                }
            }

            pathIterator.next();
        }

        // commit the final subpath
        if (subpathIsValid && !subpathBuilder.isEmpty()) {
            sb.append(subpathBuilder);
        }

        return sb.toString();
    }

    private static boolean appendSvgPathSegment(StringBuilder pathBuilder, int type, double[] coords) {
        String command = switch (type) {
            case SEG_MOVETO -> "M ";
            case SEG_LINETO -> "L ";
            case SEG_QUADTO -> "Q ";
            case SEG_CUBICTO -> "C ";
            case SEG_CLOSE -> "Z";
            default -> throw new IllegalArgumentException("type = " + type);
        };

        // NaNs are not a problem if they are in the unused part of the array
        int numCoords = getCoordinateCount(type);
        for (int i = 0; i < numCoords; i++) {
            boolean segmentValid = Double.isFinite(coords[i]);
            assert segmentValid; // developers should be alerted, but end-users not
            if (!segmentValid) {
                return false; // invalid segment, will be ignored
            }
        }

        pathBuilder.append(command);
        for (int i = 0; i < numCoords; i++) {
            pathBuilder.append(String.format(Locale.ROOT, "%.3f", coords[i]));
            if (i != numCoords - 1) {
                pathBuilder.append(' ');
            }
        }
        pathBuilder.append('\n');
        return true; // success
    }

    public static String getSvgFillRule(Shape shape) {
        int windingRule = shape instanceof Path2D path
            ? path.getWindingRule()
            : shape.getPathIterator(null).getWindingRule();

        return switch (windingRule) {
            case Path2D.WIND_EVEN_ODD -> "evenodd";
            case Path2D.WIND_NON_ZERO -> "nonzero";
            default -> throw new IllegalStateException("Error: " + windingRule);
        };
    }

    public static String debugPathIterator(Shape shape) {
        return debugPathIterator(shape.getPathIterator(null));
    }

    public static String debugPathIterator(PathIterator pathIterator) {
        StringBuilder sb = new StringBuilder();
        double[] coords = new double[6];

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            String command = switch (type) {
                case SEG_MOVETO -> "MOVE TO";
                case SEG_LINETO -> "LINE TO";
                case SEG_QUADTO -> "QUAD TO";
                case SEG_CUBICTO -> "CUBIC TO";
                case SEG_CLOSE -> "CLOSE";
                default -> throw new IllegalArgumentException("type = " + type);
            };

            int numCoords = getCoordinateCount(type);
            String line = (numCoords == 0)
                ? command
                : command + " " + formatCoords(coords, numCoords);

            sb.append(line).append(System.lineSeparator());
            pathIterator.next();
        }

        return sb.toString();
    }

    /**
     * Converts the first n elements of the given array to a string
     * representation, with numbers rounded to 2 decimal places.
     */
    private static String formatCoords(double[] array, int n) {
        var sj = new StringJoiner(", ", "(", ")");
        for (int i = 0; i < n; i++) {
            sj.add(String.format(Locale.ROOT, "%.2f", array[i]));
        }
        return sj.toString();
    }

    /**
     * Returns true if the two given shapes have identical path iterators within the given tolerance.
     */
    public static boolean pathsAreEqual(Shape shape1, Shape shape2, double tolerance) {
        PathIterator pathIterator1 = shape1.getPathIterator(null);
        PathIterator pathIterator2 = shape2.getPathIterator(null);

        int windingRule1 = pathIterator1.getWindingRule();
        int windingRule2 = pathIterator2.getWindingRule();
        if (windingRule1 != windingRule2) {
            return false;
        }

        double[] coords1 = new double[6];
        double[] coords2 = new double[6];

        while (!pathIterator1.isDone()) {
            if (pathIterator2.isDone()) {
                return false;
            }

            int type1 = pathIterator1.currentSegment(coords1);
            int type2 = pathIterator2.currentSegment(coords2);
            if (type1 != type2) {
                return false;
            }

            int numCoordsToCompare = getCoordinateCount(type1);

            for (int i = 0; i < numCoordsToCompare; i++) {
                double c1 = coords1[i];
                double c2 = coords2[i];
                if (!Double.isFinite(c1) || !Double.isFinite(c2) || Math.abs(c1 - c2) > tolerance) {
                    return false;
                }
            }

            pathIterator1.next();
            pathIterator2.next();
        }

        return pathIterator2.isDone();
    }

    public static Shape randomize(Shape in, RandomGenerator rng, double amount) {
        Path path = shapeToPath(in, null);
        path.randomize(rng, amount);
        return path.toImageSpaceShape();
    }

    public static void debug(Graphics2D g, Color c, Point2D point) {
        debug(g, c, point, 5);
    }

    public static void debug(Graphics2D g, Color c, Point2D point, int radius) {
        Shape circle = CustomShapes.createCircle(point.getX(), point.getY(), radius);
        debug(g, c, circle);
    }

    public static void debug(Graphics2D g, Color c, Shape shape) {
        Color origColor = g.getColor();
        Stroke origStroke = g.getStroke();

        g.setColor(c);
        g.setStroke(new BasicStroke(5));
        g.draw(shape);

        g.setColor(origColor);
        g.setStroke(origStroke);
    }

    /**
     * Rounds the coordinates and dimensions of a Rectangle2D, ensuring a minimum size of 1x1.
     */
    public static Rectangle roundRect(Rectangle2D rect) {
        // works only with positive rectangles
        assert rect.getWidth() >= 0 && rect.getHeight() >= 0 : "rect = " + rect;

        int x = (int) Math.round(rect.getX());
        int y = (int) Math.round(rect.getY());
        int width = (int) Math.round(rect.getWidth());
        int height = (int) Math.round(rect.getHeight());

        if (width == 0) {
            width = 1;
        }
        if (height == 0) {
            height = 1;
        }
        return new Rectangle(x, y, width, height);
    }

    /**
     * Creates a Path2D that connects the given points with line segments.
     */
    public static Path2D lineConnect(List<Point2D> points) {
        int numPoints = points.size();
        Path2D path = new Path2D.Double();
        if (numPoints == 0) {
            return path;
        }

        Point2D firstPoint = points.getFirst();
        path.moveTo(firstPoint.getX(), firstPoint.getY());
        for (int i = 1; i < numPoints; i++) {
            Point2D point = points.get(i);
            path.lineTo(point.getX(), point.getY());
        }
        return path;
    }

    /**
     * Creates a Path2D that connects the given points smoothly with cubic Bézier curves.
     * Based on http://web.archive.org/web/20131027060328/http://www.antigrain.com/research/bezier_interpolation/index.html#PAGE_BEZIER_INTERPOLATION
     */
    public static Path2D smoothConnect(List<Point2D> points) {
        Path2D.Double path = new Path2D.Double();

        Point2D first = points.getFirst();
        path.moveTo(first.getX(), first.getY());

        smoothConnect(points, path);
        return path;
    }

    /**
     * The performance-optimized version of smooth connect.
     */
    public static void smoothConnect(List<Point2D> points, Path2D path) {
        int numPoints = points.size();
        if (numPoints <= 2) {
            throw new IllegalArgumentException("numPoints = " + numPoints);
        }

        // calculate arrays of centers and lengths to avoid repeated computing
        Point2D[] centers = new Point2D[numPoints - 1];
        double[] lengths = new double[numPoints - 1];
        for (int i = 0; i < numPoints - 1; i++) {
            Point2D start = points.get(i);
            Point2D end = points.get(i + 1);
            centers[i] = Geometry.midPoint(start, end);
            lengths[i] = Geometry.distance(start, end);

            // prevent 0 / 0 division by ensuring adjacent points are not identical
            assert lengths[i] > 0;
        }

        for (int i = 1; i < numPoints; i++) {
            Point2D start = points.get(i - 1);
            Point2D end = points.get(i);

            double x1 = start.getX();
            double y1 = start.getY();
            double x2 = end.getX();
            double y2 = end.getY();

            double currentLen = lengths[i - 1];
            Point2D center2 = centers[i - 1];

            double prevLen;
            Point2D center1;
            if (i == 1) {
                prevLen = 0;
                center1 = start;
            } else {
                prevLen = lengths[i - 2];
                center1 = centers[i - 2];
            }

            double nextLen;
            Point2D center3;

            if (i == numPoints - 1) {
                nextLen = 0;
                center3 = end;
            } else {
                nextLen = lengths[i];
                center3 = centers[i];
            }

            double xc1 = center1.getX();
            double yc1 = center1.getY();
            double xc2 = center2.getX();
            double yc2 = center2.getY();
            double xc3 = center3.getX();
            double yc3 = center3.getY();

            double k1 = prevLen / (prevLen + currentLen);
            double k2 = currentLen / (currentLen + nextLen);

            double xm1 = xc1 + (xc2 - xc1) * k1;
            double ym1 = yc1 + (yc2 - yc1) * k1;

            double xm2 = xc2 + (xc3 - xc2) * k2;
            double ym2 = yc2 + (yc3 - yc2) * k2;

            double ctrl1X = xc2 + x1 - xm1;
            double ctrl1Y = yc2 + y1 - ym1;

            double ctrl2X = xc2 + x2 - xm2;
            double ctrl2Y = yc2 + y2 - ym2;

            path.curveTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, end.getX(), end.getY());
        }
    }

    /**
     * Another version of the above method, which supports closed paths and
     * has a smoothness parameter.
     */
    public static Path2D smoothConnect(List<Point2D> points, double smoothness) {
        // the path is considered closed if the first and last points are identical
        boolean isClosed = Geometry.areEqual(points.getFirst(), points.getLast());

        int numPoints = points.size();
        assert isClosed ? numPoints >= 4 : numPoints >= 3 :
            "Insufficient points: " + numPoints + (isClosed ? " (closed)" : " (open)");

        int lastPointIndex = isClosed ? numPoints - 2 : numPoints - 1;

        // Every two alternate points represent a side. There are numPoints - 1 sides.

        // mid-points of all those sides
        Point2D[] centers = new Point2D.Double[numPoints - 1];
        // lengths of all those sides
        double[] lengths = new double[numPoints - 1];

        for (int i = 0; i < numPoints - 1; i++) {
            Point2D currentPoint = points.get(i);
            Point2D nextPoint = points.get(i + 1);
            centers[i] = Geometry.midPoint(currentPoint, nextPoint);
            lengths[i] = Geometry.distance(currentPoint, nextPoint);

            // prevent 0 / 0 division by ensuring adjacent points are not identical
            assert lengths[i] > 0;
        }

        // controlPoints[i] represents the 2 control points after and before points[i]
        // If the path is closed, last point == first point, so we make a less control point.
        var controlPoints = new Point2D.Double[lastPointIndex + 1][2];
        for (int i = 0; i < controlPoints.length; i++) {
            controlPoints[i][0] = new Point2D.Double();
            controlPoints[i][1] = new Point2D.Double();
        }

        for (int i = 1; i < numPoints - 1; i++) {
            Geometry.copy(centers[i - 1], controlPoints[i][0]);
            Geometry.copy(centers[i], controlPoints[i][1]);
            calcControlPoint(points.get(i),
                controlPoints[i][0], controlPoints[i][1],
                lengths[i - 1], lengths[i], smoothness);
        }

        Path2D path = new Path2D.Float();
        Point2D point = points.getFirst();
        Point2D lastPoint = points.get(lastPointIndex);

        // for first point
        if (isClosed) {
            // first and last point's center
            Geometry.copy(centers[centers.length - 1], controlPoints[0][0]);
            // first and second's center
            Geometry.copy(centers[0], controlPoints[0][1]);
            calcControlPoint(point,
                controlPoints[0][0], controlPoints[0][1],
                lengths[lengths.length - 1], lengths[0], smoothness);
            path.moveTo(lastPoint.getX(), lastPoint.getY());
        } else {
            Geometry.copy(point, controlPoints[0][1]);
            Geometry.copy(lastPoint, controlPoints[lastPointIndex][0]);

            path.moveTo(point.getX(), point.getY());
        }

        // i = 0 tries to put the curve between first and last point.
        // Therefore, if shape is open, we start putting the curve from i=1.
        for (int i = isClosed ? 0 : 1; i < controlPoints.length; i++) {
            Point2D A = points.get(i);
            Point2D P = controlPoints[i][0];
            Point2D oldQ = i == 0
                ? controlPoints[controlPoints.length - 1][1]
                : controlPoints[i - 1][1];

            path.curveTo(oldQ.getX(), oldQ.getY(), P.getX(), P.getY(), A.getX(), A.getY());
        }

        if (isClosed) {
            path.closePath();
        }

        return path;
    }

    /**
     * Adjusts the adjacent segment midpoints P and Q around anchor
     * point B to serve as smooth cubic Bézier control points.
     *
     * @param B          the anchor point between the two segments
     * @param P          the midpoint of the preceding segment (modified in place)
     * @param Q          the midpoint of the succeeding segment (modified in place)
     * @param AB         the length of the preceding segment
     * @param BC         the length of the succeeding segment
     * @param smoothness the scaling factor for the control point distance (tension)
     */
    private static void calcControlPoint(Point2D B, Point2D P, Point2D Q,
                                         double AB, double BC, double smoothness) {
        double totalWeight = AB + BC;
        assert totalWeight > 0 : "Sum of segment lengths must be positive";

        // division point T on line PQ
        double tx = (P.getX() * BC + Q.getX() * AB) / totalWeight;
        double ty = (P.getY() * BC + Q.getY() * AB) / totalWeight;

        // displace relative to T, scale by smoothness, and translate to B
        double bx = B.getX();
        double by = B.getY();

        P.setLocation(bx + (P.getX() - tx) * smoothness, by + (P.getY() - ty) * smoothness);
        Q.setLocation(bx + (Q.getX() - tx) * smoothness, by + (Q.getY() - ty) * smoothness);
    }

    public static Shape rotate(Shape shape, double angle, double anchorX, double anchorY) {
        return AffineTransform.getRotateInstance(angle, anchorX, anchorY)
            .createTransformedShape(shape);
    }

    public static Shape translate(Shape shape, double tx, double ty) {
        return AffineTransform.getTranslateInstance(tx, ty)
            .createTransformedShape(shape);
    }

    /**
     * Creates a line between two points that can optionally be made
     * "elastic" by breaking it into small curved segments.
     */
    public static void elasticLine(Path2D path, Point2D from, Point2D to, boolean elastic) {
        assert !from.equals(to);
        if (elastic) {
            double fromX = from.getX();
            double fromY = from.getY();
            double toX = to.getX();
            double toY = to.getY();
            double dx = toX - fromX;
            double dy = toY - fromY;

            double dt = 1.0 / ELASTIC_LINE_SEGMENTS;
            for (int i = 0; i < ELASTIC_LINE_SEGMENTS; i++) {
                double cpFactor = (i + 0.5) * dt;
                double cpX = fromX + cpFactor * dx;
                double cpY = fromY + cpFactor * dy;

                boolean last = (i == ELASTIC_LINE_SEGMENTS - 1);
                double endX = last ? toX : fromX + (i + 1) * dt * dx;
                double endY = last ? toY : fromY + (i + 1) * dt * dy;

                path.curveTo(cpX, cpY, cpX, cpY, endX, endY);
            }
        } else {
            path.lineTo(to.getX(), to.getY());
        }
    }

    /**
     * Draws a curved line from the current point to the end point
     * using a quadratic Bézier curve. The control point is calculated
     * to be perpendicular to the midpoint of the line segment.
     */
    public static void curvedLine(Path2D path, double curvature,
                                  Point2D start, Point2D end) {
        assert !start.equals(end);

        double endX = end.getX();
        double endY = end.getY();

        if (curvature == 0) {
            path.lineTo(endX, endY);
        } else {
            double startX = start.getX();
            double startY = start.getY();

            double midX = (startX + endX) * 0.5;
            double midY = (startY + endY) * 0.5;

            // perpendicular vector (-dy, dx) scaled by (curvature / 2)
            double halfCurvature = curvature * 0.5;
            double cpX = midX - (endY - startY) * halfCurvature;
            double cpY = midY + (endX - startX) * halfCurvature;

            path.quadTo(cpX, cpY, endX, endY);
        }
    }

    /**
     * Resizes the given shape to fit centrally within a target rectangle
     * without distortion, considering the given width, height, margin, and offset.
     *
     * @param shape  The shape to be resized.
     * @param width  The width of the target rectangle.
     * @param height The height of the target rectangle.
     * @param startX The horizontal offset to apply after resizing.
     * @param startY The vertical offset to apply after resizing.
     * @param margin The margin around the shape inside the target rectangle.
     * @return A new shape that fits within the target rectangle.
     */
    public static Shape resizeToFit(Shape shape, double width, double height,
                                    double startX, double startY, double margin) {
        Rectangle2D bounds = shape.getBounds2D();
        assert bounds.getWidth() > 0 && bounds.getHeight() > 0;

        double areaWidth = width - 2 * margin;
        double areaHeight = height - 2 * margin;
        assert areaWidth > 0 && areaHeight > 0;

        double scale = Math.min(areaWidth / bounds.getWidth(), areaHeight / bounds.getHeight());
        double targetWidth = bounds.getWidth() * scale;
        double targetHeight = bounds.getHeight() * scale;

        // centered offsets: (totalDimension - scaledDimension) / 2
        double tx = startX + (width - targetWidth) * 0.5 - bounds.getX() * scale;
        double ty = startY + (height - targetHeight) * 0.5 - bounds.getY() * scale;

        AffineTransform at = new AffineTransform(scale, 0, 0, scale, tx, ty);
        return at.createTransformedShape(shape);
    }

    public static List<Point2D> getAnchorPoints(Shape shape) {
        List<Point2D> points = new ArrayList<>();
        double[] coords = new double[6];

        PathIterator pathIterator = shape.getPathIterator(null);
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);
            Point2D point = switch (type) {
                case SEG_MOVETO, SEG_LINETO -> new Point2D.Double(coords[0], coords[1]);
                case SEG_QUADTO -> new Point2D.Double(coords[2], coords[3]);
                case SEG_CUBICTO -> new Point2D.Double(coords[4], coords[5]);
                default -> null;
            };
            if (point != null) {
                points.add(point);
            }
            pathIterator.next();
        }
        return points;
    }

    public static double calcPathLength(Shape path) {
        double pathLength = 0;
        double[] points = new double[6];
        PathIterator pathIt = new FlatteningPathIterator(path.getPathIterator(null), 1);
        double moveX = 0, moveY = 0;
        double lastX = 0, lastY = 0;

        while (!pathIt.isDone()) {
            int type = pathIt.currentSegment(points);
            switch (type) {
                case SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    break;
                case SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // fall through
                case SEG_LINETO:
                    double dx = points[0] - lastX;
                    double dy = points[1] - lastY;
                    pathLength += Math.sqrt(dx * dx + dy * dy);
                    lastX = points[0];
                    lastY = points[1];
                    break;
            }
            pathIt.next();
        }
        return pathLength;
    }

    /**
     * Returns the number of coordinate values associated with the given PathIterator segment type.
     */
    private static int getCoordinateCount(int segmentType) {
        return switch (segmentType) {
            case SEG_MOVETO, SEG_LINETO -> 2;
            case SEG_QUADTO -> 4;
            case SEG_CUBICTO -> 6;
            case SEG_CLOSE -> 0;
            default -> throw new IllegalArgumentException("Unknown segment type: " + segmentType);
        };
    }
}
