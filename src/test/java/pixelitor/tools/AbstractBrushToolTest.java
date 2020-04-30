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

package pixelitor.tools;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.util.PPoint;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pixelitor.tools.Tools.*;

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
    private View view;

    @Parameters(name = "{index}: {0} Tool")
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

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setupMockFgBgSelector();
        TestHelper.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        var comp = TestHelper.create2LayerComp(false);
        view = comp.getView();

        dr = comp.getActiveDrawableOrThrow();

        origBrush = tool.getBrush();
        brushSpy = spy(origBrush);
        tool.setBrush(brushSpy);

        tool.toolStarted();
    }

    @After
    public void afterEachTest() {
        tool.toolEnded();

        // restore it so that next time we don't spy on a spy...
        tool.setBrush(origBrush);
    }

    @Test
    public void trace() {
        tool.trace(dr, new Rectangle(2, 2, 2, 2));

        verify(brushSpy).setTarget(any(), any());
        verify(brushSpy).startAt(any());
        verify(brushSpy, times(5)).continueTo(any());
    }

    @Test
    public void drawBrushStrokeProgrammatically() {
        PPoint start = PPoint.eagerFromIm(2.0, 2.0, view);
        PPoint end = PPoint.eagerFromIm(5.0, 5.0, view);
        tool.drawBrushStrokeProgrammatically(dr, start, end);

        verify(brushSpy).startAt(any());
        verify(brushSpy).continueTo(any());
    }
}
