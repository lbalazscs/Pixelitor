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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.ImageComponent;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.util.PPoint;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
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
    private ImageComponent ic;

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
    public static void setupClass() {
        TestHelper.setupMockFgBgSelector();
        Build.setTestingMode();
    }

    @Before
    public void setUp() {
        Composition comp = TestHelper.create2LayerComposition(false);
        ic = comp.getIC();

        dr = comp.getActiveDrawableOrThrow();

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
        verify(brushSpy).onStrokeStart(any());
        verify(brushSpy, times(5)).onNewStrokePoint(any());
    }

    @Test
    public void test_drawBrushStrokeProgrammatically() {
        PPoint start = new PPoint.Image(ic, 2.0, 2.0);
        PPoint end = new PPoint.Image(ic, 5.0, 5.0);
        tool.drawBrushStrokeProgrammatically(dr, start, end);

        verify(brushSpy).onStrokeStart(any());
        verify(brushSpy).onNewStrokePoint(any());
    }
}
