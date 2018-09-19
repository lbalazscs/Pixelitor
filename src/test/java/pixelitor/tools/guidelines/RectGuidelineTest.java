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

import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.*;

public class RectGuidelineTest {

    private RectGuideline rectGuideline;

    @Test
    public void draw_Type_NONE() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 30);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.NONE);
        rectGuideline.draw(rect, g2);

        verify(g2, never()).draw(any(Line2D.class));
    }


    @Test
    public void draw_Type_RULE_OF_THIRDS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.RULE_OF_THIRDS);
        rectGuideline.draw(rect, g2);

        verify(g2, times(2)).draw(refEq(new Line2D.Double(30, 0, 30, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(60, 0, 60, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 4, 90, 4)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 8, 90, 8)));
    }

    @Test
    public void draw_Type_GOLDEN_SECTIONS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.GOLDEN_SECTIONS);
        rectGuideline.draw(rect, g2);

        double phi = 1.618;
        double sectionWidth = rect.getWidth() / phi;
        double sectionHeight = rect.getHeight() / phi;

        verify(g2, times(2)).draw(refEq(new Line2D.Double(sectionWidth, 0, sectionWidth, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(90 - sectionWidth, 0, 90 - sectionWidth, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, sectionHeight, 90, sectionHeight)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 12 - sectionHeight, 90, 12 - sectionHeight)));
    }

    @Test
    public void draw_Type_DIAGONALS_width_gt_height() {

        // rect orientation: width >= height
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.DIAGONALS);
        rectGuideline.draw(rect, g2);

        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, 12, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 12, 12, 0)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(90, 0, 90 - 12, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(90, 12, 90 - 12, 0)));
    }

    @Test
    public void draw_Type_DIAGONALS_height_gt_width() {

        // rect orientation: height > width
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 12, 90);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.DIAGONALS);
        rectGuideline.draw(rect, g2);

        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, 12, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 12, 12, 0)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 90, 12, 90 - 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 90 - 12, 12, 90)));
    }

    @Test
    public void draw_Type_TRIANGLES_top_left_to_bottom_down()
    {

        // orientation: 0 (diagonal line from top left to bottom down)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.TRIANGLES);
        rectGuideline.setOrientation(0);
        rectGuideline.draw(rect, g2);

        Point.Double p = new Point.Double(5,5);
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, 10, 10)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 10, p.x, p.y)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(10, 0, p.x, p.y)));
    }

    @Test
    public void draw_Type_TRIANGLES_bottom_down_to_top_left()
    {
        // orientation: 1 (diagonal line from bottom down to top left)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.TRIANGLES);
        rectGuideline.setOrientation(1);
        rectGuideline.draw(rect, g2);

        Point.Double p = new Point.Double(5,5);
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 10, 10, 0)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, p.x, p.y)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(10, 10, p.x, p.y)));
    }

    @Test
    public void draw_Type_GRID_less_than_2xSize()
    {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 90);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.GRID);
        rectGuideline.draw(rect, g2);

        // cross at the center (gridSize: 50)
        verify(g2, times(2)).draw(refEq(new Line2D.Double(45, 0, 45, 90)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 45, 90, 45)));
        // total
        verify(g2, atMost(4)).draw(any());
    }

    @Test
    public void draw_Type_GRID_exact_2xSize()
    {
        // gridSize: 50 (one cross at the center if size less than 2xSize)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 100, 100);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.GRID);
        rectGuideline.draw(rect, g2);

        // cross at the center (gridSize: 50)
        verify(g2, times(2)).draw(refEq(new Line2D.Double(50, 0, 50, 100)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 50, 100, 50)));
        // sides horizontal
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, 100, 0)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 100, 100, 100)));
        // sides vertical
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, 0, 100)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(100, 0, 100, 100)));
        // total
        verify(g2, atMost(12)).draw(any());
    }

    @Test
    public void draw_Type_GRID_more_than_2xSize()
    {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 102, 102);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.GRID);
        rectGuideline.draw(rect, g2);

        // cross at the center (gridSize: 50)
        verify(g2, times(2)).draw(refEq(new Line2D.Double(51, 0, 51, 102)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 51, 102, 51)));
        // sides horizontal
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 1, 102, 1)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 101, 102, 101)));
        // sides vertical
        verify(g2, times(2)).draw(refEq(new Line2D.Double(1, 0, 1, 102)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(101, 0, 101, 102)));
        // total
        verify(g2, atMost(12)).draw(any());
    }

    @Test
    public void draw_Type_SPIRAL_orientation_0()
    {
        // orientation: 0 (spiral that starts from bottom left)
        Rectangle2D rect = new Rectangle2D.Double(0, 0, 10, 10);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.setType(RectGuidelineType.GOLDEN_SPIRAL);
        rectGuideline.setOrientation(0);
        rectGuideline.draw(rect, g2);

        verify(g2, atMost(2*11)).draw(any(Arc2D.Double.class));
    }
}