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

package pixelitor.guides;

import org.junit.Before;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GuidesRendererTest {
    private GuideStyle guideStyle;
    private GuidesRenderer guidesRenderer;

    @Before
    public void setUp() {
        guideStyle = new GuideStyle();
        guidesRenderer = new GuidesRenderer(guideStyle);
    }

    @Test
    public void testDrawNone() {
        var g2 = mock(Graphics2D.class);
        var lines = new ArrayList<Shape>();

        guidesRenderer.draw(g2, lines);

        verify(g2, times(0)).draw(any());
    }

    @Test
    public void testDrawInSingleStrokeMode() {
        var g2 = mock(Graphics2D.class);
        var lines = new ArrayList<Shape>();
        lines.add(new Line2D.Double(1, 2, 3, 4));
        guideStyle.setStrokeType(GuideStrokeType.SOLID);

        guidesRenderer.draw(g2, lines);

        verify(g2, times(1)).setColor(guideStyle.getColorA());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeA());
        verify(g2, times(1)).draw(lines.get(0));
    }

    @Test
    public void testDrawInDoubleStrokeMode() {
        var g2 = mock(Graphics2D.class);
        var lines = new ArrayList<Shape>();
        lines.add(new Line2D.Double(1, 2, 3, 4));
        guideStyle.setStrokeType(GuideStrokeType.DASHED_DOUBLE);

        guidesRenderer.draw(g2, lines);

        verify(g2, times(1)).setColor(guideStyle.getColorA());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeA());
        verify(g2, times(1)).setColor(guideStyle.getColorB());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeB());
        verify(g2, times(2)).draw(lines.get(0));
    }
}
