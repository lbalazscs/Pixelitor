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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    private RangeParam testedParam;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode(true);
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
        testedParam = new RangeParam("Tested Param", 0, 0, 200);
        testedParam.setAdjustmentListener(mockAdjustmentListener);
        params.insertParam(testedParam, 3);

        Composition comp = TestHelper.createRealComp("ParamSetTest", ImageLayer.class);
        ImageLayer layer = (ImageLayer) comp.getLayer(0);
        params.adaptToContext(layer, true);
    }

    @AfterEach
    void afterEachTest() {
        TestHelper.verifyAndClearHistory();
    }

    @Test
    void resetShouldNotNotifyListener() {
        params.reset();
        verifyListenerNotified(never());
    }

    @Test
    void randomizeShouldNotNotifyListener() {
        params.randomize();
        verifyListenerNotified(never());
    }

    @Test
    void shouldNotifyListenerOnValueChangeAndRun() {
        testedParam.setValue(42, false);
        verifyListenerNotified(never());

        testedParam.setValue(43, true);
        verifyListenerNotified(times(1));

        params.runFilter();
        verifyListenerNotified(times(2));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSaveAndRestoreFilterState(boolean forAnimation) {
        // set initial test value
        int originalValue = 36;
        int differentValue = 72;
        testedParam.setValue(originalValue, false);

        // save a FilterState
        FilterState savedState = params.copyState(forAnimation);
        testedParam.setValue(differentValue, false);
        verifyListenerNotified(never());

        // restore state and verify
        params.setState(savedState, forAnimation);
        assertThat(testedParam.getValue()).isEqualTo(originalValue);
        verifyListenerNotified(never());

        // apply the state, which should restore the value and run the filter
        testedParam.setValue(differentValue, false);
        params.applyState(savedState, false);
        verifyListenerNotified(times(1));
    }

    @Test
    void isAnimatable() {
        assertThat(params.isAnimatable()).isTrue();

        ParamSet nonAnimatableParams = new ParamSet();
        assertThat(nonAnimatableParams.isAnimatable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void setFinalAnimationMode(boolean value) {
        params.setAnimationEndStateMode(value);
        verifyListenerNotified(never());
    }

    @Test
    void hasGradient() {
        assertThat(params.hasGradient()).isTrue();

        ParamSet noGradientParams = new ParamSet();
        assertThat(noGradientParams.hasGradient()).isFalse();
    }

    @Test
    void setShouldUpdateCorrectParamValue() {
        // set the value of "Tested Param" using its string name
        params.set("Tested Param", "123");

        assertThat(testedParam.getValue()).isEqualTo(123);
        // verify that setting a value via this method does not trigger the listener
        verifyListenerNotified(never());
    }

    @Test
    void setShouldHandleInvalidNames() {
        assertThatThrownBy(() -> params.set("NonexistentParam", "value"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No param called NonexistentParam");
    }

    @Test
    void afterResetActionShouldBeExecuted() {
        // set up a mock after reset action
        Runnable mockAfterResetAction = mock(Runnable.class);
        params.setAfterResetAllAction(mockAfterResetAction);

        // perform reset and verify action was executed
        params.reset();
        verify(mockAfterResetAction, times(1)).run();
    }

    @Test
    void shouldSaveAndRestoreUserPreset() {
        UserPreset preset = new UserPreset("ParamSetTest");
        testedParam.setValue(36, false);

        params.saveStateTo(preset);
        testedParam.setValue(72, false);
        verifyListenerNotified(never());

        params.loadUserPreset(preset);
        assertThat(testedParam.getValue()).isEqualTo(36);
        verifyListenerNotified(times(1));
    }

    private void verifyListenerNotified(VerificationMode times) {
        verify(mockAdjustmentListener, times).paramAdjusted();
    }
}
