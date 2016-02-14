/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.gui.ImageComponent;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;

/**
 * Behavior that is common to all tools
 */
@RunWith(Parameterized.class)
public class ToolTest {
    private ImageComponent ic;

    @Parameter
    public Tool tool;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> instancesToTest() throws InvocationTargetException, InterruptedException {
        Tools.CLONE.setState(CloneTool.State.CLONING);

        Tool[] tools = Tools.getTools();
//        Tool[] tools = {Tools.BRUSH};

        List<Object[]> instances = new ArrayList<>();
        for (Tool tool : tools) {
            tool.setSettingsPanel(new ToolSettingsPanel());
//            tool.initSettingsPanel();
            SwingUtilities.invokeAndWait(tool::initSettingsPanel);

            instances.add(new Object[]{tool});
        }

        return instances;
    }

    @Before
    public void setUp()  {
        tool.toolStarted();
        Composition comp = TestHelper.create2LayerComposition(true);

        ic = TestHelper.setupAnActiveICFor(comp);
    }

    @After
    public void tearDown() {
        tool.toolEnded();
    }

    @Test
    public void simpleStroke() {
        strokeMouseLeftRight();

        // TODO space should also be tested
    }

    private void strokeMouseLeftRight() {
        strokeAltYesNo(Mouse.LEFT);
        strokeAltYesNo(Mouse.RIGHT);
    }

    private void strokeAltYesNo(Mouse mouse) {
        strokeCtrlYesNo(Alt.NO, mouse);
        strokeCtrlYesNo(Alt.YES, mouse);
    }

    private void strokeCtrlYesNo(Alt alt, Mouse mouse) {
        strokeShiftYesNo(alt, Ctrl.NO, mouse);
        strokeShiftYesNo(alt, Ctrl.YES, mouse);
    }

    private void strokeShiftYesNo(Alt alt, Ctrl ctrl, Mouse mouse) {
        stroke(alt, ctrl, Shift.NO, mouse);
        stroke(alt, ctrl, Shift.YES, mouse);
    }

    private void stroke(Alt alt, Ctrl ctrl, Shift shift, Mouse mouse) {
        tool.dispatchMousePressed(TestHelper.createEvent(MOUSE_PRESSED, alt, ctrl, shift, mouse, 2, 2), ic);
        tool.dispatchMouseDragged(TestHelper.createEvent(MOUSE_DRAGGED, alt, ctrl, shift, mouse, 4, 4), ic);
        tool.dispatchMouseReleased(TestHelper.createEvent(MOUSE_RELEASED, alt, ctrl, shift, mouse, 6, 6), ic);
    }
}
