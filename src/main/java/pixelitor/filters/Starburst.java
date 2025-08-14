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

package pixelitor.filters;

import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * Starburst filter.
 */
public class Starburst extends ParametrizedFilter {
    public static final String NAME = "Starburst";
    private static final int SPIRAL_RESOLUTION = 100;

    @Serial
    private static final long serialVersionUID = 1337459373010709379L;

    private final RangeParam numRaysParam = new RangeParam("Number of Rays", 2, 12, 100);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final ColorParam bgColor = new ColorParam("Background Color", BLACK, MANUAL_ALPHA_ONLY);
    private final ColorListParam rayColorsParam = new ColorListParam("Ray Colors",
        1, 1, WHITE, Colors.CW_RED, Colors.CW_GREEN, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET, Colors.CW_YELLOW);
    private final AngleParam rotate = new AngleParam("Rotate", 0);
    private final RangeParam spiralParam = new RangeParam("Spiral", -200, 0, 200);

    public Starburst() {
        super(false);

        initParams(
            numRaysParam,
            bgColor,
            rayColorsParam,
            center,
            rotate,
            spiralParam
        ).withAction(FilterButtonModel.createExportSvg(this::exportSVG));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        Colors.fillWith(bgColor.getColor(), g, width, height);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        List<ShapeWithColor> shapes = createShapes(width, height);
        for (ShapeWithColor shapeWithColor : shapes) {
            g.setColor(shapeWithColor.color());
            g.fill(shapeWithColor.shape());
        }

        g.dispose();
        return dest;
    }

    /**
     * Creates a list of shapes with their associated colors.
     * Used both for drawing and SVG export.
     */
    private List<ShapeWithColor> createShapes(int width, int height) {
        List<ShapeWithColor> shapes = new ArrayList<>();

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        int numRays = numRaysParam.getValue();
        double rayWidthInRadians = Math.PI / numRays;
        double currentAngle = rotate.getValueInRadians();
        double angleStep = 2 * rayWidthInRadians;

        double radius = width + height; // should be enough even if the center is outside the image
        double spiral = spiralParam.getPercentage();

        Color[] rayColors = rayColorsParam.getColors();
        int numRayColors = rayColors.length;

        for (int i = 0; i < numRays; i++) {
            Shape ray = createRayShape(cx, cy, currentAngle, rayWidthInRadians, radius, spiral);
            shapes.add(new ShapeWithColor(ray, rayColors[i % numRayColors]));
            currentAngle += angleStep;
        }

        return shapes;
    }

    private static Shape createRayShape(double cx, double cy, double startAngle, double rayWidthInRadians, double radius, double spiral) {
        var slice = new Path2D.Double();
        slice.moveTo(cx, cy);

        // first edge of the ray
        if (spiral == 0) {
            double p1x = cx + radius * Math.cos(startAngle);
            double p1y = cy + radius * Math.sin(startAngle);
            slice.lineTo(p1x, p1y);
        } else {
            List<Point2D> points = calcSpiralPathPoints(cx, cy,
                startAngle, radius, spiral);
            Shapes.smoothConnect(points, slice);
        }

        double endAngle = startAngle + rayWidthInRadians;

        // second edge of the ray
        if (spiral == 0) {
            double p2x = cx + radius * Math.cos(endAngle);
            double p2y = cy + radius * Math.sin(endAngle);
            slice.lineTo(p2x, p2y);
        } else {
            List<Point2D> points = calcSpiralPathPoints(cx, cy,
                endAngle, radius, spiral);
            Collections.reverse(points);
            Point2D first = points.getFirst();
            slice.lineTo(first.getX(), first.getY());
            Shapes.smoothConnect(points, slice);
        }
        slice.closePath();
        return slice;
    }

    /**
     * Calculates the points for one curved edge of a spiral ray.
     */
    private static List<Point2D> calcSpiralPathPoints(double cx, double cy,
                                                      double startAngle, double radius,
                                                      double spiral) {
        List<Point2D> points = new ArrayList<>();
        points.add(new Point2D.Double(cx, cy));
        for (int j = 1; j <= SPIRAL_RESOLUTION; j++) {
            double r = j * radius / SPIRAL_RESOLUTION;
            double a = spiral * j / SPIRAL_RESOLUTION;
            double x = cx + r * Math.cos(startAngle + a);
            double y = cy + r * Math.sin(startAngle + a);
            points.add(new Point2D.Double(x, y));
        }
        return points;
    }

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        List<ShapeWithColor> shapes = createShapes(canvas.getWidth(), canvas.getHeight());
        String svgContent = ShapeWithColor.createSvgContent(shapes, canvas, bgColor.getColor());
        FileIO.saveSVG(svgContent, "starburst.svg");
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}