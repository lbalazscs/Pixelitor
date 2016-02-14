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
import pixelitor.filters.ParamTest;
import pixelitor.utils.ReseedSupport;

import java.awt.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ParamSetTest {
    private ParamSet params;
    private ParamAdjustmentListener adjustmentListener;
    private RangeParam extraParam;

    @Before
    public void setUp() {
        params = new ParamSet(ParamTest.getTestParams())
                .withAction(ReseedSupport.createAction())
                .addCommonActions();
        adjustmentListener = mock(ParamAdjustmentListener.class);
        params.setAdjustmentListener(adjustmentListener);
        extraParam = new RangeParam("Extra Param", 0, 0, 200);
        extraParam.setAdjustmentListener(adjustmentListener);
        params.insertParam(extraParam, 3);
        params.considerImageSize(new Rectangle(0, 0, 400, 800));
    }

    @Test
    public void test_reset() {
        params.reset();
        checkThatFilterWasNotCalled();
    }

    @Test
    public void test_randomize() {
        params.randomize();
        checkThatFilterWasNotCalled();
    }

    @Test
    public void testFilterTriggering() {
        extraParam.setValue(42, false);
        checkThatFilterWasNotCalled();

        extraParam.setValue(43, true);
        verify(adjustmentListener, times(1)).paramAdjusted();

        params.triggerFilter();
        verify(adjustmentListener, times(2)).paramAdjusted();
    }

    @Test
    public void test_copyState_setState() {
        ParamSetState state = params.copyState();
        params.setState(state);

        checkThatFilterWasNotCalled();
    }

    @Test
    public void test_canBeAnimated() {
        assertThat(params.canBeAnimated()).isTrue();
    }

    @Test
    public void test_setFinalAnimationSettingMode() {
        params.setFinalAnimationSettingMode(false);
        params.setFinalAnimationSettingMode(true);

        checkThatFilterWasNotCalled();
    }

    @Test
    public void test_hasGradient() {
        assertThat(params.hasGradient()).isTrue();
    }

    private void checkThatFilterWasNotCalled() {
        verify(adjustmentListener, never()).paramAdjusted();
    }
}