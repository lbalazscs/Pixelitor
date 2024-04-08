/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.tools.pen.PenTool;
import pixelitor.utils.Texts;

import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;

/**
 * Behavior that is common to all tools
 */
@RunWith(Parameterized.class)
public class ToolTest {
    private View view;

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

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Parameters(name = "{index}: tool = {0} Tool, alt = {1}, ctrl = {2}, shift = {3}, mouseButton = {4}")
    public static Collection<Object[]> instancesToTest() throws InvocationTargetException, InterruptedException {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        Tools.CLONE.setState(CloneTool.State.CLONING);

        Tool[] tools = Tools.getAll();
//        Tool[] tools = {Tools.BRUSH};

        List<Object[]> instances = new ArrayList<>();
        ResourceBundle resources = Texts.getResources();

        for (Tool tool : tools) {
            TestHelper.initTool(tool, resources);

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

    @Before
    public void beforeEachTest() {
        PenTool.path = null;
        Composition comp = TestHelper.createComp(2, true);
        view = comp.getView();
        Tools.setCurrentTool(tool);
        tool.toolStarted();
    }

    @After
    public void afterEachTest() {
        tool.toolEnded();
        Views.setActiveView(null, false);
    }

    @Test
    public void presets() {
        if (tool.canHaveUserPresets()) {
            UserPreset preset = tool.createUserPreset("test");
            tool.loadUserPreset(preset);
        }
    }

    @Test
    public void simpleStroke() {
        stroke(alt, ctrl, shift, mouseButton);

        // also test with space down
        GlobalEvents.setSpaceDown(true);
        stroke(alt, ctrl, shift, mouseButton);
        GlobalEvents.setSpaceDown(false);
    }

    private void stroke(Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton) {
        KeyModifiers keys = new KeyModifiers(ctrl, alt, shift);
        // stroke with all the modifiers
        press(keys, mouseButton, 2, 2);
        drag(keys, mouseButton, 3, 3);
        release(keys, mouseButton, 4, 4);

        // press the modifiers, but release them before finishing
        press(keys, mouseButton, 2, 2);
        drag(keys, mouseButton, 3, 3);
        release(KeyModifiers.NONE, mouseButton, 4, 4);

        // start without the modifiers, but finish with them
        press(KeyModifiers.NONE, mouseButton, 2, 2);
        drag(keys, mouseButton, 3, 3);
        release(keys, mouseButton, 4, 4);

        // do a simple click
        press(keys, mouseButton, 1, 1);
        release(keys, mouseButton, 1, 1);
    }

    private void press(KeyModifiers keys, MouseButton mouseButton, int x, int y) {
        MouseEvent e = TestHelper.createEvent(x, y, MOUSE_PRESSED, keys, mouseButton, view);
        Tools.EventDispatcher.mousePressed(e, view);
    }

    private void drag(KeyModifiers keys, MouseButton mouseButton, int x, int y) {
        MouseEvent e = TestHelper.createEvent(x, y, MOUSE_DRAGGED, keys, mouseButton, view);
        Tools.EventDispatcher.mouseDragged(e, view);
    }

    private void release(KeyModifiers keys, MouseButton mouseButton, int x, int y) {
        MouseEvent e = TestHelper.createEvent(x, y, MOUSE_RELEASED, keys, mouseButton, view);
        Tools.EventDispatcher.mouseReleased(e, view);
    }
}
