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

package pixelitor.guides;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import pixelitor.TestHelper;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("GuidesRenderer tests")
@TestMethodOrder(MethodOrderer.Random.class)
class GuidesRendererTest {
    private GuideStyle guideStyle;
    private GuidesRenderer guidesRenderer;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        guideStyle = new GuideStyle();
        guidesRenderer = new GuidesRenderer(guideStyle);
    }

    @Test
    void drawNone() {
        var g2 = mock(Graphics2D.class);
        var lines = new ArrayList<Shape>();

        guidesRenderer.draw(g2, lines);

        verify(g2, times(0)).draw(any());
    }

    @ParameterizedTest
    @EnumSource(names = {"SOLID", "DOTTED", "DASHED"})
    void drawInSingleStrokeMode(GuideStrokeType strokeType) {
        var g2 = mock(Graphics2D.class);
        var lines = new ArrayList<Shape>();
        lines.add(new Line2D.Double(1, 2, 3, 4));
        lines.add(new Line2D.Double(2, 3, 4, 5));
        guideStyle.setStrokeType(strokeType);

        guidesRenderer.draw(g2, lines);

        verify(g2, times(1)).setColor(guideStyle.getColorA());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeA());
        verify(g2, times(1)).draw(lines.getFirst());
        verify(g2, times(1)).draw(lines.getLast());

        // verify that properties are set before drawing
        InOrder inOrder = inOrder(g2);
        inOrder.verify(g2).setStroke(guideStyle.getStrokeA());
        inOrder.verify(g2).setColor(guideStyle.getColorA());
        inOrder.verify(g2).draw(lines.getFirst());
    }

    @ParameterizedTest
    @EnumSource(names = {"DASHED_DOUBLE", "DASHED_BORDERED"})
    void drawInDoubleStrokeMode(GuideStrokeType strokeType) {
        var g2 = mock(Graphics2D.class);
        var lines = new ArrayList<Shape>();
        lines.add(new Line2D.Double(1, 2, 3, 4));
        lines.add(new Line2D.Double(2, 3, 4, 5));
        guideStyle.setStrokeType(strokeType);

        guidesRenderer.draw(g2, lines);

        verify(g2, times(1)).setColor(guideStyle.getColorA());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeA());
        verify(g2, times(1)).setColor(guideStyle.getColorB());
        verify(g2, times(1)).setStroke(guideStyle.getStrokeB());
        verify(g2, times(2)).draw(lines.getFirst());
        verify(g2, times(2)).draw(lines.getLast());

        // verify that both drawing passes occur in the correct order
        InOrder inOrder = inOrder(g2);
        inOrder.verify(g2).setStroke(guideStyle.getStrokeA());
        inOrder.verify(g2).setColor(guideStyle.getColorA());
        inOrder.verify(g2).draw(lines.getFirst());
        inOrder.verify(g2).draw(lines.getLast());
        inOrder.verify(g2).setStroke(guideStyle.getStrokeB());
        inOrder.verify(g2).setColor(guideStyle.getColorB());
        inOrder.verify(g2).draw(lines.getFirst());
        inOrder.verify(g2).draw(lines.getLast());
    }
}
