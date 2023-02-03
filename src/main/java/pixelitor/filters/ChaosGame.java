/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam.RangeParamState;
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
import static pixelitor.filters.gui.BooleanParam.BooleanParamState.NO;
import static pixelitor.filters.gui.BooleanParam.BooleanParamState.YES;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * https://en.wikipedia.org/wiki/Chaos_game
 */
public class ChaosGame extends ParametrizedFilter {
    public static final String NAME = "Chaos Game";
    private static final int MARGIN = 5;
    private static final Vertex[] EMPTY_ARRAY = new Vertex[0];

    private static final int COLORS_BW = 1;
    private static final int COLORS_LAST_VERTEX = 2;
    private static final int COLORS_LAST_BUT_ONE = 3;
    private static final int COLORS_LAST_BUT_TWO = 4;

    private final RangeParam numVerticesParam = new RangeParam("Number of Vertices", 3, 3, 10);
    private final RangeParam fraction = new RangeParam("Jump Fraction (%)", 1, 50, 99);
    private final RangeParam iterations = new RangeParam("Iterations (millions)",
        1, 1, 10, true, BORDER, IGNORE_RANDOMIZE);
    private final IntChoiceParam colors = new IntChoiceParam("Colors", new Item[]{
        new Item("None", COLORS_BW),
        new Item("Last Vertex", COLORS_LAST_VERTEX),
        new Item("Last but One", COLORS_LAST_BUT_ONE),
        new Item("Last but Two", COLORS_LAST_BUT_TWO),
    }, IGNORE_RANDOMIZE);
    private final BooleanParam centerJump = new BooleanParam("Jump to Center", false);
    private final BooleanParam midpointJump = new BooleanParam("Jump to Midpoints", false);
    private final BooleanParam restrict = new BooleanParam("No Vertex Repetition", false);
    private final BooleanParam showPoly = new BooleanParam("Show Polygon", false, IGNORE_RANDOMIZE);

    public ChaosGame() {
        super(false);

        setParams(
            numVerticesParam,
            fraction,
            iterations,
            colors,
            centerJump,
            midpointJump,
            restrict,
            showPoly).withAction(ReseedActions.noOpReseed());

        FilterState triangle = new FilterState("Sierpinski Triangle (defaults)")
            .with(numVerticesParam, new RangeParamState(3))
            .with(fraction, new RangeParamState(50))
            .with(centerJump, NO)
            .with(midpointJump, NO)
            .with(restrict, NO);

        FilterState carpet = new FilterState("Sierpinski Carpet")
            .with(numVerticesParam, new RangeParamState(4))
            .with(fraction, new RangeParamState(33.33))
            .with(centerJump, NO)
            .with(midpointJump, YES)
            .with(restrict, NO);

        FilterState vicsek = new FilterState("Vicsek Fractal")
            .with(numVerticesParam, new RangeParamState(4))
            .with(fraction, new RangeParamState(33.33))
            .with(centerJump, YES)
            .with(midpointJump, NO)
            .with(restrict, NO);

        FilterState penta = new FilterState("Pentaflake")
            .with(numVerticesParam, new RangeParamState(5))
            .with(fraction, new RangeParamState(38.1966))
            .with(centerJump, NO)
            .with(midpointJump, NO)
            .with(restrict, NO);

        FilterState hexa = new FilterState("Hexaflake")
            .with(numVerticesParam, new RangeParamState(6))
            .with(fraction, new RangeParamState(33.33))
            .with(centerJump, YES)
            .with(midpointJump, NO)
            .with(restrict, NO);

        paramSet.setBuiltinPresets(triangle, carpet, vicsek, penta, hexa);

        helpURL = "https://en.wikipedia.org/wiki/Chaos_game";
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int numIterations = iterations.getValue() * 1_000_000;
        int workUnit = numIterations / 20;
        int numWorkUnits = 20;
        var pt = new StatusBarProgressTracker(NAME, numWorkUnits);

        int numVertices = numVerticesParam.getValue();
        int colorsValue = colors.getValue();
        int width = dest.getWidth();
        int height = dest.getHeight();
        List<Vertex> vertices = createVertices(numVertices, colorsValue, width, height);

        int[] destPixels = ImageUtils.getPixelArray(dest);
        Arrays.fill(destPixels, 0xFF_FF_FF_FF); // fill the image with white

        Random r = ThreadLocalRandom.current();

        double factor = fraction.getPercentage();
        double factor2 = 1 - factor;

        // start at a random location
        double x = r.nextInt(width);
        double y = r.nextInt(height);

        Vertex[] verticesArray = vertices.toArray(EMPTY_ARRAY);
        int numPoints = vertices.size();

        // throw away the first 50 points
        Vertex last = null;
        Vertex last2 = null;
        for (int i = 0; i < 50; i++) {
            int rand = r.nextInt(numPoints);
            Vertex vertex = verticesArray[rand];
            x = x * factor + vertex.x * factor2;
            y = y * factor + vertex.y * factor2;
            last2 = last;
            last = vertex;
        }

        int counter = 0;
        boolean restrictRepetition = restrict.isChecked();
        for (int i = 0; i < numIterations; i++) {
            int rand = r.nextInt(numPoints);
            Vertex vertex = verticesArray[rand];
            if (restrictRepetition && vertex == last) {
                continue;
            }
            x = x * factor + vertex.x * factor2;
            y = y * factor + vertex.y * factor2;

            int index = (int) x + width * (int) y;

            if (colorsValue == COLORS_LAST_BUT_TWO) {
                destPixels[index] = last2.color;
            } else if (colorsValue == COLORS_LAST_BUT_ONE) {
                destPixels[index] = last.color;
            } else {
                destPixels[index] = vertex.color;
            }
            last2 = last;
            last = vertex;

            counter++;
            if (counter == workUnit) {
                counter = 0;
                pt.unitDone();
            }
        }

        if (showPoly.isChecked()) {
            drawPolygon(dest, vertices, numVertices, colorsValue != COLORS_BW);
        }

        pt.finished();
        return dest;
    }

    private List<Vertex> createVertices(int numVertices, int colorsValue, int width, int height) {
        Vertex.resetMaxMin();
        List<Vertex> vertices = addBasicVertices(numVertices);

        if (midpointJump.isChecked()) {
            addMidPoints(vertices, numVertices);
        }

        if (centerJump.isChecked()) {
            addCenterVertex(vertices, colorsValue);
        }

        colorVertices(vertices, numVertices, colorsValue);
        scaleVertices(vertices, width, height);

        return vertices;
    }

    private static List<Vertex> addBasicVertices(int numVertices) {
        List<Vertex> vertices = new ArrayList<>();
        if (numVertices == 4) {
            // it looks better if the 4 vertices are in the corners
            vertices.add(new Vertex(0, 0));
            vertices.add(new Vertex(0, 1));
            vertices.add(new Vertex(1, 1));
            vertices.add(new Vertex(1, 0));
        } else {
            // arrange the points in a circle
            for (int i = 0; i < numVertices; i++) {
                double x = (1.0 + Math.cos(i * 2 * PI / numVertices - PI / 2)) / 2.0;
                double y = (1.0 + Math.sin(i * 2 * PI / numVertices - PI / 2)) / 2.0;
                vertices.add(new Vertex(x, y));
            }
        }
        return vertices;
    }

    private static void addMidPoints(List<Vertex> vertices, int numVertices) {
        List<Vertex> midPoints = new ArrayList<>();
        for (int i = 0; i < numVertices; i++) {
            Vertex curr = vertices.get(i);
            int prevIndex = i - 1;
            if (prevIndex == -1) {
                prevIndex = numVertices - 1;
            }
            Vertex prev = vertices.get(prevIndex);
            Vertex midPoint = new Vertex((curr.x + prev.x) / 2, (curr.y + prev.y) / 2);
            midPoints.add(midPoint);
        }
        vertices.addAll(midPoints);
    }

    private static void addCenterVertex(List<Vertex> vertices, int colorsValue) {
        Vertex center = new Vertex(0.5, 0.5);
        if (colorsValue == COLORS_BW) {
            center.color = 0xFF_00_00_00;
        } else {
            center.color = 0xFF_00_40_00; // dark green
        }
        vertices.add(center);
    }

    private static void colorVertices(List<Vertex> vertices, int numVertices, int colorsValue) {
        float hue = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vertex point = vertices.get(i);
            if (colorsValue == COLORS_BW) {
                point.color = 0xFF_00_00_00;
            } else {
                if (i < numVertices) {
                    point.color = Color.HSBtoRGB(hue, 0.9f, 0.8f);
                } else {
                    // darker colors for the midpoints and center
                    point.color = Color.HSBtoRGB(hue, 0.9f, 0.4f);
                }

                // https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
                hue += 0.618034f;
            }
        }
    }

    private static void scaleVertices(List<Vertex> vertices, int width, int height) {
        // transform the vertex coordinates from the 0..1 space
        // to the actual image space
        double xRange = Vertex.maxX - Vertex.minX;
        double yRange = Vertex.maxY - Vertex.minY;
        double actualHMargin = Math.min(MARGIN, width / 3.0);
        double actualVMargin = Math.min(MARGIN, height / 3.0);
        double hScale = (width - 2 * actualHMargin) / xRange;
        double vScale = (height - 2 * actualVMargin) / yRange;
        for (Vertex p : vertices) {
            p.x = actualHMargin + hScale * (p.x - Vertex.minX);
            p.y = actualVMargin + vScale * (p.y - Vertex.minY);
        }
    }

    private static void drawPolygon(BufferedImage dest, List<Vertex> vertices,
                                    int numVertices, boolean color) {
        Graphics2D g = dest.createGraphics();
        if (color) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.RED);
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.0f));

        for (int i = 0; i < numVertices; i++) {
            Vertex vertex = vertices.get(i);
            Vertex lastVertex;
            if (i > 0) {
                lastVertex = vertices.get(i - 1);
            } else {
                lastVertex = vertices.get(numVertices - 1);
            }
            g.draw(new Line2D.Double(lastVertex.x, lastVertex.y, vertex.x, vertex.y));
        }
        if (color) {
            for (Vertex p : vertices) {
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
    private static final class Vertex {
        double x;
        double y;
        int color;

        static double minX, minY, maxX, maxY;

        public Vertex(double x, double y) {
            this.x = x;
            this.y = y;

            if (x > maxX) {
                maxX = x;
            }
            if (x < minX) {
                minX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (y < minY) {
                minY = y;
            }
        }

        @Override
        public String toString() {
            return String.format("Vertex (x = %.2f, y = %.2f)", x, y);
        }

        static void resetMaxMin() {
            minX = Double.POSITIVE_INFINITY;
            minY = Double.POSITIVE_INFINITY;
            maxX = Double.NEGATIVE_INFINITY;
            maxY = Double.NEGATIVE_INFINITY;
        }
    }
}
