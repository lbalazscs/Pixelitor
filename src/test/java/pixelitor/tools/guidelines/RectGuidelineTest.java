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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RectGuidelineTest {

    private RectGuideline rectGuideline;

    @Test
    public void draw_GuideLineType_NONE() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 30);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.NONE, g2);

        verify(g2, never()).draw(any(Line2D.class));
    }


    @Test
    public void draw_Type_RULE_OF_THIRDS() {

        Rectangle2D rect = new Rectangle2D.Double(0, 0, 90, 12);
        Graphics2D g2 = mock(Graphics2D.class);

        rectGuideline = new RectGuideline();
        rectGuideline.draw(rect, RectGuidelineType.RULE_OF_THIRDS, g2);

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
        rectGuideline.draw(rect, RectGuidelineType.GOLDEN_SECTIONS, g2);

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
        rectGuideline.draw(rect, RectGuidelineType.DIAGONALS, g2);

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
        rectGuideline.draw(rect, RectGuidelineType.DIAGONALS, g2);

        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 0, 12, 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 12, 12, 0)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 90, 12, 90 - 12)));
        verify(g2, times(2)).draw(refEq(new Line2D.Double(0, 90 - 12, 12, 90)));
    }
}