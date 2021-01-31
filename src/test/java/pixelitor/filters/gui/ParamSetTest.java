/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.filters.ParamTest;
import pixelitor.utils.ReseedSupport;

import java.awt.Dimension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ParamSet tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ParamSetTest {
    private ParamSet params;
    private ParamAdjustmentListener adjustmentListener;
    private RangeParam extraParam;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        params = new ParamSet(ParamTest.getTestParams())
            .withAction(ReseedSupport.createAction())
            .addCommonActions();
        adjustmentListener = mock(ParamAdjustmentListener.class);
        params.setAdjustmentListener(adjustmentListener);
        extraParam = new RangeParam("Extra Param", 0, 0, 200);
        extraParam.setAdjustmentListener(adjustmentListener);
        params.insertParamAtIndex(extraParam, 3);
        params.adaptToImageSize(new Dimension(400, 800));
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
    @DisplayName("copyState()/setState()")
    void copyState_setState() {
        FilterState state = params.copyState(true);
        params.setState(state, true);

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