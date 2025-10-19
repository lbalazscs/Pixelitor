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

package pixelitor.utils;

import com.bric.geom.RectangularTransform;
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.pen.Path;
import pixelitor.tools.pen.SubPath;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;

/**
 * Static shape-related utility methods.
 */
public class Shapes {
    private static final Stroke BIG_STROKE = new BasicStroke(3);
    private static final Stroke SMALL_STROKE = new BasicStroke(1);
    public static final double UNIT_ARROW_HEAD_WIDTH = 0.7;

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
        assert fillColor != BLACK;

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
     * and then resores the previous color.
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
    public static Rectangle toPositiveRect(int x1, int x2, int y1, int y2) {
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
        double inX = input.getX();
        double inY = input.getY();
        double inWidth = input.getWidth();
        double inHeight = input.getHeight();

        if (inWidth >= 0 && inHeight >= 0) {
            return input; // should be the most common case
        }

        double x = (inWidth < 0) ? inX + inWidth : inX;
        double y = (inHeight < 0) ? inY + inHeight : inY;
        double width = Math.abs(inWidth);
        double height = Math.abs(inHeight);

        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Ensures that the returned rectangle has positive width and height.
     */
    public static Rectangle toPositiveRect(Rectangle rect) {
        if (rect.width >= 0 && rect.height >= 0) {
            // no adjustments are needed => return the original
            return rect;
        }

        int x = rect.x;
        int y = rect.y;
        int width = rect.width;
        int height = rect.height;

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

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);
            appendSvgPathSegment(sb, type, coords);
            pathIterator.next();
        }
        return sb.toString();
    }

    private static void appendSvgPathSegment(StringBuilder pathBuilder, int type, double[] coords) {
        int numCoords;
        String command;

        switch (type) {
            case SEG_MOVETO -> {
                numCoords = 2;
                command = "M ";
            }
            case SEG_LINETO -> {
                numCoords = 2;
                command = "L ";
            }
            case SEG_QUADTO -> {
                numCoords = 4;
                command = "Q ";
            }
            case SEG_CUBICTO -> {
                numCoords = 6;
                command = "C ";
            }
            case SEG_CLOSE -> {
                numCoords = 0;
                command = "Z";
            }
            default -> throw new IllegalArgumentException("type = " + type);
        }

        // NaNs are not a problem if they are in the unused part of the array
        for (int i = 0; i < numCoords; i++) {
            if (Double.isNaN(coords[i])) {
                throw new IllegalArgumentException("i = " + i);
            }
        }

        pathBuilder.append(command);
        for (int i = 0; i < numCoords; i++) {
            pathBuilder.append(String.format("%.3f", coords[i]));
            if (i != numCoords - 1) {
                pathBuilder.append(' ');
            }
        }
        pathBuilder.append('\n');
    }

    public static String getSvgFillRule(Shape shape) {
        if (shape instanceof Path2D path) {
            return switch (path.getWindingRule()) {
                case Path2D.WIND_EVEN_ODD -> "evenodd";
                case Path2D.WIND_NON_ZERO -> "nonzero";
                default -> throw new IllegalStateException("Error: " + path.getWindingRule());
            };
        }
        return "nonzero";
    }

    public static void debugPathIterator(Shape shape) {
        debugPathIterator(shape.getPathIterator(null));
    }

    public static void debugPathIterator(PathIterator pathIterator) {
        double[] coords = new double[6];

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            switch (type) {
                case SEG_MOVETO -> System.out.println("MOVE TO " + arrayToString(coords, 2));
                case SEG_LINETO -> System.out.println("LINE TO " + arrayToString(coords, 2));
                case SEG_QUADTO -> System.out.println("QUAD TO " + arrayToString(coords, 4));
                case SEG_CUBICTO -> System.out.println("CUBIC TO " + arrayToString(coords, 6));
                case SEG_CLOSE -> System.out.println("CLOSE " + arrayToString(coords, 2));
                default -> throw new IllegalArgumentException("type = " + type);
            }

            pathIterator.next();
        }
    }

    /**
     * Converts the first n elements of the given array to a string
     * representation, with numbers rounded to 2 decimal places.
     */
    private static String arrayToString(double[] array, int n) {
        return Arrays.stream(Arrays.copyOf(array, n))
            .mapToObj(d -> String.format("%.2f", d))
            .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Returns true if the two given shapes have identical path iterators within the given tolerance.
     */
    public static boolean pathsAreEqual(Shape shape1, Shape shape2, double tolerance) {
        PathIterator pathIterator1 = shape1.getPathIterator(null);
        PathIterator pathIterator2 = shape2.getPathIterator(null);

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

            int numCoordsToCompare = switch (type1) {
                case SEG_MOVETO, SEG_LINETO -> 2;
                case SEG_QUADTO -> 4;
                case SEG_CUBICTO -> 6;
                case SEG_CLOSE -> 0;
                default -> throw new IllegalStateException("Unexpected segment type: " + type1);
            };

            for (int i = 0; i < numCoordsToCompare; i++) {
                if (Math.abs(coords1[i] - coords2[i]) > tolerance) {
                    return false;
                }
            }

            pathIterator1.next();
            pathIterator2.next();
        }

        return pathIterator2.isDone();
    }

    public static Shape randomize(Shape in, Random rng, double amount) {
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

    public static Rectangle2D calcBounds(List<? extends Point2D> points) {
        BoundingBox boundingBox = new BoundingBox();
        for (Point2D point : points) {
            boundingBox.add(point);
        }
        return boundingBox.toRectangle2D();
    }

    /**
     * Rounds the coordinates and dimensions of a Rectangle2D, ensuring a minimum size of 1x1.
     */
    public static Rectangle roundRect(Rectangle2D rect) {
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

            double ctrl1X = xm1 + (xc2 - xm1) + x1 - xm1;
            double ctrl1Y = ym1 + (yc2 - ym1) + y1 - ym1;

            double ctrl2X = xm2 + (xc2 - xm2) + x2 - xm2;
            double ctrl2Y = ym2 + (yc2 - ym2) + y2 - ym2;

            path.curveTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, end.getX(), end.getY());
        }
    }

    /**
     * Another version of the above method, which supports closed paths and
     * has a smoothness parameter.
     */
    public static Path2D smoothConnect(List<Point2D> points, double smoothness) {
        int numPoints = points.size();
        assert numPoints >= 3 : "There should be at least 3 points in the shape!!!";

        // the path is considered closed if the first and last points are identical
        boolean isClosed = Geometry.areEqual(points.getFirst(), points.get(numPoints - 1));
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
            calculateControlPoint(points.get(i),
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
            calculateControlPoint(point,
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

        return path;
    }

    private static void calculateControlPoint(Point2D B, Point2D P, Point2D Q,
                                              double AB, double BC, double smoothness) {
        // a temporary point T calculated such that
        // * for A=points[i-1], B=points[i] and C = points[i+1]
        //   * for midpoint of AB, P=centers[i-1] and midpoint of BC, Q=centers[i]
        //     * it lies on the line joining P and Q
        //     * PT / AB == TQ / BC       - (1)
        //
        // mathematically, with the given data,
        //
        // * using section formula (on Vectors)
        //   * T = (P * n + Q * m) / (m + n)
        //   * T = P * n / (m + n) + Q * m / (m + n)
        //   * T = P * TQ / PQ + Q * PT / PQ
        //
        // * using componendo rule on (1)
        //   * T = P * AB / (AB + BC) + Q * BC / (AB + BC)
        //   * T = (P * AB + Q * BC) / (AB + BC)
        //
        var T = new Point2D.Double(); // the division point

        Geometry.calcDivisionPoint(P, Q, AB, BC, T);

        // converting point vectors P and Q to show relative displacement from T
        // P = P - T, Q = Q - T
        Geometry.subtract(P, T, P);
        Geometry.subtract(Q, T, Q);

        // scaling the point vectors P and Q about origin
        if (smoothness != 1) {
            Geometry.scale(P, smoothness);
            Geometry.scale(Q, smoothness);
        }

        // translating point vectors P and Q by B so that
        // the relative position of original P and Q to T is same as
        // the relative position of new P and Q to B.
        Geometry.add(P, B, P);
        Geometry.add(Q, B, Q);
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
        if (elastic) {
            // create a line that can be distorted by nonlinear distortions
            int numSegments = 26;
            double dt = 1.0 / numSegments;
            for (int i = 0; i <= numSegments; i++) {
                double t = i * dt;
                Point2D controlPoint = Geometry.interpolate(from, to, t + dt * 0.5);
                Point2D segmentEnd = Geometry.interpolate(from, to, t + dt);
                path.curveTo(controlPoint.getX(), controlPoint.getY(),
                    controlPoint.getX(), controlPoint.getY(),
                    segmentEnd.getX(), segmentEnd.getY());
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
        double endX = end.getX();
        double endY = end.getY();

        if (curvature == 0) {
            path.lineTo(endX, endY);
        } else {
            Point2D midPoint = Geometry.midPoint(start, end);

            // vector from start to end
            Point2D vec = new Point2D.Double();
            Geometry.subtract(end, start, vec);

            // perpendicular vector
            Point2D perpVec = new Point2D.Double(-vec.getY(), vec.getX());
            Geometry.normalize(perpVec);

            // the distance of the control point is proportional
            // to the curvature and line length
            double distance = curvature * Geometry.distance(start, end) / 2.0;
            Geometry.scale(perpVec, distance);

            Point2D controlPoint = Geometry.add(midPoint, perpVec, new Point2D.Double());

            path.quadTo(controlPoint.getX(), controlPoint.getY(), end.getX(), end.getY());
        }
    }

    /**
     * Resizes the given shape to fit centrally within a target rectangle
     * without distortion, considering the given width, height, margin, and offset.
     *
     * @param shape   The shape to be resized.
     * @param width   The width of the target rectangle.
     * @param height  The height of the target rectangle.
     * @param margin  The margin around the shape inside the target rectangle.
     * @param startX  The horizontal offset to apply after resizing.
     * @param startY  The vertical offset to apply after resizing.
     * @return A new shape that fits within the target rectangle.
     */
    public static Shape resizeToFit(Shape shape, double width, double height, double margin,
                                    double startX, double startY) {
        Rectangle2D bounds = shape.getBounds2D();
        double shapeAspectRatio = bounds.getWidth() / bounds.getHeight();
        double areaWidth = width - 2 * margin;
        double areaHeight = height - 2 * margin;
        double areaAspectRatio = areaWidth / areaHeight;

        Rectangle2D targetArea;
        if (shapeAspectRatio >= areaAspectRatio) {
            double newAreaHeight = areaWidth / shapeAspectRatio;
            double newAreaY = margin + (areaHeight - newAreaHeight) / 2.0;
            targetArea = new Rectangle2D.Double(
                margin + startX,
                newAreaY + startY,
                areaWidth,
                newAreaHeight
            );
        } else {
            double newAreaWidth = areaHeight * shapeAspectRatio;
            double newAreaX = margin + (areaWidth - newAreaWidth) / 2.0;
            targetArea = new Rectangle2D.Double(
                newAreaX + startX,
                margin + startY,
                newAreaWidth,
                areaHeight
            );
        }

        AffineTransform at = RectangularTransform.create(bounds, targetArea);
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
}
