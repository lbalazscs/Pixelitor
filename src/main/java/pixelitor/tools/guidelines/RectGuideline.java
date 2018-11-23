/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.guidelines;

import pixelitor.guides.GuidesRenderer;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Crop guidelines renderer
 */
public class RectGuideline {

    private Graphics2D g2;
    private RectGuidelineType type = RectGuidelineType.NONE;
    private int orientation = 0;
    private GuidesRenderer glRenderer;
    private static final double GOLDEN_RATIO = 1.618;

    public RectGuideline(GuidesRenderer glRenderer) {
        this.glRenderer = glRenderer;
    }

    public RectGuidelineType getType()
    {
        return type;
    }

    public void setType(RectGuidelineType type)
    {
        this.type = type;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation)
    {
        this.orientation = orientation % 4;
    }

    public void draw(Rectangle2D rect, Graphics2D g2)
    {
        this.g2 = g2;
        switch (type) {
            case NONE:
                break;
            case RULE_OF_THIRDS:
                drawRuleOfThirds(rect);
                break;
            case GOLDEN_SECTIONS:
                drawGoldenSections(rect);
                break;
            case GOLDEN_SPIRAL:
                drawGoldenSpiral(rect);
                break;
            case DIAGONALS:
                drawDiagonals(rect);
                break;
            case TRIANGLES:
                drawTriangles(rect);
                break;
            case GRID:
                drawGrid(rect);
                break;
        }
    }

    private void drawShapes(Shape[] shapes)
    {
        this.glRenderer.draw(g2, Arrays.asList(shapes));
    }

    private void drawSections(Rectangle2D rect, double phi)
    {
        double sectionWidth = rect.getWidth() / phi;
        double sectionHeight = rect.getHeight() / phi;
        double x1, x2, y1, y2;
        Line2D[] lines = new Line2D.Double[4];

        // vertical lines
        x1 = (rect.getX() + sectionWidth);
        x2 = (rect.getX() + rect.getWidth() - sectionWidth);
        y1 = rect.getY();
        y2 = (rect.getY() + rect.getHeight());
        lines[0] = new Line2D.Double(x1, y1, x1, y2);
        lines[1] = new Line2D.Double(x2, y1, x2, y2);

        // horizontal lines
        x1 = rect.getX();
        x2 = (rect.getX() + rect.getWidth());
        y1 = (rect.getY() + sectionHeight);
        y2 = (rect.getY() + rect.getHeight() - sectionHeight);
        lines[2] = new Line2D.Double(x1, y1, x2, y1);
        lines[3] = new Line2D.Double(x1, y2, x2, y2);

        drawShapes(lines);
    }

    private void drawRuleOfThirds(Rectangle2D rect)
    {
        drawSections(rect, 3);
    }

    private void drawGoldenSections(Rectangle2D rect)
    {
        drawSections(rect, GOLDEN_RATIO);
    }

    private void drawDiagonals(Rectangle2D rect)
    {
        double x1, x2, y1, y2;
        Line2D[] lines = new Line2D.Double[4];

        if (rect.getWidth() >= rect.getHeight()) {
            y1 = rect.getY();
            y2 = (rect.getY() + rect.getHeight());

            // from left
            x1 = rect.getX();
            x2 = (rect.getX() + rect.getHeight());
            lines[0] = new Line2D.Double(x1, y1, x2, y2);
            lines[1] = new Line2D.Double(x1, y2, x2, y1);

            // from right
            x1 = (rect.getX() + rect.getWidth());
            x2 = (rect.getX() + rect.getWidth() - rect.getHeight());
            lines[2] = new Line2D.Double(x1, y1, x2, y2);
            lines[3] = new Line2D.Double(x1, y2, x2, y1);
        } else {
            x1 = rect.getX();
            x2 = (rect.getX() + rect.getWidth());

            // from top
            y1 = rect.getY();
            y2 = (rect.getY() + rect.getWidth());
            lines[0] = new Line2D.Double(x1, y1, x2, y2);
            lines[1] = new Line2D.Double(x1, y2, x2, y1);

            // from bottom
            y1 = (rect.getY() + rect.getHeight());
            y2 = (rect.getY() + rect.getHeight() - rect.getWidth());
            lines[2] = new Line2D.Double(x1, y1, x2, y2);
            lines[3] = new Line2D.Double(x1, y2, x2, y1);
        }

        drawShapes(lines);
    }

    private void drawGrid(Rectangle2D rect)
    {
        int gridSize = 50;
        int gridCountH = 1 + 2*((int) (rect.getHeight() / 2 / gridSize));
        int gridCountV = 1 + 2*((int) (rect.getWidth() / 2 / gridSize));
        double gridOffsetH = (rect.getHeight() - (gridCountH+1) * gridSize) / 2;
        double gridOffsetV = (rect.getWidth() - (gridCountV+1) * gridSize) / 2;
        Line2D[] lines = new Line2D.Double[gridCountH + gridCountV];

        // horizontal lines (change only y position)
        double x1 = rect.getX();
        double x2 = rect.getX() + rect.getWidth();
        for (int i = 0; i < gridCountH; i++) {
            double yh = rect.getY() + (i+1) * gridSize + gridOffsetH;
            lines[i] = new Line2D.Double(x1, yh,  x2, yh);
        }

        // vertical lines (change only x position)
        double y1 = rect.getY();
        double y2 = rect.getY()+ rect.getHeight();
        for (int i = 0; i < gridCountV; i++) {
            double xv = rect.getX() + (i+1) * gridSize + gridOffsetV;
            lines[gridCountH+i] = new Line2D.Double(xv, y1, xv, y2);
        }

        drawShapes(lines);
    }

    /**
     * Get projected point P' of P on given line
     * @return projected point p.
     */
    private Point2D.Double getProjectedPointOnLine(Line2D line, Point2D.Double p)
    {
        Point2D.Double l1 = (Point2D.Double) line.getP1();
        Point2D.Double l2 = (Point2D.Double) line.getP2();

        // get dot product of vectors v1, v2
        Point2D.Double v1 = new Point2D.Double(l2.x - l1.x, l2.y - l1.y);
        Point2D.Double v2 = new Point2D.Double(p.x - l1.x, p.y - l1.y);
        double d = v1.x * v2.x + v1.y * v2.y;

        // get squared length of vector v1
        double v1Length = v1.x * v1.x + v1.y * v1.y;
        if (v1Length == 0) {
            return l1;
        }

        return new Point2D.Double(
            (int)(l1.x + (d * v1.x) / v1Length),
            (int)(l1.y + (d * v1.y) / v1Length));
    }

    /**
     * Get orthogonal line to given line that pass through point P
     * @return orthogonal line
     */
    private Line2D getOrthogonalLineThroughPoint(Line2D line, Point2D.Double p)
    {
        return new Line2D.Double(p, getProjectedPointOnLine(line, p));
    }

    private void drawTriangles(Rectangle2D rect)
    {
        Line2D[] lines = new Line2D.Double[3];
        double x1, x2, y1, y2;

        if ((orientation % 2) == 0) {
            // diagonal line from top left to bottom right
            x1 = rect.getX();
            y1 = rect.getY();
            x2 = rect.getX() + rect.getWidth();
            y2 = rect.getY() + rect.getHeight();
        } else {
            // diagonal line form bottom left to top right
            x1 = rect.getX();
            y1 = rect.getY() + rect.getHeight();
            x2 = rect.getX() + rect.getWidth();
            y2 = rect.getY();
        }

        lines[0] = new Line2D.Double(x1, y1, x2, y2);
        lines[1] = getOrthogonalLineThroughPoint(lines[0], new Point2D.Double(x1, y2));
        lines[2] = getOrthogonalLineThroughPoint(lines[0], new Point2D.Double(x2, y1));

        drawShapes(lines);
    }

    private void drawGoldenSpiral(Rectangle2D rect)
    {
        Shape[] arc2D = new Arc2D.Double[11];
        double angle;
        double arcWidth = rect.getWidth() / GOLDEN_RATIO;
        double arcHeight = rect.getHeight();
        Point2D.Double center;

        switch (orientation % 4) {
            case 0:
            {
                angle = 180;
                center = new Point2D.Double(rect.getX()+arcWidth, rect.getY() + arcHeight);

                for(int i=0;;) {
                    arc2D[i] = new Arc2D.Double(
                        (center.getX() - arcWidth),
                        (center.getY() - arcHeight),
                        arcWidth*2,
                        arcHeight*2,
                        angle,
                        -90,
                        Arc2D.OPEN);

                    if (++i > 10) break;
                    angle -= 90;
                    arcWidth = arcWidth / GOLDEN_RATIO;
                    arcHeight = arcHeight / GOLDEN_RATIO;
                    center.setLocation(
                        center.getX() + Math.sin( Math.toRadians(  90 - angle ) ) * arcWidth  / GOLDEN_RATIO,
                        center.getY() - Math.sin( Math.toRadians( 180 - angle ) ) * arcHeight / GOLDEN_RATIO
                    );
                }
            }
            break;
            case 1:
            {
                angle = 180;
                center = new Point2D.Double(rect.getX()+arcWidth, rect.getY());

                for(int i=0;;) {
                    arc2D[i] = new Arc2D.Double(
                        (center.getX() - arcWidth),
                        (center.getY() - arcHeight),
                        arcWidth*2,
                        arcHeight*2,
                        angle,
                        90,
                        Arc2D.OPEN);

                    if (++i > 10) break;
                    angle += 90;
                    arcWidth = arcWidth / GOLDEN_RATIO;
                    arcHeight = arcHeight / GOLDEN_RATIO;
                    center.setLocation(
                        center.getX() - Math.sin( Math.toRadians(  -90 + angle ) ) * arcWidth  / GOLDEN_RATIO,
                        center.getY() + Math.sin( Math.toRadians( -180 + angle ) ) * arcHeight / GOLDEN_RATIO
                    );
                }
            }
            break;
            case 2:
            {
                angle = 0;
                center = new Point2D.Double(rect.getX()+(rect.getWidth()-arcWidth), rect.getY() + rect.getHeight());

                for(int i=0;;) {
                    arc2D[i] = new Arc2D.Double(
                        (center.getX() - arcWidth),
                        (center.getY() - arcHeight),
                        arcWidth*2,
                        arcHeight*2,
                        angle,
                        90,
                        Arc2D.OPEN);

                    if (++i > 10) break;
                    angle += 90;
                    arcWidth = arcWidth / GOLDEN_RATIO;
                    arcHeight = arcHeight / GOLDEN_RATIO;
                    center.setLocation(
                        center.getX() + Math.sin( Math.toRadians( 90 + angle ) ) * arcWidth  / GOLDEN_RATIO,
                        center.getY() - Math.sin( Math.toRadians(  0 + angle ) ) * arcHeight / GOLDEN_RATIO
                    );
                }
            }
            break;
            case 3:
            {
                angle = 0;
                center = new Point2D.Double(rect.getX()+(rect.getWidth()-arcWidth), rect.getY());

                for(int i=0;;) {
                    arc2D[i] = new Arc2D.Double(
                        (center.getX() - arcWidth),
                        (center.getY() - arcHeight),
                        arcWidth*2,
                        arcHeight*2,
                        angle,
                        -90,
                        Arc2D.OPEN);

                    if (++i > 10) break;
                    angle -= 90;
                    arcWidth = arcWidth / GOLDEN_RATIO;
                    arcHeight = arcHeight / GOLDEN_RATIO;
                    center.setLocation(
                        center.getX() + Math.sin( Math.toRadians( 90 - angle ) ) * arcWidth  / GOLDEN_RATIO,
                        center.getY() + Math.sin( Math.toRadians(  0 - angle ) ) * arcHeight / GOLDEN_RATIO
                    );
                }
            }
            break;
        }

        drawShapes(arc2D);
    }
}
