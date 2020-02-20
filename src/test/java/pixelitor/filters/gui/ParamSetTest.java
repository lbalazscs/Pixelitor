/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    void beforeEachTest() {
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
    void reset() {
        params.reset();
        verify(adjustmentListener, never()).paramAdjusted();
    }

    @Test
    void randomize() {
        params.randomize();
        verify(adjustmentListener, never()).paramAdjusted();
    }

    @Test
    void filterTriggering() {
        extraParam.setValue(42, false);
        verify(adjustmentListener, never()).paramAdjusted();

        extraParam.setValue(43, true);
        verify(adjustmentListener, times(1)).paramAdjusted();

        params.runFilter();
        verify(adjustmentListener, times(2)).paramAdjusted();
    }

    @Test
    void copyState_setState() {
        CompositeState state = params.copyState();
        params.setState(state);

        verify(adjustmentListener, never()).paramAdjusted();
    }

    @Test
    void canBeAnimated() {
        assertThat(params.canBeAnimated()).isTrue();
    }

    @Test
    void setFinalAnimationSettingMode() {
        params.setFinalAnimationSettingMode(false);
        params.setFinalAnimationSettingMode(true);

        verify(adjustmentListener, never()).paramAdjusted();
    }

    @Test
    void hasGradient() {
        assertThat(params.hasGradient()).isTrue();
    }
}