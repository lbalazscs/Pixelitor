/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

public class ParamSetTest {
    private ParamSet params;
    private ParamAdjustmentListenerSpy adjustmentListener;
    private RangeParam extraParam;

    @Before
    public void setUp() throws Exception {
        params = new ParamSet(ParamTest.getTestParams())
                .withAction(ReseedSupport.createAction())
                .addCommonActions();
        adjustmentListener = new ParamAdjustmentListenerSpy();
        params.setAdjustmentListener(adjustmentListener);
        extraParam = new RangeParam("Extra Param", 0, 200, 0);
        extraParam.setAdjustmentListener(adjustmentListener);
        params.insertParam(extraParam, 3);
        params.considerImageSize(new Rectangle(0, 0, 400, 800));
    }

    @Test
    public void testReset() {
        params.reset();
        checkThatFilterWasNotCalled();
    }

    @Test
    public void testRandomize() {
        params.randomize();
        checkThatFilterWasNotCalled();
    }

    @Test
    public void testFilterTriggering() {
        extraParam.setValue(42, false);
        checkThatFilterWasNotCalled();

        extraParam.setValue(43, true);
        checkThatFilterWasCalled(1);

        params.triggerFilter();
        checkThatFilterWasCalled(2);
    }

    @Test
    public void testCopyAnSetState() {
        ParamSetState state = params.copyState();
        params.setState(state);

        checkThatFilterWasNotCalled();
    }

    @Test
    public void testCanBeAnimated() {
        assertThat(params.canBeAnimated()).isTrue();
    }

    @Test
    public void testSetFinalAnimationSettingMode() {
        params.setFinalAnimationSettingMode(false);
        params.setFinalAnimationSettingMode(true);

        checkThatFilterWasNotCalled();
    }

    @Test
    public void testHasGradient() {
        assertThat(params.hasGradient()).isTrue();
    }

    private void checkThatFilterWasNotCalled() {
        assertThat(adjustmentListener.getNumCalled()).isEqualTo(0);
    }

    private void checkThatFilterWasCalled(int n) {
        assertThat(adjustmentListener.getNumCalled()).isEqualTo(n);
    }
}