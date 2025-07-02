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

package pixelitor.tools;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InOrder;
import pixelitor.TestHelper;
import pixelitor.Views;
import pixelitor.layers.Drawable;
import pixelitor.tools.brushes.Brush;
import pixelitor.utils.Texts;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;

/**
 * Tests functionality common to all {@link AbstractBrushTool} subclasses.
 */
@RunWith(Parameterized.class)
public class AbstractBrushToolTest {
    // the tool being tested in each parameterized run
    @Parameter
    public AbstractBrushTool tool;

    private Brush spyBrush;
    private Brush origBrush;
    private Drawable dr;

    @Parameters(name = "{index}: {0} Tool")
    public static Collection<Object[]> instancesToTest() throws InvocationTargetException, InterruptedException {
        // call it here, because this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        AbstractBrushTool[] brushTools = {BRUSH, ERASER, CLONE, SMUDGE};
        ResourceBundle resources = Texts.getResources();

        for (AbstractBrushTool tool : brushTools) {
            TestHelper.initToolSettings(tool, resources);
        }

        return Arrays.stream(brushTools)
            .map(tool -> new Object[]{tool})
            .toList();
    }

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        var comp = TestHelper.createComp("AbstractBrushToolTest", 2, false);

        dr = comp.getActiveDrawableOrThrow();

        origBrush = tool.getBrush();
        spyBrush = spy(origBrush);
        tool.setBrush(spyBrush);

        tool.toolActivated(comp.getView());
    }

    @After
    public void afterEachTest() {
        tool.toolDeactivated(Views.getActive());

        // restore it so that next time we don't spy on a spy...
        tool.setBrush(origBrush);
    }

    @Test
    public void trace_invokesBrushMethodsInOrder() {
        tool.trace(dr, new Rectangle(2, 2, 2, 2));

        InOrder inOrder = inOrder(spyBrush);

        inOrder.verify(spyBrush).setTarget(any(), any());
        inOrder.verify(spyBrush).startAt(any());
        inOrder.verify(spyBrush, atLeastOnce()).continueTo(any());
    }
}
