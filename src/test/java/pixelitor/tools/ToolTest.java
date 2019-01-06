/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GlobalEventWatch;
import pixelitor.gui.CompositionView;
import pixelitor.gui.OpenComps;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.pen.PenTool;
import pixelitor.tools.util.PMouseEvent;

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
    private CompositionView cv;

    @Parameter
    public Tool tool;

    @Parameter(value = 1)
    public Alt alt;

    @Parameter(value = 2)
    public Ctrl ctrl;

    @Parameter(value = 3)
    public Shift shift;

    @Parameter(value = 4)
    public MouseButton mouseButton;

    @Parameters(name = "{index}: tool = {0} Tool, alt = {1}, ctrl = {2}, shift = {3}, mouseButton = {4}")
    public static Collection<Object[]> instancesToTest() throws InvocationTargetException, InterruptedException {
        Tools.CLONE.setState(CloneTool.State.CLONING);

        Tool[] tools = Tools.getAll();
//        Tool[] tools = {Tools.BRUSH};

        List<Object[]> instances = new ArrayList<>();
        for (Tool tool : tools) {
            tool.setSettingsPanel(new ToolSettingsPanel());
//            tool.initSettingsPanel();
            SwingUtilities.invokeAndWait(tool::initSettingsPanel);

            // for each combination create an independent test run
            for (Alt alt : Alt.values()) {
                for (Ctrl ctrl : Ctrl.values()) {
                    for (Shift shift : Shift.values()) {
                        for (MouseButton mouseButton : MouseButton.values()) {
                            instances.add(new Object[]{tool, alt, ctrl, shift, mouseButton});
                        }
                    }
                }
            }
        }

        return instances;
    }

    @BeforeClass
    public static void setupClass() {
        Build.setUnitTestingMode();
        TestHelper.setupMockFgBgSelector();
    }

    @Before
    public void setUp()  {
        PenTool.path = null;
        Composition comp = TestHelper.create2LayerComposition(true);
        cv = comp.getView();
        tool.toolStarted();
    }

    @After
    public void tearDown() {
        tool.toolEnded();
        OpenComps.setActiveIC(null, false);
    }

    @Test
    public void simpleStroke() {
        stroke(alt, ctrl, shift, mouseButton);

        // also test with space down
        GlobalEventWatch.setSpaceDown(true);
        stroke(alt, ctrl, shift, mouseButton);
        GlobalEventWatch.setSpaceDown(false);
    }

    private void stroke(Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton) {
        // stroke with all the modifiers
        press(alt, ctrl, shift, mouseButton, 2, 2);
        drag(alt, ctrl, shift, mouseButton, 3, 3);
        release(alt, ctrl, shift, mouseButton, 4, 4);

        // press the modifiers, but release them before finishing
        press(alt, ctrl, shift, mouseButton, 2, 2);
        drag(alt, ctrl, shift, mouseButton, 3, 3);
        release(Alt.NO, Ctrl.NO, Shift.NO, mouseButton, 4, 4);

        // start without the modifiers, but finish with them
        press(Alt.NO, Ctrl.NO, Shift.NO, mouseButton, 2, 2);
        drag(alt, ctrl, shift, mouseButton, 3, 3);
        release(alt, ctrl, shift, mouseButton, 4, 4);

        // do a simple click
        press(alt, ctrl, shift, mouseButton, 1, 1);
        release(alt, ctrl, shift, mouseButton, 1, 1);
    }

    private void press(Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton, int x, int y) {
        PMouseEvent e = TestHelper.createPEvent(x, y, MOUSE_PRESSED, ctrl, alt, shift, mouseButton, cv
        );
        tool.handlerChain.handleMousePressed(e);
    }

    private void drag(Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton, int x, int y) {
        PMouseEvent e = TestHelper.createPEvent(x, y, MOUSE_DRAGGED, ctrl, alt, shift, mouseButton, cv
        );
        tool.handlerChain.handleMouseDragged(e);
    }

    private void release(Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton, int x, int y) {
        PMouseEvent e = TestHelper.createPEvent(x, y, MOUSE_RELEASED, ctrl, alt, shift, mouseButton, cv
        );
        tool.handlerChain.handleMouseReleased(e);
    }
}
