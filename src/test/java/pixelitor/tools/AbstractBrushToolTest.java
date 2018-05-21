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

package pixelitor.tools;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;

/**
 * Tests the functionality common to all AbstractBrush subclasses
 */
@RunWith(Parameterized.class)
public class AbstractBrushToolTest {
    @Parameter
    public AbstractBrushTool tool;

    private Brush brushSpy;
    private Brush origBrush;

    private Drawable dr;

    @Parameters(name = "{index}: {0}, mask = {1}")
    public static Collection<Object[]> instancesToTest() {
        Tool[] tools = {BRUSH, ERASER, CLONE, SMUDGE};
        for (Tool tool : tools) {
            tool.setSettingsPanel(new ToolSettingsPanel());
            tool.initSettingsPanel();
        }

        return Arrays.asList(new Object[][]{
                {BRUSH},
                {ERASER},
                {CLONE},
                {SMUDGE},
        });
    }

    @Before
    public void setUp() {
        Composition comp = TestHelper.create2LayerComposition(false);
        dr = comp.getActiveDrawable();

        origBrush = tool.getBrush();
        brushSpy = spy(origBrush);
        tool.setBrush(brushSpy);

        tool.toolStarted();
    }

    @After
    public void tearDown() {
        tool.toolEnded();

        // restore it so that next time we don't spy on a spy...
        tool.setBrush(origBrush);
    }

    @Test
    public void test_trace() {
        tool.trace(dr, new Rectangle(2, 2, 2, 2));

        verify(brushSpy).setTarget(any(), any());
        verify(brushSpy).onStrokeStart(2.0, 2.0);
        verify(brushSpy, times(5)).onNewStrokePoint(anyDouble(), anyDouble());
    }

    @Test
    public void test_drawBrushStrokeProgrammatically() {
        tool.drawBrushStrokeProgrammatically(dr, new Point(2, 2), new Point(5, 5));

        verify(brushSpy).onStrokeStart(2.0, 2.0);
        verify(brushSpy).onNewStrokePoint(5.0, 5.0);
    }
}
