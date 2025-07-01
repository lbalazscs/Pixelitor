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

package pixelitor.filters.gui;

import org.junit.jupiter.api.*;
import org.mockito.verification.VerificationMode;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.filters.ParamTestFilter;
import pixelitor.layers.ImageLayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ParamSet tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ParamSetTest {
    private ParamSet params;
    private ParamAdjustmentListener mockAdjustmentListener;
    private RangeParam extraParam;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        params = new ParamSet();
        params.addParams(ParamTestFilter.getTestParams());
        params.withReseedAction();
        params.addCommonActions();

        mockAdjustmentListener = mock(ParamAdjustmentListener.class);
        params.setAdjustmentListener(mockAdjustmentListener);

        // add an extra parameter that uses the adjustment listener defined here
        extraParam = new RangeParam("Extra Param", 0, 0, 200);
        extraParam.setAdjustmentListener(mockAdjustmentListener);
        params.insertParam(extraParam, 3);

        Composition comp = TestHelper.createRealComp("ParamSetTest", ImageLayer.class);
        ImageLayer layer = (ImageLayer) comp.getLayer(0);
        params.adaptToContext(layer, true);
    }

    @Test
    void resetShouldNotTriggerFilter() {
        params.reset();
        verifyFilterNotExecuted();
    }

    @Test
    void randomizeShouldNotTriggerFilter() {
        params.randomize();
        verifyFilterNotExecuted();
    }

    @Test
    void filterTriggering() {
        extraParam.setValue(42, false);
        verifyFilterNotExecuted();

        extraParam.setValue(43, true);
        verifyFilterExecuted(times(1));

        params.runFilter();
        verifyFilterExecuted(times(2));
    }

    @Test
    @DisplayName("copyState()/setState()")
    void copyState_setState() {
        // Set initial test value
        extraParam.setValue(75, false);

        // Copy state and modify current values
        FilterState savedState = params.copyState(true);
        extraParam.setValue(100, false);

        // Restore state and verify
        params.setState(savedState, true);
        assertThat(extraParam.getValue()).isEqualTo(75);
        verifyFilterNotExecuted();
    }

    @Test
    void isAnimatable() {
        assertThat(params.isAnimatable()).isTrue();
    }

    @Test
    void setFinalAnimationMode() {
        params.setFinalAnimationMode(false);
        params.setFinalAnimationMode(true);

        verifyFilterNotExecuted();
    }

    @Test
    void hasGradient() {
        assertThat(params.hasGradient()).isTrue();
    }

    @Test
    void setShouldHandleInvalidNames() {
        assertThatThrownBy(() -> params.set("NonexistentParam", "value"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No param called NonexistentParam");
    }

    @Test
    void afterResetActionShouldBeExecuted() {
        // Set up a mock after reset action
        Runnable mockAfterResetAction = mock(Runnable.class);
        params.setAfterResetAllAction(mockAfterResetAction);

        // Perform reset and verify action was executed
        params.reset();
        verify(mockAfterResetAction, times(1)).run();
    }

    @Test
    void shouldSaveAndRestoreUserPreset() {
        UserPreset preset = new UserPreset("ParamSetTest");
        extraParam.setValue(36, false);

        params.saveStateTo(preset);
        extraParam.setValue(72, false);
        verifyFilterNotExecuted();

        params.loadUserPreset(preset);
        assertThat(extraParam.getValue()).isEqualTo(36);
        verifyFilterExecuted(times(1));
    }

    @Test
    void shouldSaveAndRestoreFilterState() {
        extraParam.setValue(36, false);

        // save an animation state so that it doesn't
        // try to update the gui when restoring it
        FilterState filterState = params.copyState(true);
        extraParam.setValue(72, false);
        verifyFilterNotExecuted();

        params.setState(filterState, true);
        assertThat(extraParam.getValue()).isEqualTo(36);
        verifyFilterNotExecuted();
    }

    private void verifyFilterExecuted(VerificationMode times) {
        verify(mockAdjustmentListener, times).paramAdjusted();
    }

    private void verifyFilterNotExecuted() {
        verifyFilterExecuted(never());
    }
}