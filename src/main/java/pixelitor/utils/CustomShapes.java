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

import com.jhlabs.image.ImageMath;
import net.jafama.FastMath;
import org.jdesktop.swingx.geom.Star2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;

import static java.awt.Color.WHITE;
import static java.lang.Math.PI;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.sin;

public class CustomShapes {
    private CustomShapes() {
        // prevents instantiation
    }

    /**
     * Creates an arrow shape that can be either horizontal or vertical.
     * The width of the shaft and the size of the arrowhead don't depend
     * on the arrow's length.
     */
    public static Shape createFixedWidthArrow(double startX, double startY, double endX, double endY) {
        // configurable properties
        double arrowWidth = 10.0;  // width of the arrow shaft
        double arrowheadSize = 20.0;  // size of the arrowhead

        Path2D.Double path = new Path2D.Double();

        // calculate direction and length
        double dx = endX - startX;
        double dy = endY - startY;
        boolean isVertical = Math.abs(dy) > Math.abs(dx);

        if (isVertical) { // vertical arrow (pointing up or down)
            double halfWidth = arrowWidth / 2.0;
            double arrowheadBase = endY + (dy > 0 ? -1 : 1) * arrowheadSize;

            path.moveTo(startX - halfWidth, startY);
            path.lineTo(startX - halfWidth, arrowheadBase);
            path.lineTo(startX - arrowheadSize, arrowheadBase);
            path.lineTo(endX, endY);
            path.lineTo(startX + arrowheadSize, arrowheadBase);
            path.lineTo(startX + halfWidth, arrowheadBase);
            path.lineTo(startX + halfWidth, startY);
            path.closePath();
        } else { // horizontal arrow (pointing left or right)
            double halfWidth = arrowWidth / 2.0;
            double arrowheadBase = endX + (dx > 0 ? -1 : 1) * arrowheadSize;

            path.moveTo(startX, startY + halfWidth);
            path.lineTo(arrowheadBase, startY + halfWidth);
            path.lineTo(arrowheadBase, startY + arrowheadSize);
            path.lineTo(endX, endY);
            path.lineTo(arrowheadBase, startY - arrowheadSize);
            path.lineTo(arrowheadBase, startY - halfWidth);
            path.lineTo(startX, startY - halfWidth);
            path.closePath();
        }

        return path;
    }

    public static void drawDirectionArrow(Graphics2D g,
                                          double startX, double startY,
                                          double endX, double endY) {
        Shapes.drawVisibly(g, new Line2D.Double(startX, startY, endX, endY));

        double angle = atan2(endY - startY, endX - startX);

        double backAngle1 = 2.8797926 + angle;
        double backAngle2 = 3.4033926 + angle;
        int arrowRadius = 20;

        double arrowEnd1X = endX + arrowRadius * cos(backAngle1);
        double arrowEnd1Y = endY + arrowRadius * sin(backAngle1);
        double arrowEnd2X = endX + arrowRadius * cos(backAngle2);
        double arrowEnd2Y = endY + arrowRadius * sin(backAngle2);

        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(endX, endY);
        arrowHead.lineTo(arrowEnd1X, arrowEnd1Y);
        arrowHead.lineTo(arrowEnd2X, arrowEnd2Y);
        arrowHead.closePath();
        assert arrowHead != null;

        Shapes.fillVisibly(g, arrowHead, WHITE);
    }

    /**
     * Creates a horizontal, arrow-shaped path around the X axis, with a length of 1.0.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public static Path2D createUnitArrow() {
        double arrowWidth = 0.3;
        double arrowHeadStart = 0.6;

        double halfArrowWidth = arrowWidth / 2.0;
        double halfArrowHeadWidth = Shapes.UNIT_ARROW_HEAD_WIDTH / 2;

        Path2D path = new Path2D.Double();

        path.moveTo(0.0, -halfArrowWidth);
        path.lineTo(0.0, halfArrowWidth);
        path.lineTo(arrowHeadStart, halfArrowWidth);
        path.lineTo(arrowHeadStart, halfArrowHeadWidth);
        path.lineTo(1.0, 0.0);
        path.lineTo(arrowHeadStart, -halfArrowHeadWidth);
        path.lineTo(arrowHeadStart, -halfArrowWidth);
        path.closePath();

        return path;
    }

    public static Shape createCircumscribedPolygon(int n, double cx, double cy, double radius, double tuning) {
        double angleIncrement = PI * 2 / n;
        double maxRadius = radius / cos(angleIncrement / 2);
        double angle = 3 * PI / 2;
        if (n % 2 == 0) {
            angle += angleIncrement / 2;
        }
        Path2D path = new Path2D.Double();
        double prevX = cx + maxRadius * cos(angle);
        double prevY = cy + maxRadius * sin(angle);
        path.moveTo(prevX, prevY);
        for (int i = 0; i < n; i++) {
            angle += angleIncrement;
            double nextX = cx + maxRadius * cos(angle);
            double nextY = cy + maxRadius * sin(angle);
            if (tuning == 0) {
                path.lineTo(nextX, nextY);
            } else {
                double cpX = ImageMath.lerp(tuning + 1, cx, (prevX + nextX) / 2.0);
                double cpY = ImageMath.lerp(tuning + 1, cy, (prevY + nextY) / 2.0);
                path.curveTo(cpX, cpY, cpX, cpY, nextX, nextY);
                prevX = nextX;
                prevY = nextY;
            }
        }
        path.closePath();

        return path;
    }

    public static Shape createFlower(int n, double cx, double cy, double radius, double width) {
        double angleIncrement = PI * 2 / n;
        // 0.45 instead of 0.5 so that the distant radius never goes to infinity
        double halfAngleIncrement = angleIncrement * (0.5 + width * 0.45);
        double angle = -PI / 2;
        double distantRadius = radius / cos(halfAngleIncrement);

        Path2D path = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            path.moveTo(cx, cy);

            double p2X = cx + radius * cos(angle);
            double p2Y = cy + radius * sin(angle);

            double startAngle = angle - halfAngleIncrement;
            double cp2X = cx + distantRadius * cos(startAngle);
            double cp2Y = cy + distantRadius * sin(startAngle);

            // p2 is exactly halfway between cp2 and cp3
            double cp3X = p2X + (p2X - cp2X);
            double cp3Y = p2Y + (p2Y - cp2Y);

            path.curveTo(cx, cy, cp2X, cp2Y, p2X, p2Y);
            path.curveTo(cp3X, cp3Y, cx, cy, cx, cy);

            angle += angleIncrement;
            path.closePath();
        }

        return path;
    }

    public static Shape createSquare(double cx, double cy, double radius) {
        double diameter = 2 * radius;
        return new Rectangle2D.Double(cx - radius, cy - radius, diameter, diameter);
    }

    public static Shape createHexagon(double cx, double cy, double radius) {
        double cos60 = 0.5;
        double sin60 = 0.8660254037844386;
        double rCos60 = radius * cos60;
        double rSin60 = radius * sin60;

        Path2D path = new Path2D.Double();
        path.moveTo(cx + radius, cy);
        path.lineTo(cx + rCos60, cy + rSin60);
        path.lineTo(cx - rCos60, cy + rSin60);
        path.lineTo(cx - radius, cy);
        path.lineTo(cx - rCos60, cy - rSin60);
        path.lineTo(cx + rCos60, cy - rSin60);
        path.lineTo(cx + radius, cy);
        path.closePath();

        return path;
    }

    public static Shape createCircle(Point2D center, double radius) {
        return createCircle(center.getX(), center.getY(), radius);
    }

    /**
     * Creates a circle shape with double precision around the given center.
     */
    public static Shape createCircle(double cx, double cy, double radius) {
        double diameter = 2 * radius;
        return new Ellipse2D.Double(cx - radius, cy - radius, diameter, diameter);
    }

    public static void fillCircle(double cx, double cy, double radius, Color color, Graphics2D g) {
        Shape circle = createCircle(cx, cy, radius);
        g.setColor(color);
        g.fill(circle);
    }

    /**
     * Creates a Bezier path approximating a circle with the given number of control points.
     * Useful if the circle will be distorted in a nonlinear way.
     */
    public static Shape createCircle(double cx, double cy, double radius, int numPoints) {
        // Math at https://stackoverflow.com/questions/1734745/how-to-create-circle-with-b%C3%A9zier-curves
        Path2D path = new Path2D.Double();
        double angle = 0;
        double angleIncrement = 2 * PI / numPoints;
        double handleLength = 4 * radius * FastMath.tan(PI / (2 * numPoints)) / 3;
        Point2D[] points = new Point2D[numPoints];
        Point2D[] forwardControls = new Point2D[numPoints];
        Point2D[] backwardControls = new Point2D[numPoints];

        for (int i = 0; i < numPoints; i++) {
            double cos = cos(angle);
            double sin = sin(angle);
            double x = cx + radius * cos;
            double y = cy + radius * sin;
            angle += angleIncrement;
            points[i] = new Point2D.Double(x, y);
            double handleX = handleLength * sin;
            double handleY = handleLength * cos;
            forwardControls[i] = new Point2D.Double(x - handleX, y + handleY);
            backwardControls[i] = new Point2D.Double(x + handleX, y - handleY);
        }

        for (int i = 0; i < numPoints + 1; i++) {
            Point2D p = i == numPoints ? points[0] : points[i];
            if (i == 0) {
                path.moveTo(p.getX(), p.getY());
            } else {
                Point2D cp1 = forwardControls[i - 1];
                Point2D cp2 = i == numPoints ? backwardControls[0] : backwardControls[i];
                path.curveTo(cp1.getX(), cp1.getY(), cp2.getX(), cp2.getY(), p.getX(), p.getY());
            }
        }

        path.closePath();
        return path;
    }

    /**
     * Creates a circle shape with float precision around the given center.
     */
    public static Shape createCircle(float cx, float cy, float radius) {
        float diameter = 2 * radius;
        return new Ellipse2D.Float(cx - radius, cy - radius, diameter, diameter);
    }

    /**
     * Creates an ellipse shape with double precision around the given center.
     */
    public static Shape createEllipse(double cx, double cy, double radiusX, double radiusY) {
        return new Ellipse2D.Double(cx - radiusX, cy - radiusY, 2 * radiusX, 2 * radiusY);
    }

    /**
     * Creates an ellipse shape with float precision around the given center.
     */
    public static Shape createEllipse(float cx, float cy, float radiusX, float radiusY) {
        return new Ellipse2D.Float(cx - radiusX, cy - radiusY, 2 * radiusX, 2 * radiusY);
    }

    public static Shape createDiamond(double x, double y, double width, double height) {
        Path2D path = new Path2D.Double();

        double cx = x + width / 2.0;
        double cy = y + height / 2.0;

        path.moveTo(cx, y);
        path.lineTo(x + width, cy);
        path.lineTo(cx, y + height);
        path.lineTo(x, cy);
        path.closePath();

        return path;
    }

    public static Shape createStar(int numBranches, double x, double y,
                                   double width, double height, double radiusRatio) {
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        double cx = x + halfWidth;
        double cy = y + halfHeight;

        double outerRadius = Math.max(halfWidth, halfHeight);
        double innerRadius = radiusRatio * outerRadius;

        Shape shape = new Star2D(cx, cy, innerRadius, outerRadius, numBranches);
        if (width != height) {
            double sx = 1.0;
            double sy = 1.0;
            if (width > height) {
                sy = height / width;
            } else {
                sx = width / height;
            }
            AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
            at.scale(sx, sy);
            at.translate(-cx, -cy);
            shape = at.createTransformedShape(shape);
        }
        return shape;
    }

    /**
     * Rabbit shape based on http://commons.wikimedia.org/wiki/File:Lapin01.svg
     */
    public static Shape createRabbit(double x, double y, double width, double height) {
        Path2D path = new Path2D.Float();

        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.0225290f * width;
        epY = y + 0.9892685f * height;
        path.moveTo(epX, epY);

        cp1X = x + -0.024759f * width;
        cp1Y = y + 0.9614873f * height;
        cp2X = x + 0.0071945f * width;
        cp2Y = y + 0.9306156f * height;
        epX = x + 0.0738482f * width;
        epY = y + 0.9396878f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1331521f * width;
        cp1Y = y + 0.9477595f * height;
        cp2X = x + 0.1338864f * width;
        cp2Y = y + 0.9364654f * height;
        epX = x + 0.0784516f * width;
        epY = y + 0.8688863f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + -0.036686f * width;
        cp1Y = y + 0.7285250f * height;
        cp2X = x + 0.0345920f * width;
        cp2Y = y + 0.5265495f * height;
        epX = x + 0.2341483f * width;
        epY = y + 0.4276998f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.3047678f * width;
        cp1Y = y + 0.3927187f * height;
        cp2X = x + 0.4253724f * width;
        cp2Y = y + 0.3631989f * height;
        epX = x + 0.4995234f * width;
        epY = y + 0.3627453f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5905108f * width;
        cp1Y = y + 0.3621887f * height;
        cp2X = x + 0.6123865f * width;
        cp2Y = y + 0.3519972f * height;
        epX = x + 0.6186130f * width;
        epY = y + 0.3072637f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6233143f * width;
        cp1Y = y + 0.2734866f * height;
        cp2X = x + 0.6171384f * width;
        cp2Y = y + 0.2616981f * height;
        epX = x + 0.5839637f * width;
        epY = y + 0.2411260f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5366514f * width;
        cp1Y = y + 0.2117872f * height;
        cp2X = x + 0.5059068f * width;
        cp2Y = y + 0.1660547f * height;
        epX = x + 0.4921512f * width;
        epY = y + 0.1045552f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.4837125f * width;
        cp1Y = y + 0.0668264f * height;
        cp2X = x + 0.4866014f * width;
        cp2Y = y + 0.0593085f * height;
        epX = x + 0.5121517f * width;
        epY = y + 0.0525067f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5285768f * width;
        cp1Y = y + 0.0481343f * height;
        cp2X = x + 0.5554013f * width;
        cp2Y = y + 0.0505655f * height;
        epX = x + 0.5717618f * width;
        epY = y + 0.0579093f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5971791f * width;
        cp1Y = y + 0.0693187f * height;
        cp2X = x + 0.6034718f * width;
        cp2Y = y + 0.0669290f * height;
        epX = x + 0.6150042f * width;
        epY = y + 0.0414879f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6341550f * width;
        cp1Y = y + -0.000759f * height;
        cp2X = x + 0.6568651f * width;
        cp2Y = y + -0.008506f * height;
        epX = x + 0.6893284f * width;
        epY = y + 0.0161347f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7124922f * width;
        cp1Y = y + 0.0337173f * height;
        cp2X = x + 0.7174728f * width;
        cp2Y = y + 0.0539854f * height;
        epX = x + 0.7174728f * width;
        epY = y + 0.1306661f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7174728f * width;
        cp1Y = y + 0.1306661f * height;
        cp2X = x + 0.7068683f * width;
        cp2Y = y + 0.2006619f * height;
        epX = x + 0.7269300f * width;
        epY = y + 0.2174885f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7420615f * width;
        cp1Y = y + 0.2301800f * height;
        cp2X = x + 0.8046360f * width;
        cp2Y = y + 0.2389345f * height;
        epX = x + 0.8046360f * width;
        epY = y + 0.2389345f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9100748f * width;
        cp1Y = y + 0.2572006f * height;
        cp2X = x + 1.0125896f * width;
        cp2Y = y + 0.3273645f * height;
        epX = x + 0.9987402f * width;
        epY = y + 0.3717845f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9888083f * width;
        cp1Y = y + 0.4036398f * height;
        cp2X = x + 0.9524266f * width;
        cp2Y = y + 0.4318752f * height;
        epX = x + 0.9136272f * width;
        epY = y + 0.4378396f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8906142f * width;
        cp1Y = y + 0.4413773f * height;
        cp2X = x + 0.8713374f * width;
        cp2Y = y + 0.4552008f * height;
        epX = x + 0.8641774f * width;
        epY = y + 0.5042721f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8490333f * width;
        cp1Y = y + 0.6080650f * height;
        cp2X = x + 0.8333525f * width;
        cp2Y = y + 0.6571297f * height;
        epX = x + 0.7569181f * width;
        epY = y + 0.7195737f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7002070f * width;
        cp1Y = y + 0.7659046f * height;
        cp2X = x + 0.6829472f * width;
        cp2Y = y + 0.7891283f * height;
        epX = x + 0.6778798f * width;
        epY = y + 0.8259222f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6717813f * width;
        cp1Y = y + 0.8702050f * height;
        cp2X = x + 0.6750328f * width;
        cp2Y = y + 0.8749209f * height;
        epX = x + 0.7327265f * width;
        epY = y + 0.9054682f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7327265f * width;
        cp1Y = y + 0.9054682f * height;
        cp2X = x + 0.7940558f * width;
        cp2Y = y + 0.9208544f * height;
        epX = x + 0.7940558f * width;
        epY = y + 0.9379408f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7940558f * width;
        cp1Y = y + 0.9601508f * height;
        cp2X = x + 0.7312392f * width;
        cp2Y = y + 0.9757761f * height;
        epX = x + 0.7312392f * width;
        epY = y + 0.9757761f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6497790f * width;
        cp1Y = y + 1.0042619f * height;
        cp2X = x + 0.6109745f * width;
        cp2Y = y + 0.9975729f * height;
        epX = x + 0.3687538f * width;
        epY = y + 0.9940703f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.2289030f * width;
        cp1Y = y + 0.9920482f * height;
        cp2X = x + 0.1012021f * width;
        cp2Y = y + 0.9944944f * height;
        epX = x + 0.0849741f * width;
        epY = y + 0.9995067f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.0661758f * width;
        cp1Y = y + 1.0053127f * height;
        cp2X = x + 0.0435150f * width;
        cp2Y = y + 1.0015974f * height;
        epX = x + 0.0225290f * width;
        epY = y + 0.9892685f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.6012331f * width;
        epY = y + 0.9044799f * height;
        path.moveTo(epX, epY);

        cp1X = x + 0.6012331f * width;
        cp1Y = y + 0.8895969f * height;
        cp2X = x + 0.5561252f * width;
        cp2Y = y + 0.8258701f * height;
        epX = x + 0.5455904f * width;
        epY = y + 0.8258701f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5393719f * width;
        cp1Y = y + 0.8258701f * height;
        cp2X = x + 0.5121032f * width;
        cp2Y = y + 0.8450645f * height;
        epX = x + 0.4849935f * width;
        epY = y + 0.8685243f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.4357029f * width;
        epY = y + 0.9111785f * height;
        path.lineTo(epX, epY);

        epX = x + 0.5184681f * width;
        epY = y + 0.9111785f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.5639889f * width;
        cp1Y = y + 0.9111785f * height;
        cp2X = x + 0.6012331f * width;
        cp2Y = y + 0.9081641f * height;
        epX = x + 0.6012331f * width;
        epY = y + 0.9044799f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        path.closePath();
        return path;
    }

    /**
     * Bat shape based on http://en.wikipedia.org/wiki/File:Bat_shadow_black.svg
     */
    public static Shape createBat(double x, double y, double width, double height) {
        Path2D path = new Path2D.Float();

        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.48396146f * width;
        epY = y + 0.8711912f * height;
        path.moveTo(epX, epY);

        epX = x + 0.48396146f * width;
        epY = y + 0.8711912f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.46530184f * width;
        cp1Y = y + 0.8309665f * height;
        cp2X = x + 0.38263357f * width;
        cp2Y = y + 0.8160471f * height;
        epX = x + 0.3731808f * width;
        epY = y + 0.84056836f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.35756963f * width;
        cp1Y = y + 0.88106513f * height;
        cp2X = x + 0.34080854f * width;
        cp2Y = y + 0.9193152f * height;
        epX = x + 0.36195248f * width;
        epY = y + 0.83724654f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.3386589f * width;
        cp1Y = y + 0.8935222f * height;
        cp2X = x + 0.3420269f * width;
        cp2Y = y + 0.8584848f * height;
        epX = x + 0.36583346f * width;
        epY = y + 0.79526377f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.36583346f * width;
        cp1Y = y + 0.79526377f * height;
        cp2X = x + 0.39358535f * width;
        cp2Y = y + 0.7799532f * height;
        epX = x + 0.38822776f * width;
        epY = y + 0.7415569f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.38251382f * width;
        cp1Y = y + 0.7006066f * height;
        cp2X = x + 0.30032519f * width;
        cp2Y = y + 0.59342885f * height;
        epX = x + 0.23370443f * width;
        epY = y + 0.61983544f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.15583336f * width;
        cp1Y = y + 0.6507012f * height;
        cp2X = x + 0.18591423f * width;
        cp2Y = y + 0.6526183f * height;
        epX = x + 0.16175953f * width;
        epY = y + 0.58064216f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1528414f * width;
        cp1Y = y + 0.55406797f * height;
        cp2X = x + 0.14705019f * width;
        cp2Y = y + 0.5273279f * height;
        epX = x + 0.07419801f * width;
        epY = y + 0.54101586f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.06698244f * width;
        cp1Y = y + 0.44892874f * height;
        cp2X = x + 0.058711186f * width;
        cp2Y = y + 0.38432577f * height;
        epX = x + 0.0079061575f * width;
        epY = y + 0.35636365f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.15172054f * width;
        cp1Y = y + 0.14081612f * height;
        cp2X = x + 0.2041543f * width;
        cp2Y = y + 0.14397591f * height;
        epX = x + 0.20349151f * width;
        epY = y + 0.13025562f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20031318f * width;
        cp1Y = y + 0.0644616f * height;
        cp2X = x + 0.19046022f * width;
        cp2Y = y + -0.01011441f * height;
        epX = x + 0.21284555f * width;
        epY = y + 0.12160087f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.21233447f * width;
        cp1Y = y + 0.007110184f * height;
        cp2X = x + 0.20867635f * width;
        cp2Y = y + 0.04029033f * height;
        epX = x + 0.22205064f * width;
        epY = y + 0.12669845f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.22513902f * width;
        cp1Y = y + 0.1466518f * height;
        cp2X = x + 0.28788444f * width;
        cp2Y = y + 0.21163544f * height;
        epX = x + 0.33955792f * width;
        epY = y + 0.23622294f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.41069368f * width;
        cp1Y = y + 0.27007103f * height;
        cp2X = x + 0.43251768f * width;
        cp2Y = y + 0.30622476f * height;
        epX = x + 0.43251768f * width;
        epY = y + 0.28272754f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.43251768f * width;
        cp1Y = y + 0.2655886f * height;
        cp2X = x + 0.42556417f * width;
        cp2Y = y + 0.2189889f * height;
        epX = x + 0.41960958f * width;
        epY = y + 0.21069744f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.40620282f * width;
        cp1Y = y + 0.19202933f * height;
        cp2X = x + 0.39478126f * width;
        cp2Y = y + 0.056988757f * height;
        epX = x + 0.41154054f * width;
        epY = y + 0.028444579f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.41581085f * width;
        cp1Y = y + 0.018658942f * height;
        cp2X = x + 0.50143397f * width;
        cp2Y = y + 0.16836204f * height;
        epX = x + 0.50143397f * width;
        epY = y + 0.16836204f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.50143397f * width;
        cp1Y = y + 0.16836204f * height;
        cp2X = x + 0.5785536f * width;
        cp2Y = y + 0.0273761f * height;
        epX = x + 0.5868916f * width;
        epY = y + 0.019599283f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5989375f * width;
        cp1Y = y + 0.04062414f * height;
        cp2X = x + 0.60303134f * width;
        cp2Y = y + 0.12733895f * height;
        epX = x + 0.5784455f * width;
        epY = y + 0.20690775f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5733677f * width;
        cp1Y = y + 0.22334124f * height;
        cp2X = x + 0.5701417f * width;
        cp2Y = y + 0.25919494f * height;
        epX = x + 0.5701417f * width;
        epY = y + 0.27444258f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5701417f * width;
        cp1Y = y + 0.3071336f * height;
        cp2X = x + 0.6937292f * width;
        cp2Y = y + 0.24839245f * height;
        epX = x + 0.7691606f * width;
        epY = y + 0.14076737f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7923608f * width;
        cp1Y = y + 0.107665494f * height;
        cp2X = x + 0.77971613f * width;
        cp2Y = y + -0.0058511556f * height;
        epX = x + 0.7865298f * width;
        epY = y + 0.114005655f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8027102f * width;
        cp1Y = y + 0.0111046815f * height;
        cp2X = x + 0.79922926f * width;
        cp2Y = y + 0.052270394f * height;
        epX = x + 0.7920877f * width;
        epY = y + 0.12626775f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.79057056f * width;
        cp1Y = y + 0.14198744f * height;
        cp2X = x + 0.899306f * width;
        cp2Y = y + 0.17557377f * height;
        epX = x + 0.9920938f * width;
        epY = y + 0.3866074f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.9691352f * width;
        cp1Y = y + 0.3866074f * height;
        cp2X = x + 0.9349961f * width;
        cp2Y = y + 0.3966509f * height;
        epX = x + 0.9280588f * width;
        epY = y + 0.5432177f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8615748f * width;
        cp1Y = y + 0.5425939f * height;
        cp2X = x + 0.8545972f * width;
        cp2Y = y + 0.5403216f * height;
        epX = x + 0.82650656f * width;
        epY = y + 0.63398916f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7509983f * width;
        cp1Y = y + 0.6200947f * height;
        cp2X = x + 0.67899865f * width;
        cp2Y = y + 0.61476916f * height;
        epX = x + 0.64566225f * width;
        epY = y + 0.6818779f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.609466f * width;
        epY = y + 0.7547435f * height;
        path.lineTo(epX, epY);

        epX = x + 0.6363375f * width;
        epY = y + 0.80627334f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.6519878f * width;
        cp1Y = y + 0.8362852f * height;
        cp2X = x + 0.6647533f * width;
        cp2Y = y + 0.88090324f * height;
        epX = x + 0.63656145f * width;
        epY = y + 0.83025086f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6401184f * width;
        cp1Y = y + 0.9251861f * height;
        cp2X = x + 0.64008677f * width;
        cp2Y = y + 0.87504876f * height;
        epX = x + 0.62064874f * width;
        epY = y + 0.8233091f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.59059566f * width;
        cp1Y = y + 0.823376f * height;
        cp2X = x + 0.5248581f * width;
        cp2Y = y + 0.834002f * height;
        epX = x + 0.5188337f * width;
        epY = y + 0.874816f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.49758786f * width;
        cp1Y = y + 1.0187525f * height;
        cp2X = x + 0.49775344f * width;
        cp2Y = y + 1.0135795f * height;
        epX = x + 0.48396146f * width;
        epY = y + 0.8711912f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        path.closePath();
        return path;
    }

    /**
     * Cat shape based on http://commons.wikimedia.org/wiki/File:Cat_silhouette.svg
     */
    public static Shape createCat(double x, double y, double width, double height) {
        Path2D path = new Path2D.Float();

        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.3783726f * width;
        epY = y + 0.80843306f * height;
        path.moveTo(epX, epY);

        epX = x + 0.3783726f * width;
        epY = y + 0.80843306f * height;
        path.lineTo(epX, epY);

        epX = x + 0.6608726f * width;
        epY = y + 0.80843306f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.6608726f * width;
        cp1Y = y + 0.7661604f * height;
        cp2X = x + 0.65800524f * width;
        cp2Y = y + 0.7577751f * height;
        epX = x + 0.5914976f * width;
        epY = y + 0.7577751f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6021226f * width;
        cp1Y = y + 0.72422254f * height;
        cp2X = x + 0.6453106f * width;
        cp2Y = y + 0.6430476f * height;
        epX = x + 0.6693101f * width;
        epY = y + 0.6430476f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6905427f * width;
        cp1Y = y + 0.6430476f * height;
        cp2X = x + 0.7158726f * width;
        cp2Y = y + 0.6442787f * height;
        epX = x + 0.7158726f * width;
        epY = y + 0.6952751f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7158726f * width;
        cp1Y = y + 0.7531699f * height;
        cp2X = x + 0.80842924f * width;
        cp2Y = y + 0.8465144f * height;
        epX = x + 0.8308726f * width;
        epY = y + 0.80843306f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8643192f * width;
        cp1Y = y + 0.7516815f * height;
        cp2X = x + 0.7733726f * width;
        cp2Y = y + 0.76898724f * height;
        epX = x + 0.7733726f * width;
        epY = y + 0.6321172f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7733726f * width;
        cp1Y = y + 0.44487196f * height;
        cp2X = x + 0.87718546f * width;
        cp2Y = y + 0.47152817f * height;
        epX = x + 0.87718546f * width;
        epY = y + 0.36632773f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.87718546f * width;
        cp1Y = y + 0.31369618f * height;
        cp2X = x + 0.86337256f * width;
        cp2Y = y + 0.3065888f * height;
        epX = x + 0.86337256f * width;
        epY = y + 0.26895934f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.86337256f * width;
        cp1Y = y + 0.21922882f * height;
        cp2X = x + 0.9049929f * width;
        cp2Y = y + 0.22211468f * height;
        epX = x + 0.8968952f * width;
        epY = y + 0.18088126f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8913015f * width;
        cp1Y = y + 0.1523991f * height;
        cp2X = x + 0.886924f * width;
        cp2Y = y + 0.12955788f * height;
        epX = x + 0.8836629f * width;
        epY = y + 0.0951155f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.88134897f * width;
        cp1Y = y + 0.07067712f * height;
        cp2X = x + 0.88061124f * width;
        cp2Y = y + 0.044629995f * height;
        epX = x + 0.85649633f * width;
        epY = y + 0.04567732f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8281626f * width;
        cp1Y = y + 0.046907872f * height;
        cp2X = x + 0.8174137f * width;
        cp2Y = y + 0.09940576f * height;
        epX = x + 0.7733726f * width;
        epY = y + 0.103169866f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7294213f * width;
        cp1Y = y + 0.10692629f * height;
        cp2X = x + 0.69194585f * width;
        cp2Y = y + 0.06378429f * height;
        epX = x + 0.6749351f * width;
        epY = y + 0.069946185f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6580677f * width;
        cp1Y = y + 0.076056145f * height;
        cp2X = x + 0.6633726f * width;
        cp2Y = y + 0.124222495f * height;
        epX = x + 0.67337257f * width;
        epY = y + 0.15843302f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6890897f * width;
        cp1Y = y + 0.21220203f * height;
        cp2X = x + 0.7233726f * width;
        cp2Y = y + 0.28211725f * height;
        epX = x + 0.6608726f * width;
        epY = y + 0.29264355f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.59837264f * width;
        cp1Y = y + 0.30316988f * height;
        cp2X = x + 0.49087262f * width;
        cp2Y = y + 0.31369618f * height;
        epX = x + 0.4133726f * width;
        epY = y + 0.4215909f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.33587262f * width;
        cp1Y = y + 0.52948564f * height;
        cp2X = x + 0.33873355f * width;
        cp2Y = y + 0.65610844f * height;
        epX = x + 0.3083726f * width;
        epY = y + 0.6952751f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20647675f * width;
        cp1Y = y + 0.82672447f * height;
        cp2X = x + 0.1020598f * width;
        cp2Y = y + 0.77275324f * height;
        epX = x + 0.1020598f * width;
        epY = y + 0.8821173f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1020598f * width;
        cp1Y = y + 0.93107945f * height;
        cp2X = x + 0.18087262f * width;
        cp2Y = y + 0.9663278f * height;
        epX = x + 0.1933726f * width;
        epY = y + 0.95053834f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.20587261f * width;
        cp1Y = y + 0.9347488f * height;
        cp2X = x + 0.08715942f * width;
        cp2Y = y + 0.88769966f * height;
        epX = x + 0.21998873f * width;
        epY = y + 0.83211726f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.33346778f * width;
        cp1Y = y + 0.7846319f * height;
        cp2X = x + 0.34359783f * width;
        cp2Y = y + 0.77493846f * height;
        epX = x + 0.3783726f * width;
        epY = y + 0.80843306f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        path.closePath();

        return path;
    }

    public static Path2D createHeart(double x, double y, double width, double height) {
        Path2D path = new Path2D.Float();

        double maxX = x + width;

        double centerX = x + width / 2.0f;
        double bottomY = y + height;

        double cp1XRight = x + 0.58f * width;
        double cp1XLeft = x + 0.42f * width;
        double cp1Y = y + 0.78f * height;

        double cp2Y = y + 0.6f * height;
        double sideY = y + 0.3f * height;
        double cp3Y = y + 0.18f * height;
        double cp4XRight = x + 0.9f * width;
        double cp4XLeft = x + 0.1f * width;

        double cp5XRight = x + 0.6f * width;
        double cp5XLeft = x + 0.4f * width;
        double topCenterY = y + 0.18f * height;

        double topXRight = x + 0.75f * width;
        double topXLeft = x + 0.25f * width;

        path.moveTo(centerX, bottomY);
        // right side
        path.curveTo(cp1XRight, cp1Y, // control point 1
            maxX, cp2Y,    // control point 2
            maxX, sideY); // side point
        path.curveTo(maxX, cp3Y, // control point 3
            cp4XRight, y, // control point 4
            topXRight, y); // top point
        path.curveTo(cp5XRight, y, // control point 5
            centerX, topCenterY,  // this control point is the same as the following endpoint
            centerX, topCenterY); // top center point
        // left side
        path.curveTo(centerX, topCenterY,   // this control point is the same as the start point
            cp5XLeft, y, // left mirror of control point 5
            topXLeft, y);
        path.curveTo(cp4XLeft, y,
            x, cp3Y,
            x, sideY);
        path.curveTo(x, cp2Y,
            cp1XLeft, cp1Y,
            centerX, bottomY);

        path.closePath();
        return path;
    }

    /**
     * Kiwi shape based on http://en.wikipedia.org/wiki/File:Kiwi_silhouette-by-flomar.svg
     */
    public static Shape createKiwi(double x, double y, double width, double height) {
        Path2D path = new Path2D.Float();

        double cp1X; // x of control point 1
        double cp1Y; // y of control point 1
        double cp2X; // x of control point 2
        double cp2Y; // y of control point 2
        double epX;  // x of end point
        double epY;  // y of end point

        epX = x + 0.3201992f * width;
        epY = y + 0.4113421f * height;
        path.moveTo(epX, epY);

        cp1X = x + 0.3201992f * width;
        cp1Y = y + 0.4113421f * height;
        cp2X = x + 0.3886100f * width;
        cp2Y = y + 0.6210994f * height;
        epX = x + 0.6166460f * width;
        epY = y + 0.6810300f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        epX = x + 0.5979885f * width;
        epY = y + 0.8349427f * height;
        path.lineTo(epX, epY);

        cp1X = x + 0.5555098f * width;
        cp1Y = y + 0.8410811f * height;
        cp2X = x + 0.4960748f * width;
        cp2Y = y + 0.8598133f * height;
        epX = x + 0.5013717f * width;
        epY = y + 0.9341682f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5153917f * width;
        cp1Y = y + 0.9033809f * height;
        cp2X = x + 0.5378418f * width;
        cp2Y = y + 0.8967044f * height;
        epX = x + 0.5627466f * width;
        epY = y + 0.8894251f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5616206f * width;
        cp1Y = y + 0.9079770f * height;
        cp2X = x + 0.5613357f * width;
        cp2Y = y + 0.9263459f * height;
        epX = x + 0.5623692f * width;
        epY = y + 0.9448706f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5757225f * width;
        cp1Y = y + 0.9125036f * height;
        cp2X = x + 0.5948789f * width;
        cp2Y = y + 0.8921492f * height;
        epX = x + 0.6166460f * width;
        epY = y + 0.8880631f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6298102f * width;
        cp1Y = y + 0.8855920f * height;
        cp2X = x + 0.6456055f * width;
        cp2Y = y + 0.8902638f * height;
        epX = x + 0.6493103f * width;
        epY = y + 0.8818314f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6532299f * width;
        cp1Y = y + 0.8729106f * height;
        cp2X = x + 0.6404861f * width;
        cp2Y = y + 0.8699734f * height;
        epX = x + 0.6404861f * width;
        epY = y + 0.8608218f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6404861f * width;
        cp1Y = y + 0.8376669f * height;
        cp2X = x + 0.6467052f * width;
        cp2Y = y + 0.7477709f * height;
        epX = x + 0.6767646f * width;
        epY = y + 0.7055470f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6767646f * width;
        cp1Y = y + 0.7055470f * height;
        cp2X = x + 0.7462119f * width;
        cp2Y = y + 0.6960126f * height;
        epX = x + 0.7555406f * width;
        epY = y + 0.7859087f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7648693f * width;
        cp1Y = y + 0.8758046f * height;
        cp2X = x + 0.7451753f * width;
        cp2Y = y + 0.8867011f * height;
        epX = x + 0.7358466f * width;
        epY = y + 0.8921492f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7031188f * width;
        cp1Y = y + 0.9122557f * height;
        cp2X = x + 0.6401039f * width;
        cp2Y = y + 0.9133864f * height;
        epX = x + 0.6230900f * width;
        epY = y + 0.9638219f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.6498552f * width;
        cp1Y = y + 0.9548539f * height;
        cp2X = x + 0.6771716f * width;
        cp2Y = y + 0.9520250f * height;
        epX = x + 0.7047508f * width;
        epY = y + 0.9507179f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7014105f * width;
        cp1Y = y + 0.9670282f * height;
        cp2X = x + 0.6977268f * width;
        cp2Y = y + 0.9831436f * height;
        epX = x + 0.6964999f * width;
        epY = y + 1.0f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7110112f * width;
        cp1Y = y + 0.9904656f * height;
        cp2X = x + 0.7431022f * width;
        cp2Y = y + 0.9602523f * height;
        epX = x + 0.7752346f * width;
        epY = y + 0.9561661f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7899039f * width;
        cp1Y = y + 0.9543007f * height;
        cp2X = x + 0.8067132f * width;
        cp2Y = y + 0.9595316f * height;
        epX = x + 0.8107810f * width;
        epY = y + 0.9502732f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8138921f * width;
        cp1Y = y + 0.9431922f * height;
        cp2X = x + 0.7940724f * width;
        cp2Y = y + 0.9360016f * height;
        epX = x + 0.7918191f * width;
        epY = y + 0.9071320f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.7876730f * width;
        cp1Y = y + 0.8540115f * height;
        cp2X = x + 0.8001113f * width;
        cp2Y = y + 0.8008913f * height;
        epX = x + 0.8498646f * width;
        epY = y + 0.7368744f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8996180f * width;
        cp1Y = y + 0.6728576f * height;
        cp2X = x + 0.9721922f * width;
        cp2Y = y + 0.6497447f * height;
        epX = x + 0.9918688f * width;
        epY = y + 0.4868325f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 1.0136793f * width;
        cp1Y = y + 0.3062516f * height;
        cp2X = x + 0.9873011f * width;
        cp2Y = y + 0.2081411f * height;
        epX = x + 0.9253203f * width;
        epY = y + 0.1288647f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.8632650f * width;
        cp1Y = y + 0.0494930f * height;
        cp2X = x + 0.7348101f * width;
        cp2Y = y + 0.0027241f * height;
        epX = x + 0.6601802f * width;
        epY = y + 0.0f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.5855501f * width;
        cp1Y = y + -0.002724f * height;
        cp2X = x + 0.5357969f * width;
        cp2Y = y + 0.0081724f * height;
        epX = x + 0.4590939f * width;
        epY = y + 0.0572065f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.3823908f * width;
        cp1Y = y + 0.1062407f * height;
        cp2X = x + 0.3740986f * width;
        cp2Y = y + 0.1661713f * height;
        epX = x + 0.3139800f * width;
        epY = y + 0.1579989f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1685614f * width;
        cp1Y = y + 0.1297290f * height;
        cp2X = x + 0.1173945f * width;
        cp2Y = y + 0.1811683f * height;
        epX = x + 0.1066745f * width;
        epY = y + 0.3663941f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1077111f * width;
        cp1Y = y + 0.4508418f * height;
        cp2X = x + 0.1077111f * width;
        cp2Y = y + 0.4508418f * height;
        epX = x + 0.1077111f * width;
        epY = y + 0.4508418f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1077111f * width;
        cp1Y = y + 0.4508418f * height;
        cp2X = x + 0.0163014f * width;
        cp2Y = y + 0.5921931f * height;
        epX = x + 0.0030218f * width;
        epY = y + 0.7886327f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.0030218f * width;
        cp1Y = y + 0.7886327f * height;
        cp2X = x + -0.004829f * width;
        cp2Y = y + 0.8463501f * height;
        epX = x + 0.0075935f * width;
        epY = y + 0.8707875f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.0290599f * width;
        cp1Y = y + 0.7727283f * height;
        cp2X = x + 0.0542405f * width;
        cp2Y = y + 0.6017497f * height;
        epX = x + 0.1451244f * width;
        epY = y + 0.4803964f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        cp1X = x + 0.1451244f * width;
        cp1Y = y + 0.4803964f * height;
        cp2X = x + 0.2389402f * width;
        cp2Y = y + 0.4977325f * height;
        epX = x + 0.3201992f * width;
        epY = y + 0.4113421f * height;
        path.curveTo(cp1X, cp1Y, cp2X, cp2Y, epX, epY);

        path.closePath();
        return path;
    }
}
