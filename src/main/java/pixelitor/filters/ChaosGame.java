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

import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam.RangeParamState;
import pixelitor.utils.BoundingBox;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;
import static pixelitor.filters.gui.BooleanParam.BooleanParamState.NO;
import static pixelitor.filters.gui.BooleanParam.BooleanParamState.YES;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.BORDER;

/**
 * https://en.wikipedia.org/wiki/Chaos_game
 */
public class ChaosGame extends ParametrizedFilter {
    public static final String NAME = "Chaos Game";

    @Serial
    private static final long serialVersionUID = 6399413203597332126L;

    private static final int MARGIN = 5;
    private static final Vertex[] EMPTY_ARRAY = new Vertex[0];

    private static final int COLORS_BW = 1;
    private static final int COLORS_LAST_VERTEX = 2;
    private static final int COLORS_LAST_BUT_ONE = 3;
    private static final int COLORS_LAST_BUT_TWO = 4;

    private static final int NUM_WORK_UNITS = 20;
    private static final int ARGB_WHITE = 0xFF_FF_FF_FF;

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
    private final BooleanParam centerJump = new BooleanParam("Jump to Center");
    private final BooleanParam midpointJump = new BooleanParam("Jump to Midpoints");
    private final BooleanParam restrict = new BooleanParam("No Vertex Repetition");
    private final BooleanParam showPoly = new BooleanParam("Show Polygon", false, IGNORE_RANDOMIZE);

    public ChaosGame() {
        super(false);

        initParams(
            numVerticesParam,
            fraction,
            iterations,
            colors,
            centerJump,
            midpointJump,
            restrict,
            showPoly).withReseedAction();

        setupBuiltinPresets();

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Chaos_game");
    }

    private void setupBuiltinPresets() {
        FilterState triangle = new FilterState("Sierpinski Triangle (defaults)")
            .with(numVerticesParam, new RangeParamState(3))
            .with(fraction, new RangeParamState(50))
            .with(centerJump, NO)
            .with(midpointJump, NO)
            .with(restrict, NO);

        FilterState carpet = new FilterState("Sierpinski Carpet")
            .with(numVerticesParam, new RangeParamState(4))
            .with(fraction, new RangeParamState(33.3333)) // jump ratio is 1/3
            .with(centerJump, NO)
            .with(midpointJump, YES)
            .with(restrict, NO);

        FilterState vicsek = new FilterState("Vicsek Fractal")
            .with(numVerticesParam, new RangeParamState(4))
            .with(fraction, new RangeParamState(33.3333)) // jump ratio is 1/3
            .with(centerJump, YES)
            .with(midpointJump, NO)
            .with(restrict, NO);

        FilterState penta = new FilterState("Pentaflake")
            .with(numVerticesParam, new RangeParamState(5))
            .with(fraction, new RangeParamState(38.1966)) // jump ratio is 1 / (2 * cos(π / 5) + 1) = 1 / (φ + 1)
            .with(centerJump, NO)
            .with(midpointJump, NO)
            .with(restrict, NO);

        FilterState hexa = new FilterState("Hexaflake")
            .with(numVerticesParam, new RangeParamState(6))
            .with(fraction, new RangeParamState(33.3333)) // jump ratio is 1/3
            .with(centerJump, YES)
            .with(midpointJump, NO)
            .with(restrict, NO);

        paramSet.setBuiltinPresets(triangle, carpet, vicsek, penta, hexa);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int numIterations = iterations.getValue() * 1_000_000;
        int workUnit = numIterations / NUM_WORK_UNITS;
        var pt = new StatusBarProgressTracker(NAME, NUM_WORK_UNITS);

        int numVertices = numVerticesParam.getValue();
        int colorsValue = colors.getValue();
        int width = dest.getWidth();
        int height = dest.getHeight();

        RandomGenerator random = paramSet.getLastSeedOf("Xoroshiro128PlusPlus");

        List<Vertex> vertices = createVertices(numVertices, colorsValue, width, height);

        int[] destPixels = ImageUtils.getPixels(dest);
        Arrays.fill(destPixels, ARGB_WHITE); // fill the background with white

        double jumpRatio = fraction.getPercentage();
        double remainingRatio = 1 - jumpRatio;

        // start at a random location
        double currentX = random.nextInt(width);
        double currentY = random.nextInt(height);

        Vertex[] verticesArray = vertices.toArray(EMPTY_ARRAY);
        int numPoints = vertices.size();
        boolean restrictRepetition = restrict.isChecked();

        // do 50 iterations without drawing any pixels to ensure that
        // the point moves from its random starting position into the fractal
        // (prevents stray pixels from appearing outside the main pattern)
        Vertex previousVertex = null;
        Vertex secondPreviousVertex = null;
        for (int i = 0; i < 50; i++) {
            Vertex vertex = pickNextVertex(random, verticesArray, numPoints, restrictRepetition, previousVertex);
            currentX = currentX * jumpRatio + vertex.x * remainingRatio;
            currentY = currentY * jumpRatio + vertex.y * remainingRatio;
            secondPreviousVertex = previousVertex;
            previousVertex = vertex;
        }

        int counter = 0;
        for (int i = 0; i < numIterations; i++) {
            Vertex vertex = pickNextVertex(random, verticesArray, numPoints, restrictRepetition, previousVertex);

            // calculate the new point
            currentX = currentX * jumpRatio + vertex.x * remainingRatio;
            currentY = currentY * jumpRatio + vertex.y * remainingRatio;

            // plot the pixel
            int index = (int) currentX + width * (int) currentY;
            destPixels[index] = switch (colorsValue) {
                case COLORS_LAST_BUT_TWO -> secondPreviousVertex.color;
                case COLORS_LAST_BUT_ONE -> previousVertex.color;
                default -> vertex.color;
            };
            secondPreviousVertex = previousVertex;
            previousVertex = vertex;

            if (++counter == workUnit) {
                counter = 0;
                pt.unitDone();
            }
        }

        // render the polygon outline and vertices on top of the generated fractal
        if (showPoly.isChecked()) {
            drawPolygon(dest, vertices, numVertices, colorsValue != COLORS_BW);
        }

        pt.finished();
        return dest;
    }

    /**
     * Picks the next random vertex, optionally applying the no-repetition rule.
     */
    private static Vertex pickNextVertex(RandomGenerator random, Vertex[] verticesArray, int numPoints,
                                         boolean restrictRepetition, Vertex previousVertex) {
        Vertex vertex;
        do {
            int rand = random.nextInt(numPoints);
            vertex = verticesArray[rand];
        } while (restrictRepetition && vertex == previousVertex);
        return vertex;
    }

    /**
     * Creates, colors, and scales all attractor vertices.
     */
    private List<Vertex> createVertices(int numVertices, int colorsValue, int width, int height) {
        List<Vertex> vertices = createPolyCornerVertices(numVertices);

        // add more attractor points based on the user's selection
        if (midpointJump.isChecked()) {
            addMidPoints(vertices, numVertices);
        }
        if (centerJump.isChecked()) {
            vertices.add(new Vertex(0.5, 0.5)); // add central point
        }

        // assign a color to each vertex
        colorVertices(vertices, numVertices, colorsValue);

        BoundingBox bbox = calculateBoundingBox(vertices);
        scaleVerticesToImage(vertices, width, height, bbox);

        return vertices;
    }

    /**
     * Calculates the bounding box for a list of vertices.
     */
    private static BoundingBox calculateBoundingBox(List<Vertex> vertices) {
        BoundingBox bbox = new BoundingBox();
        for (Vertex vertex : vertices) {
            bbox.add(vertex.x, vertex.y);
        }
        return bbox;
    }

    /**
     * Creates the initial corner vertices for the polygon.
     */
    private static List<Vertex> createPolyCornerVertices(int numVertices) {
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

    /**
     * Adds the midpoints of the polygon's edges to the vertex list.
     */
    private static void addMidPoints(List<Vertex> vertices, int numVertices) {
        List<Vertex> midPoints = new ArrayList<>();
        for (int i = 0; i < numVertices; i++) {
            int prevIndex = (i + numVertices - 1) % numVertices;
            Vertex prev = vertices.get(prevIndex);
            Vertex curr = vertices.get(i);
            Vertex midPoint = new Vertex((curr.x + prev.x) / 2, (curr.y + prev.y) / 2);
            midPoints.add(midPoint);
        }
        vertices.addAll(midPoints);
    }

    /**
     * Assigns a unique color to each vertex based on the selected coloring scheme.
     */
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

    /**
     * Transforms vertex coordinates from the normalized 0..1 space to image space.
     */
    private static void scaleVerticesToImage(List<Vertex> vertices, int width, int height, BoundingBox bbox) {
        double horRange = bbox.getMaxX() - bbox.getMinX();
        double verRange = bbox.getMaxY() - bbox.getMinY();
        double actualHMargin = Math.min(MARGIN, width / 3.0);
        double actualVMargin = Math.min(MARGIN, height / 3.0);
        double hScale = (width - 2 * actualHMargin) / horRange;
        double vScale = (height - 2 * actualVMargin) / verRange;
        for (Vertex p : vertices) {
            p.x = actualHMargin + hScale * (p.x - bbox.getMinX());
            p.y = actualVMargin + vScale * (p.y - bbox.getMinY());
        }
    }

    /**
     * Draws the base polygon and its vertices on the destination image.
     */
    private static void drawPolygon(BufferedImage dest, List<Vertex> vertices,
                                    int numVertices, boolean color) {
        Graphics2D g = dest.createGraphics();
        g.setColor(color ? Color.BLACK : Color.RED);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.0f));

        for (int i = 0; i < numVertices; i++) {
            Vertex vertex = vertices.get(i);
            Vertex lastVertex = vertices.get((i + numVertices - 1) % numVertices);
            g.draw(new Line2D.Double(lastVertex.x, lastVertex.y, vertex.x, vertex.y));
        }
        if (color) {
            for (Vertex p : vertices) {
                g.setColor(new Color(p.color));
                Shape circle = Shapes.createCircle(p.x, p.y, MARGIN);
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
     * An attractor point with double precision and an associated color.
     */
    private static final class Vertex {
        double x;
        double y;
        int color;

        public Vertex(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("Vertex (x = %.2f, y = %.2f)", x, y);
        }
    }
}