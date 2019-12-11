/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import pixelitor.guides.GuidesRenderer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;

public class CompositionGuideTest {

    private CompositionGuide compositionGuide;

    @Test
    public void draw_Type_NONE() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 30);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.NONE);
        compositionGuide.draw(rect, g2);

        verify(glRenderer, never()).draw(g2, new ArrayList<>());
    }

    @Test
    public void draw_Type_RULE_OF_THIRDS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.RULE_OF_THIRDS);
        compositionGuide.draw(rect, g2);

        Line2D[] lines = new Line2D[4];
        lines[0] = new Line2D.Double(30, 0, 30, 12);
        lines[1] = new Line2D.Double(60, 0, 60, 12);
        lines[2] = new Line2D.Double(0, 4, 90, 4);
        lines[3] = new Line2D.Double(0, 8, 90, 8);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_GOLDEN_SECTIONS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.GOLDEN_SECTIONS);
        compositionGuide.draw(rect, g2);

        double phi = 1.618;
        double sectionWidth = rect.getWidth() / phi;
        double sectionHeight = rect.getHeight() / phi;

        Line2D[] lines = new Line2D[4];
        lines[0] = new Line2D.Double(sectionWidth, 0, sectionWidth, 12);
        lines[1] = new Line2D.Double(90 - sectionWidth, 0, 90 - sectionWidth, 12);
        lines[2] = new Line2D.Double(0, sectionHeight, 90, sectionHeight);
        lines[3] = new Line2D.Double(0, 12 - sectionHeight, 90, 12 - sectionHeight);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_DIAGONALS_width_gt_height() {

        // rect orientation: width >= height
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.DIAGONALS);
        compositionGuide.draw(rect, g2);

        Line2D[] lines = new Line2D[4];
        lines[0] = new Line2D.Double(0, 0, 12, 12);
        lines[1] = new Line2D.Double(0, 12, 12, 0);
        lines[2] = new Line2D.Double(90, 0, 90 - 12, 12);
        lines[3] = new Line2D.Double(90, 12, 90 - 12, 0);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_DIAGONALS_height_gt_width() {

        // rect orientation: height > width
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 12, 90);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.DIAGONALS);
        compositionGuide.draw(rect, g2);

        Line2D[] lines = new Line2D[4];
        lines[0] = new Line2D.Double(0, 0, 12, 12);
        lines[1] = new Line2D.Double(0, 12, 12, 0);
        lines[2] = new Line2D.Double(0, 90, 12, 90 - 12);
        lines[3] = new Line2D.Double(0, 90 - 12, 12, 90);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_TRIANGLES_top_left_to_bottom_down()
    {
        // orientation: 0 (diagonal line from top left to bottom down)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.TRIANGLES);
        compositionGuide.setOrientation(0);
        compositionGuide.draw(rect, g2);

        Point.Double p = new Point.Double(5,5);
        Line2D[] lines = new Line2D[3];
        lines[0] = new Line2D.Double(0, 0, 10, 10);
        lines[1] = new Line2D.Double(0, 10, p.x, p.y);
        lines[2] = new Line2D.Double(10, 0, p.x, p.y);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_TRIANGLES_bottom_down_to_top_left()
    {
        // orientation: 1 (diagonal line from bottom down to top left)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.TRIANGLES);
        compositionGuide.setOrientation(1);
        compositionGuide.draw(rect, g2);

        Point.Double p = new Point.Double(5,5);
        Line2D[] lines = new Line2D[3];
        lines[0] = new Line2D.Double(0, 10, 10, 0);
        lines[1] = new Line2D.Double(0, 0, p.x, p.y);
        lines[2] = new Line2D.Double(10, 10, p.x, p.y);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_GRID_less_than_2xSize()
    {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 90);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.GRID);
        compositionGuide.draw(rect, g2);

        // cross at the center (gridSize: 50)
        Line2D[] lines = new Line2D[2];
        lines[0] = new Line2D.Double(0, 45, 90, 45);
        lines[1] = new Line2D.Double(45, 0, 45, 90);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_GRID_exact_2xSize()
    {
        // gridSize: 50 (one cross at the center if size less than 2xSize)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 100, 100);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.GRID);
        compositionGuide.draw(rect, g2);

        Line2D[] lines = new Line2D[6];
        // horizontal : cross at the center (gridSize: 50)
        lines[0] = new Line2D.Double(0, 0, 100, 0);
        lines[1] = new Line2D.Double(0, 50, 100, 50);
        lines[2] = new Line2D.Double(0, 100, 100, 100);

        // vertical : cross at the center (gridSize: 50)
        lines[3] = new Line2D.Double(0, 0, 0, 100);
        lines[4] = new Line2D.Double(50, 0, 50, 100);
        lines[5] = new Line2D.Double(100, 0, 100, 100);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_GRID_more_than_2xSize()
    {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 102, 102);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.GRID);
        compositionGuide.draw(rect, g2);

        Line2D[] lines = new Line2D[6];
        // horizontal : cross at the center (gridSize: 50)
        lines[0] = new Line2D.Double(0, 1, 102, 1);
        lines[1] = new Line2D.Double(0, 51, 102, 51);
        lines[2] = new Line2D.Double(0, 101, 102, 101);

        // vertical : cross at the center (gridSize: 50)
        lines[3] = new Line2D.Double(1, 0, 1, 102);
        lines[4] = new Line2D.Double(51, 0, 51, 102);
        lines[5] = new Line2D.Double(101, 0, 101, 102);

        verify(glRenderer).draw(refEq(g2), argThat(new DrawMatcherLine2D(Arrays.asList(lines))));
    }

    @Test
    public void draw_Type_SPIRAL_orientation_0()
    {
        // orientation: 0 (spiral that starts from bottom left)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        Graphics2D g2 = mock(Graphics2D.class);
        GuidesRenderer glRenderer = mock(GuidesRenderer.class);

        compositionGuide = new CompositionGuide(glRenderer);
        compositionGuide.setType(CompositionGuideType.GOLDEN_SPIRAL);
        compositionGuide.setOrientation(0);
        compositionGuide.draw(rect, g2);

        verify(glRenderer).draw(refEq(g2), any());
    }
}

class DrawMatcherLine2D implements ArgumentMatcher<List<Shape>> {

    private final List<Line2D> shapes;

    public DrawMatcherLine2D(List<Line2D> shapes) {
        this.shapes = shapes;
    }

    @Override
    public boolean matches(List<Shape> shapes) {
        for (int i=0; i < shapes.size(); i++) {
            if (shapes.get(i) instanceof Line2D) {
                Line2D line = (Line2D) shapes.get(i);
                Line2D line2 = this.shapes.get(i);
                assertEquals(line.getX1(), line2.getX1(), 1.0e-15);
                assertEquals(line.getY1(), line2.getY1(), 1.0e-15);
                assertEquals(line.getX2(), line2.getX2(), 1.0e-15);
                assertEquals(line.getY2(), line2.getY2(), 1.0e-15);
            }
        }
        return true;
    }
}
