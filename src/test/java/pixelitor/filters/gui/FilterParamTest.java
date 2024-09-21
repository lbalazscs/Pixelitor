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

package pixelitor.filters.gui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.TestHelper;
import pixelitor.filters.gui.IntChoiceParam.Item;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.ANIMATION_ENDING_STATE;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.APP_LOGIC;
import static pixelitor.filters.gui.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.TransparencyPolicy.NO_TRANSPARENCY;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

/**
 * Checks whether different FilterParam implementations implement
 * the FilterParam contract correctly
 */
@RunWith(Parameterized.class)
public class FilterParamTest {
    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Parameter
    public FilterParam param;

    private ParamAdjustmentListener adjustmentListener;

    @Before
    public void beforeEachTest() {
        adjustmentListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjustmentListener);
    }

    @Parameters(name = "{index}: param = {0}")
    public static Collection<Object[]> instancesToTest() {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        return Arrays.asList(new Object[][]{
            {new RangeParam("Param Name", 0, 0, 10)},
            {new RangeWithColorsParam(CYAN, RED, "Param Name", -100, 0, 100)},
            {new GroupedRangeParam("Param Name", 0, 0, 100, true)},
            {new GroupedRangeParam("Param Name", 0, 0, 100, false)},
            {new ImagePositionParam("Param Name")},
            {new GradientParam("Param Name", BLACK, WHITE)},
            {new TextParam("Param Name", "default text", true)},
            {new ColorParam("Param Name", BLACK, FREE_TRANSPARENCY)},
            {new ColorParam("Param Name", WHITE, USER_ONLY_TRANSPARENCY)},
            {new ColorParam("Param Name", BLUE, NO_TRANSPARENCY)},
            {new ColorListParam("Param Name", 1, 1, BLACK, BLUE)},
            {new GroupedColorsParam("Param Name", "Name 1", BLUE, "Name 2", BLUE, FREE_TRANSPARENCY, true, true)},
            {new BooleanParam("Param Name", true)},
            {new AngleParam("Param Name", 0)},
            {new ElevationAngleParam("Param Name", 0)},
            {new IntChoiceParam("Param Name", new Item[]{
                new Item("Better", 0),
                new Item("Faster", 1),
            })
            },
            {new StrokeParam("Param Name")},
            {new EffectsParam("Param Name")},
            {new DialogParam("Param Name",
                new RangeParam("Child", 0, 50, 100),
                new AngleParam("Child 2", 0),
                new BooleanParam("Child 3", true))
            },
            {new LogZoomParam("Param Name", 200, 200, 1000)}
        });
    }

    @Test
    public void GUI_test() {
        JComponent gui = param.createGUI();
        assertThat(gui).isNotNull();
        assertThat(gui.isEnabled()).isTrue();

        assertThat(gui).isInstanceOf(ParamGUI.class);
        ParamGUI paramGUI = (ParamGUI) gui;

        paramGUI.setEnabled(false);
        assertThat(gui.isEnabled()).isFalse();
        paramGUI.setEnabled(true);
        assertThat(gui.isEnabled()).isTrue();

        paramGUI.updateGUI();

        checkThatFilterWasNotCalled();
    }

    @Test
    public void getNumLayoutColumns() {
        int cols = ((ParamGUI) param.createGUI()).getNumLayoutColumns();
        assertThat(cols > 0 && cols < 3).isTrue();
        checkThatFilterWasNotCalled();
    }

    @Test
    public void randomize() {
        param.setRandomizePolicy(RandomizePolicy.ALLOW_RANDOMIZE);
        param.randomize();
        checkThatFilterWasNotCalled();

        Object beforeValue = param.getParamValue();
        param.setRandomizePolicy(RandomizePolicy.IGNORE_RANDOMIZE);
        param.randomize();
        assertThat(param.getParamValue())
            .isNotNull()
            .isEqualTo(beforeValue);
        checkThatFilterWasNotCalled();
    }

    @Test
    public void reset_false() {
        param.reset(false);

        checkThatFilterWasNotCalled();
        assertThat(param).isDefault();
    }

    @Test
    public void reset_true() {
        // make sure that the value is set to the real default value,
        // otherwise (depending on the method execution order) the
        // copyState_setState test could change the value of the angle param
        // from 0 to 2*pi, which confuses this test
        param.reset(false);

        Object defaultValue = param.getParamValue();
        // we can change the value in a general way only
        // through randomize
        if (!param.canRandomize()) {
            param.reset(true);
            assertThat(param).isDefault();
            return;
        }

        // wait until randomize changes the value to a non-default value
        boolean changed = false;
        while (!changed) {
            param.randomize();
            checkThatFilterWasNotCalled();
            changed = !param.hasDefault();
        }
        assertThat(param.getParamValue())
            .isNotNull()
            .isNotEqualTo(defaultValue);

        param.reset(true);

        assertThat(param).isDefault();
        assertThat(param.getParamValue())
            .isNotNull()
            .isEqualTo(defaultValue);

        verify(adjustmentListener, times(1)).paramAdjusted();
    }

    @Test
    public void copyState_setState() {
        ParamState<?> paramState = param.copyState();
        assertThat(paramState).isNotNull();
        param.loadStateFrom(paramState, false);
        checkThatFilterWasNotCalled();
    }

    @Test
    public void simpleMethodsDontRunFilter() {
        assertThat(param).nameIs("Param Name");

        JComponent gui = param.createGUI();

        param.isAnimatable();

        param.setEnabled(false, APP_LOGIC);
        assertThat(gui.isEnabled()).isFalse();

        param.setEnabled(true, APP_LOGIC);
        assertThat(gui.isEnabled()).isTrue();

        param.setEnabled(false, ANIMATION_ENDING_STATE);
        if (param.isAnimatable()) {
            assertThat(gui.isEnabled()).isTrue();
        } else {
            assertThat(gui.isEnabled()).isFalse();
        }

        param.setEnabled(true, ANIMATION_ENDING_STATE);
        assertThat(gui.isEnabled()).isTrue();

        checkThatFilterWasNotCalled();
    }

    private void checkThatFilterWasNotCalled() {
        verify(adjustmentListener, never()).paramAdjusted();
    }
}
