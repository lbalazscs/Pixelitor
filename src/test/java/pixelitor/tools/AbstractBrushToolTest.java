/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

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

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Parameters(name = "{index}: {0} Tool")
    public static Collection<Object[]> instancesToTest() throws InvocationTargetException, InterruptedException {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        Tool[] tools = {BRUSH, ERASER, CLONE, SMUDGE};
        for (Tool tool : tools) {
            TestHelper.initTool(tool);
        }

        return Arrays.asList(new Object[][]{
            {BRUSH},
            {ERASER},
            {CLONE},
            {SMUDGE},
        });
    }

    @Before
    public void beforeEachTest() {
        var comp = TestHelper.createComp(2, false);

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
}
