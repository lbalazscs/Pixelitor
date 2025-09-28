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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.utils.Texts;
import pixelitor.utils.input.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Tests behavior that is common to all tools.
 */
@ParameterizedClass(name = "tool = {0} Tool, alt = {1}, ctrl = {2}, shift = {3}, mouseButton = {4}")
@MethodSource("instancesToTest")
@DisplayName("tool tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ToolTest {
    private View view;

    @Parameter(0)
    private Tool tool;

    @Parameter(1)
    private Alt alt;

    @Parameter(2)
    private Ctrl ctrl;

    @Parameter(3)
    private Shift shift;

    @Parameter(4)
    private MouseButton mouseButton;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    static Collection<Object[]> instancesToTest() throws InvocationTargetException, InterruptedException {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        Tools.CLONE.setState(CloneTool.State.CLONING);

        Tool[] tools = Tools.getAll();
//        Tool[] tools = {Tools.BRUSH};

        List<Object[]> instances = new ArrayList<>();
        ResourceBundle resources = Texts.getResources();

        for (Tool tool : tools) {
            TestHelper.initToolSettings(tool, resources);

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

    @BeforeEach
    void beforeEachTest() {
        Composition comp = TestHelper.createComp("ToolTest", 2, true);
        view = comp.getView();
        Tools.setActiveTool(tool);
        tool.toolActivated(view);
    }

    @AfterEach
    void afterEachTest() {
        tool.toolDeactivated(view);
        Views.setActiveView(null, false);
    }

    @Test
    void presets() {
        if (tool.shouldHaveUserPresetsMenu()) {
            UserPreset preset = tool.createUserPreset("test");
            tool.loadUserPreset(preset);
        }
    }

    @Test
    void simpleStroke() {
        stroke(alt, ctrl, shift, mouseButton);

        // also test with space down
        GlobalEvents.setSpaceDown(true);
        stroke(alt, ctrl, shift, mouseButton);
        GlobalEvents.setSpaceDown(false);
    }

    private void stroke(Alt alt, Ctrl ctrl, Shift shift, MouseButton mouseButton) {
        Modifiers modifiers = new Modifiers(ctrl, alt, shift, mouseButton);
        // stroke with all the modifiers
        press(modifiers, 2, 2);
        drag(modifiers, 3, 3);
        release(modifiers, 4, 4);

        // press the modifiers, but release them before finishing
        press(modifiers, 2, 2);
        drag(modifiers, 3, 3);
        release(Modifiers.NONE, 4, 4);

        // start without the modifiers, but finish with them
        press(Modifiers.NONE, 2, 2);
        drag(modifiers, 3, 3);
        release(modifiers, 4, 4);

        // do a simple click
        press(modifiers, 1, 1);
        release(modifiers, 1, 1);
    }

    private void press(Modifiers modifiers, int x, int y) {
        modifiers.dispatchPressedEvent(x, y, view);
    }

    private void drag(Modifiers modifiers, int x, int y) {
        modifiers.dispatchDraggedEvent(x, y, view);
    }

    private void release(Modifiers modifiers, int x, int y) {
        modifiers.dispatchReleasedEvent(x, y, view);
    }
}
