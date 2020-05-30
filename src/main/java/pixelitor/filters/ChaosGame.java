/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * https://en.wikipedia.org/wiki/Chaos_game
 */
public class ChaosGame extends ParametrizedFilter {
    public static final String NAME = "Chaos Game";
    private static final int MARGIN = 5;
    private static final Point[] EMPTY_ARRAY = new Point[0];

    private static final int COLORS_BW = 1;
    private static final int COLORS_LAST_VERTEX = 2;
    private static final int COLORS_LAST_BUT_ONE = 3;
    private static final int COLORS_LAST_BUT_TWO = 4;

    private final RangeParam numVerticesParam = new RangeParam("Number of Vertices", 3, 3, 10);
    private final RangeParam fraction = new RangeParam("Jump Fraction (%)", 1, 50, 99);
    private final RangeParam iterations = new RangeParam("Iterations (millions)",
            1, 1, 10, true, BORDER, IGNORE_RANDOMIZE);
    private final IntChoiceParam colors = new IntChoiceParam("Colors", new Value[]{
            new Value("None", COLORS_BW),
            new Value("Last Vertex", COLORS_LAST_VERTEX),
            new Value("Last but One", COLORS_LAST_BUT_ONE),
            new Value("Last but Two", COLORS_LAST_BUT_TWO),
    }, IGNORE_RANDOMIZE);
    private final BooleanParam centerJump = new BooleanParam("Jump to Center", false);
    private final BooleanParam midpointJump = new BooleanParam("Jump to Midpoints", false);
    private final BooleanParam restrict = new BooleanParam("No Vertex Repetition", false);
    private final BooleanParam showPoly = new BooleanParam("Show Polygon", false, IGNORE_RANDOMIZE);

    public ChaosGame() {
        super(ShowOriginal.NO);

        setParams(
                numVerticesParam,
                fraction,
                iterations,
                colors,
                centerJump,
                midpointJump,
                restrict,
                showPoly).withAction(ReseedActions.noOpReseed());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int numIterations = iterations.getValue() * 1_000_000;
        int workUnit = numIterations / 20;
        int numWorkUnits = 20;
        var pt = new StatusBarProgressTracker(NAME, numWorkUnits);

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        int numVertices = numVerticesParam.getValue();
        int colorsValue = colors.getValue();
        List<Point> points = new ArrayList<>();
        if (numVertices == 4) {
            // put the 4 vertices in the corners
            points.add(new Point(0, 0));
            points.add(new Point(0, 1));
            points.add(new Point(1, 1));
            points.add(new Point(1, 0));
            minX = 0;
            minY = 0;
            maxX = 1;
            maxY = 1;
        } else {
            for (int i = 0; i < numVertices; i++) {
                double x = (1.0 + Math.cos(i * 2 * PI / numVertices - PI / 2)) / 2.0;
                if (x > maxX) {
                    maxX = x;
                }
                if (x < minX) {
                    minX = x;
                }

                double y = (1.0 + Math.sin(i * 2 * PI / numVertices - PI / 2)) / 2.0;
                if (y > maxY) {
                    maxY = y;
                }
                if (y < minY) {
                    minY = y;
                }

                Point vertex = new Point(x, y);
                points.add(vertex);
            }
        }

        if(midpointJump.isChecked()) {
            List<Point> midPoints = new ArrayList<>();
            for (int i = 0; i < numVertices; i++) {
                Point curr = points.get(i);
                int prevIndex = i - 1;
                if(prevIndex == -1) {
                    prevIndex = numVertices - 1;
                }
                Point prev =  points.get(prevIndex);
                Point midPoint = new Point((curr.x + prev.x)/2, (curr.y + prev.y)/2);
                midPoints.add(midPoint);
            }
            points.addAll(midPoints);
        }

        if(centerJump.isChecked()) {
            Point center = new Point(0.5, 0.5);
            if(colorsValue == COLORS_BW) {
                center.color = 0xFF_00_00_00;
            } else {
                center.color = 0xFF004000; // dark green
            }
            points.add(center);
        }

        float hue = 0;
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            if(colorsValue == COLORS_BW) {
                point.color = 0xFF_00_00_00;
            } else {
                if(i < numVertices) {
                    point.color = Color.HSBtoRGB(hue, 0.9f, 0.8f);
                } else {
                    // darker colors for the midpoints and center
                    point.color = Color.HSBtoRGB(hue, 0.9f, 0.4f);
                }

                // https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
                hue += 0.618034f;
            }
        }

        double xRange = maxX - minX;
        double yRange = maxY - minY;
        int width = dest.getWidth();
        int height = dest.getHeight();
        double hScale = (width - 2 * MARGIN) / xRange;
        double vScale = (height - 2 * MARGIN) / yRange;
        for (Point p : points) {
            p.x = MARGIN + hScale * (p.x - minX);
            p.y = MARGIN + vScale * (p.y - minY);
        }

        int[] destPixels = ImageUtils.getPixelsAsArray(dest);
        Arrays.fill(destPixels, 0xFF_FF_FF_FF); // fill with white

        Random r = ThreadLocalRandom.current();

        double factor = fraction.getPercentageValD();
        double factor2 = 1 - factor;

        // start at a random location
        double x = r.nextInt(width);
        double y = r.nextInt(height);

        Point[] pointsArray = points.toArray(EMPTY_ARRAY);
        int numPoints = points.size();

        // throw away the first 50 points
        Point last = null;
        Point last2 = null;
        for (int i = 0; i < 50; i++) {
            int rand = r.nextInt(numPoints);
            Point point = pointsArray[rand];
            x = x * factor + point.x * factor2;
            y = y * factor + point.y * factor2;
            last2 = last;
            last = point;
        }

        int counter = 0;
        boolean restrictRepetition = restrict.isChecked();
        for (int i = 0; i < numIterations; i++) {
            int rand = r.nextInt(numPoints);
            Point point = pointsArray[rand];
            if(restrictRepetition && point == last) {
                continue;
            }
            x = x * factor + point.x * factor2;
            y = y * factor + point.y * factor2;

            int index = (int) x + width * (int) y;

            if(colorsValue == COLORS_LAST_BUT_TWO) {
                destPixels[index] = last2.color;
            } else if(colorsValue == COLORS_LAST_BUT_ONE) {
                destPixels[index] = last.color;
            } else {
                destPixels[index] = point.color;
            }
            last2 = last;
            last = point;

            counter++;
            if (counter == workUnit) {
                counter = 0;
                pt.unitDone();
            }
        }

        if (showPoly.isChecked()) {
            drawPolygon(dest, points, numVertices, colorsValue != COLORS_BW);
        }

        pt.finished();
        return dest;
    }

    private static void drawPolygon(BufferedImage dest, List<Point> points, int numVertices, boolean color) {
        Graphics2D g = dest.createGraphics();
        if(color) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.RED);
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.0f));

        for (int i = 0; i < numVertices; i++) {
            Point vertex = points.get(i);
            Point lastVertex;
            if (i > 0) {
                lastVertex = points.get(i - 1);
            } else {
                lastVertex = points.get(numVertices - 1);
            }
            g.draw(new Line2D.Double(lastVertex.x, lastVertex.y, vertex.x, vertex.y));
        }
        if(color) {
            for (Point p : points) {
                g.setColor(new Color(p.color));
                var circle = Shapes.createCircle(p.x, p.y, MARGIN);
                g.fill(circle);
                g.setColor(Color.BLACK);
                g.draw(circle);
            }
        }
        g.dispose();
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    /**
     * A point with double precision and an associated color
     */
    private static final class Point {
        double x;
        double y;
        int color;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
