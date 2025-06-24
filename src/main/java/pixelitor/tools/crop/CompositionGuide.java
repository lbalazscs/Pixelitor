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

package pixelitor.tools.crop;

import pixelitor.guides.GuidesRenderer;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import static pixelitor.utils.Geometry.GOLDEN_RATIO;
import static pixelitor.utils.Geometry.createOrthogonalLine;

/**
 * Compositional guides for cropping.
 */
public class CompositionGuide {
    private static final int NUM_SPIRAL_SEGMENTS = 11;
    private static final int GRID_CELL_SIZE = 50;

    private CompositionGuideType type = CompositionGuideType.NONE;
    private int orientation = 0;
    private final GuidesRenderer renderer;

    public CompositionGuide(GuidesRenderer renderer) {
        this.renderer = renderer;
    }

    public void draw(Rectangle2D rect, Graphics2D g) {
        switch (type) {
            case NONE -> {
            }
            case RULE_OF_THIRDS -> drawRuleOfThirds(rect, g);
            case GOLDEN_SECTIONS -> drawGoldenSections(rect, g);
            case GOLDEN_SPIRAL -> drawGoldenSpiral(rect, g);
            case DIAGONALS -> drawDiagonals(rect, g);
            case TRIANGLES -> drawTriangles(rect, g);
            case GRID -> drawGrid(rect, g);
        }
    }

    /**
     * Renders the given shapes using the guides renderer.
     */
    private void renderShapes(Shape[] shapes, Graphics2D g) {
        renderer.draw(g, Arrays.asList(shapes));
    }

    /**
     * Draws equally spaced vertical and horizontal lines dividing
     * the rectangle according to the given ratio.
     */
    private void drawDivisionLines(Rectangle2D rect, double divisionRatio, Graphics2D g) {
        double sectionWidth = rect.getWidth() / divisionRatio;
        double sectionHeight = rect.getHeight() / divisionRatio;
        Line2D[] lines = new Line2D.Double[4];

        // vertical lines
        double x1 = rect.getX() + sectionWidth;
        double x2 = rect.getX() + rect.getWidth() - sectionWidth;
        double y1 = rect.getY();
        double y2 = rect.getY() + rect.getHeight();
        lines[0] = new Line2D.Double(x1, y1, x1, y2);
        lines[1] = new Line2D.Double(x2, y1, x2, y2);

        // horizontal lines
        x1 = rect.getX();
        x2 = rect.getX() + rect.getWidth();
        y1 = rect.getY() + sectionHeight;
        y2 = rect.getY() + rect.getHeight() - sectionHeight;
        lines[2] = new Line2D.Double(x1, y1, x2, y1);
        lines[3] = new Line2D.Double(x1, y2, x2, y2);

        renderShapes(lines, g);
    }

    private void drawRuleOfThirds(Rectangle2D rect, Graphics2D g) {
        drawDivisionLines(rect, 3, g);
    }

    private void drawGoldenSections(Rectangle2D rect, Graphics2D g) {
        drawDivisionLines(rect, GOLDEN_RATIO, g);
    }

    private void drawDiagonals(Rectangle2D rect, Graphics2D g) {
        Line2D[] lines = (rect.getWidth() >= rect.getHeight())
            ? createLandscapeDiagonals(rect)
            : createPortraitDiagonals(rect);

        renderShapes(lines, g);
    }

    private static Line2D[] createLandscapeDiagonals(Rectangle2D rect) {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        return new Line2D.Double[]{
            // left-side diagonals
            new Line2D.Double(x, y, x + height, y + height),
            new Line2D.Double(x, y + height, x + height, y),

            // right-side diagonals
            new Line2D.Double(x + width, y, x + width - height, y + height),
            new Line2D.Double(x + width, y + height, x + width - height, y)
        };
    }

    private static Line2D[] createPortraitDiagonals(Rectangle2D rect) {
        double x = rect.getX();
        double y = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        return new Line2D.Double[]{
            // top diagonals
            new Line2D.Double(x, y, x + width, y + width),
            new Line2D.Double(x, y + width, x + width, y),

            // bottom diagonals
            new Line2D.Double(x, y + height, x + width, y + height - width),
            new Line2D.Double(x, y + height - width, x + width, y + height)
        };
    }

    private void drawGrid(Rectangle2D rect, Graphics2D g) {
        int horLineCount = 1 + 2 * (int) (rect.getHeight() / 2 / GRID_CELL_SIZE);
        int verLineCount = 1 + 2 * (int) (rect.getWidth() / 2 / GRID_CELL_SIZE);

        // calculate offsets to center the grid
        double horOffset = (rect.getHeight() - (horLineCount + 1) * GRID_CELL_SIZE) / 2;
        double verOffset = (rect.getWidth() - (verLineCount + 1) * GRID_CELL_SIZE) / 2;

        Line2D[] lines = new Line2D.Double[horLineCount + verLineCount];

        // horizontal lines
        double startX = rect.getX();
        double endX = rect.getX() + rect.getWidth();
        for (int i = 0; i < horLineCount; i++) {
            double y = rect.getY() + (i + 1) * GRID_CELL_SIZE + horOffset;
            lines[i] = new Line2D.Double(startX, y, endX, y);
        }

        // vertical lines
        double startY = rect.getY();
        double endY = rect.getY() + rect.getHeight();
        for (int i = 0; i < verLineCount; i++) {
            double x = rect.getX() + (i + 1) * GRID_CELL_SIZE + verOffset;
            lines[horLineCount + i] = new Line2D.Double(x, startY, x, endY);
        }

        renderShapes(lines, g);
    }

    private void drawTriangles(Rectangle2D rect, Graphics2D g) {
        double startX, endX, startY, endY;

        if (orientation % 2 == 0) {
            // diagonal line from top left to bottom right
            startX = rect.getX();
            startY = rect.getY();
            endX = rect.getX() + rect.getWidth();
            endY = rect.getY() + rect.getHeight();
        } else {
            // diagonal line form bottom left to top right
            startX = rect.getX();
            startY = rect.getY() + rect.getHeight();
            endX = rect.getX() + rect.getWidth();
            endY = rect.getY();
        }

        Line2D[] lines = new Line2D.Double[3];
        lines[0] = new Line2D.Double(startX, startY, endX, endY);
        lines[1] = createOrthogonalLine(lines[0], new Point2D.Double(startX, endY));
        lines[2] = createOrthogonalLine(lines[0], new Point2D.Double(endX, startY));

        renderShapes(lines, g);
    }

    private void drawGoldenSpiral(Rectangle2D rect, Graphics2D g) {
        Arc2D[] arcs = new Arc2D.Double[NUM_SPIRAL_SEGMENTS];
        double arcWidth = rect.getWidth() / GOLDEN_RATIO;
        double arcHeight = rect.getHeight();

        switch (orientation % 4) {
            case 0 -> createSpiral0(rect, arcs, arcWidth, arcHeight);
            case 1 -> createSpiral1(rect, arcs, arcWidth, arcHeight);
            case 2 -> createSpiral2(rect, arcs, arcWidth, arcHeight);
            case 3 -> createSpiral3(rect, arcs, arcWidth, arcHeight);
        }

        renderShapes(arcs, g);
    }

    private static void createSpiral0(Rectangle2D rect, Arc2D[] arcs, double arcWidth, double arcHeight) {
        double angle = 180;
        Point2D center = new Point2D.Double(rect.getX() + arcWidth, rect.getY() + arcHeight);

        for (int i = 0; i < NUM_SPIRAL_SEGMENTS; i++) {
            arcs[i] = createArc(center, arcWidth, arcHeight, angle, -90);

            angle -= 90;
            arcWidth = arcWidth / GOLDEN_RATIO;
            arcHeight = arcHeight / GOLDEN_RATIO;
            center.setLocation(
                center.getX() + Math.sin(Math.toRadians(90 - angle)) * arcWidth / GOLDEN_RATIO,
                center.getY() - Math.sin(Math.toRadians(180 - angle)) * arcHeight / GOLDEN_RATIO
            );
        }
    }

    private static void createSpiral1(Rectangle2D rect, Arc2D[] arcs, double arcWidth, double arcHeight) {
        double angle = 180;
        Point2D center = new Point2D.Double(rect.getX() + arcWidth, rect.getY());

        for (int i = 0; i < NUM_SPIRAL_SEGMENTS; i++) {
            arcs[i] = createArc(center, arcWidth, arcHeight, angle, 90);

            angle += 90;
            arcWidth = arcWidth / GOLDEN_RATIO;
            arcHeight = arcHeight / GOLDEN_RATIO;
            center.setLocation(
                center.getX() - Math.sin(Math.toRadians(-90 + angle)) * arcWidth / GOLDEN_RATIO,
                center.getY() + Math.sin(Math.toRadians(-180 + angle)) * arcHeight / GOLDEN_RATIO
            );
        }
    }

    private static void createSpiral2(Rectangle2D rect, Arc2D[] arcs, double arcWidth, double arcHeight) {
        double angle = 0;
        Point2D center = new Point2D.Double(rect.getX() + (rect.getWidth() - arcWidth), rect.getY() + rect.getHeight());

        for (int i = 0; i < NUM_SPIRAL_SEGMENTS; i++) {
            arcs[i] = createArc(center, arcWidth, arcHeight, angle, 90);

            angle += 90;
            arcWidth = arcWidth / GOLDEN_RATIO;
            arcHeight = arcHeight / GOLDEN_RATIO;
            center.setLocation(
                center.getX() + Math.sin(Math.toRadians(90 + angle)) * arcWidth / GOLDEN_RATIO,
                center.getY() - Math.sin(Math.toRadians(0 + angle)) * arcHeight / GOLDEN_RATIO
            );
        }
    }

    private static void createSpiral3(Rectangle2D rect, Arc2D[] arcs, double arcWidth, double arcHeight) {
        double angle = 0;
        Point2D center = new Point2D.Double(rect.getX() + (rect.getWidth() - arcWidth), rect.getY());

        for (int i = 0; i < NUM_SPIRAL_SEGMENTS; i++) {
            arcs[i] = createArc(center, arcWidth, arcHeight, angle, -90);

            angle -= 90;
            arcWidth = arcWidth / GOLDEN_RATIO;
            arcHeight = arcHeight / GOLDEN_RATIO;
            center.setLocation(
                center.getX() + Math.sin(Math.toRadians(90 - angle)) * arcWidth / GOLDEN_RATIO,
                center.getY() + Math.sin(Math.toRadians(0 - angle)) * arcHeight / GOLDEN_RATIO
            );
        }
    }

    private static Arc2D createArc(Point2D center,
                                   double width, double height,
                                   double angle, int extent) {
        return new Arc2D.Double(
            center.getX() - width,
            center.getY() - height,
            width * 2,
            height * 2,
            angle,
            extent,
            Arc2D.OPEN);
    }

    public CompositionGuideType getType() {
        return type;
    }

    public void setType(CompositionGuideType type) {
        this.type = type;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation % 4;
    }

    public void setNextOrientation() {
        setOrientation(orientation + 1);
    }
}
