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

package pixelitor.filters.gui;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.swing.*;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.FREE_OPACITY;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.APP_LOGIC;
import static pixelitor.filters.gui.FilterSetting.EnabledReason.FINAL_ANIMATION_SETTING;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Checks whether different FilterParam implementations implement
 * the FilterParam contract correctly
 */
@RunWith(Parameterized.class)
public class FilterParamTest {
    @Parameter
    public FilterParam param;

    private ParamAdjustmentListener adjustmentListener;

    @Before
    public void setUp() {
        adjustmentListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(adjustmentListener);
    }

    @Parameters(name = "{index}: param = {0}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {new RangeParam("Param Name", 0, 0, 10)},
                {new RangeParam("Param Name", 0, 5, 10).setRandomizePolicy(IGNORE_RANDOMIZE)},
                {new RangeWithColorsParam(CYAN, RED, "Param Name", -100, 0, 100)},
                {new GroupedRangeParam("Param Name", 0, 0, 100, true)},
                {new GroupedRangeParam("Param Name", 0, 0, 100, false)},
                {new ImagePositionParam("Param Name")},
                {new GradientParam("Param Name", BLACK, WHITE)},
                {new TextParam("Param Name", "default text")},
                {new ColorParam("Param Name", BLACK, FREE_OPACITY)},
                {new ColorParam("Param Name", WHITE, USER_ONLY_OPACITY)},
                {new ColorParam("Param Name", BLUE, NO_OPACITY)},
                {new BooleanParam("Param Name", true)},
                {new AngleParam("Param Name", 0)},
                {new ElevationAngleParam("Param Name", 0)},
                {new IntChoiceParam("Param Name", new IntChoiceParam.Value[]{
                        new IntChoiceParam.Value("Better", 0),
                        new IntChoiceParam.Value("Faster", 1),
                })
                },
                {new StrokeParam("Param Name")},
                {new EffectsParam("Param Name")},
        });
    }

    @Test
    public void test_createGUI() {
        JComponent gui = param.createGUI();
        assertThat(gui).isNotNull();
        assertThat(gui.isEnabled()).isTrue();

        // all params return a GUI that implements ParamGUI
        // with the exception of GradientParam
        if (!(param instanceof GradientParam)) {
            assertThat(gui).isInstanceOf(ParamGUI.class);
            ParamGUI paramGUI = (ParamGUI) gui;

            paramGUI.setEnabled(false);
            assertThat(gui.isEnabled()).isFalse();
            paramGUI.setEnabled(true);
            assertThat(gui.isEnabled()).isTrue();

            paramGUI.updateGUI();
        }

        checkThatFilterWasNotCalled();
    }

    @Test
    public void test_getNrOfGridBagCols() {
        int cols = param.getNrOfGridBagCols();
        assertThat(cols > 0 && cols < 3).isTrue();
        checkThatFilterWasNotCalled();
    }

    @Test
    public void test_randomize() {
        param.randomize();

        checkThatFilterWasNotCalled();
    }

    @Test
    public void test_reset_false() {
        param.reset(false);

        checkThatFilterWasNotCalled();
        assertThat(param.isSetToDefault()).isTrue();
    }

    @Test
    public void test_reset_true() {
        // we can change the value in a general way only
        // through randomize
        if (param.ignoresRandomize()) {
            // in this case we don't know whether to expect
            // the calling of the filter
            param.reset(true);
            assertThat(param.isSetToDefault()).isTrue();
            return;
        }

        // wait until randomize changes the value to a non-default value
        boolean changed = false;
        while (!changed) {
            param.randomize();
            checkThatFilterWasNotCalled();
            changed = !param.isSetToDefault();
        }

        param.reset(true);
        assertThat(param.isSetToDefault()).isTrue();

        verify(adjustmentListener, times(1)).paramAdjusted();
    }

    @Test
    public void test_copyState_setState() {
        try {
            ParamState paramState = param.copyState();
            assertThat(paramState).isNotNull();
            param.setState(paramState);
        } catch (UnsupportedOperationException e) {
            // It is OK to throw this exception
        }
        checkThatFilterWasNotCalled();
    }

    @Test
    public void testSimpleMethods() {
        assertThat(param.getName()).isEqualTo("Param Name");

        JComponent gui = param.createGUI();
        param.considerImageSize(new Rectangle(0, 0, 1000, 600));

        param.canBeAnimated();

        param.setEnabled(false, APP_LOGIC);
        assertThat(gui.isEnabled()).isFalse();

        param.setEnabled(true, APP_LOGIC);
        assertThat(gui.isEnabled()).isTrue();

        param.setEnabled(false, FINAL_ANIMATION_SETTING);
        if (param.canBeAnimated()) {
            assertThat(gui.isEnabled()).isTrue();
        } else {
            assertThat(gui.isEnabled()).isFalse();
        }

        param.setEnabled(true, FINAL_ANIMATION_SETTING);
        assertThat(gui.isEnabled()).isTrue();

        checkThatFilterWasNotCalled();
    }

    private void checkThatFilterWasNotCalled() {
        verify(adjustmentListener, never()).paramAdjusted();
    }
}
