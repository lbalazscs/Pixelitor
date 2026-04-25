/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.TestHelper;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.jhlabsproxies.JHWeave;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import static pixelitor.filters.gui.FilterSetting.EnabledReason.FILTER_LOGIC;
import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;
import static pixelitor.filters.gui.TransparencyMode.OPAQUE_ONLY;

/**
 * Checks whether different {@link FilterParam} implementations
 * adhere to the interface contract.
 */
@ParameterizedClass
@MethodSource("instancesToTest")
@DisplayName("Filter Parameter Tests")
@TestMethodOrder(MethodOrderer.Random.class)
class FilterParamTest {
    @Parameter
    private AbstractFilterParam param;

    private ParamAdjustmentListener mockAdjustmentListener;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        mockAdjustmentListener = mock(ParamAdjustmentListener.class);
        param.setAdjustmentListener(mockAdjustmentListener);
    }

    static Stream<AbstractFilterParam> instancesToTest() {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        return Stream.of(
            new RangeParam("Param Name", 0, 0, 10),
            new RangeWithColorsParam(CYAN, RED, "Param Name", -100, 0, 100),
            new GroupedRangeParam("Param Name", 0, 0, 100, true),
            new GroupedRangeParam("Param Name", 0, 0, 100, false),
            new ImagePositionParam("Param Name"),
            new GradientParam("Param Name", BLACK, WHITE),
            new TextParam("Param Name", "default text", true),
            new ColorParam("Param Name", BLACK, ALPHA_ENABLED),
            new ColorParam("Param Name", WHITE, MANUAL_ALPHA_ONLY),
            new ColorParam("Param Name", BLUE, OPAQUE_ONLY),
            new ColorListParam("Param Name", 1, 1, BLACK, BLUE),
            new GroupedColorsParam("Param Name", "Name 1", BLUE, "Name 2", BLUE, ALPHA_ENABLED, true, true),
            new BooleanParam("Param Name", true, RandomizeMode.ALLOW_RANDOMIZE, true),
            new AngleParam("Param Name", 0),
            new ElevationAngleParam("Param Name", 0),
            new IntChoiceParam("Param Name", new Item[]{
                new Item("Better", 0),
                new Item("Faster", 1),
            }),
            new StrokeParam("Param Name"),
            new EffectsParam("Param Name"),
            new CompositeParam("Param Name",
                new RangeParam("Child 1", 0, 50, 100),
                new AngleParam("Child 2", 0),
                new BooleanParam("Child 3", true)),
            new LogZoomParam("Param Name", 200, 200, 1000),
            new GridParam("Param Name", JHWeave.WEAVE_PRESETS, GridCellPainter.createForWeave())
        );
    }

    @Test
    void shouldCreateWorkingGUI() {
        JComponent gui = param.createGUI();
        assertThat(gui)
            .as(param + " gui")
            .isNotNull()
            .isInstanceOf(ParamGUI.class);
        assertIsEnabled(gui);

        ParamGUI paramGUI = (ParamGUI) gui;

        param.setEnabled(false);
        assertIsDisabled(gui);

        param.setEnabled(true);
        assertIsEnabled(gui);

        paramGUI.updateGUI();

        verifyNoParamAdjustments();
    }

    @Test
    void shouldHaveValidLayoutColumnCount() {
        int columnCount = ((ParamGUI) param.createGUI()).getNumLayoutColumns();

        assertThat(columnCount)
            .as("layout column count")
            .isIn(1, 2);

        verifyNoParamAdjustments();
    }

    @Test
    void shouldHandleRandomization() {
        // test allowed randomization
        param.setRandomizeMode(RandomizeMode.ALLOW_RANDOMIZE);
        assertThat(param).shouldRandomize();
        param.randomize();
        verifyNoParamAdjustments();

        // test ignored randomization
        String origValue = param.getValueAsString();
        param.setRandomizeMode(RandomizeMode.IGNORE_RANDOMIZE);
        assertThat(param).shouldNotRandomize();
        param.randomize();
        assertThat(param).valueAsStringIs(origValue); // it didn't change
        verifyNoParamAdjustments();
    }

    @Test
    void shouldResetWithoutTriggering() {
        param.reset(false);

        verifyNoParamAdjustments();
        assertThat(param).isAtDefault();
    }

    @Test
    void shouldResetWithTriggering() {
        // check that another test didn't leave the param disabled
        assertThat(param).isEnabled();

        param.reset(false);
        String defaultValue = param.getValueAsString();

        randomizeUntilNotDefault();

        assertThat(param.getValueAsString())
            .isNotNull()
            .isNotEqualTo(defaultValue);

        // finally we can test reset with triggering
        param.reset(true);

        assertThat(param)
            .isAtDefault()
            .valueAsStringIs(defaultValue);

        // check that it was triggered once
        verify(mockAdjustmentListener, times(1)).paramAdjusted();
    }

    /**
     * Randomizes the parameter until it is no longer at its default value.
     * This is the only general way to change the value.
     */
    private void randomizeUntilNotDefault() {
        param.setRandomizeMode(RandomizeMode.ALLOW_RANDOMIZE);
        int attempts = 0;
        while (param.isAtDefault() && attempts < 100) {
            param.randomize();
            verifyNoParamAdjustments();
            attempts++;
        }
    }

    @Test
    void shouldPreserveStateWhenCopiedAndRestored() {
        String origValue = param.getValueAsString();

        ParamState<?> paramState = param.copyState();
        assertThat(paramState).isNotNull();

        param.loadStateFrom(paramState, false);

        assertThat(param)
            .as("restored " + param.toString())
            .valueAsStringIs(origValue);

        verifyNoParamAdjustments();
    }

    @Test
    void shouldNotTriggerFilterForStateChanges() {
        assertThat(param)
            .as(param.toString())
            .hasName("Param Name");

        JComponent gui = param.createGUI();

        param.isAnimatable();

        // test FILTER_LOGIC disable/enable
        param.setEnabled(false, FILTER_LOGIC);
        assertIsDisabled(gui);

        param.setEnabled(true, FILTER_LOGIC);
        assertIsEnabled(gui);

        // test ANIMATION_ENDING_STATE disable/enable
        param.setEnabled(false, ANIMATION_ENDING_STATE);
        if (param.isAnimatable()) {
            assertIsEnabled(gui); // unaffected
        } else {
            assertIsDisabled(gui); // disabled
        }

        param.setEnabled(true, ANIMATION_ENDING_STATE);
        assertIsEnabled(gui);

        verifyNoParamAdjustments();
    }

    @Test
    void shouldDisableSideButtonsWhenParamIsDisabled() {
        String paramClass = param.getClass().getSimpleName();

        // inject a side button with a known lookup name so we can find it in the GUI
        String sideButtonName = "testSideButton";
        FilterButtonModel sideButtonModel = new FilterButtonModel(
            "Side", () -> {}, null, "Tooltip", sideButtonName
        );
        param.withSideButton(sideButtonModel);
        JComponent gui = param.createGUI();

        // randomize until the parent parameter is NOT at default
        param.setRandomizeMode(RandomizeMode.ALLOW_RANDOMIZE);
        int attempts = 0;
        while (param.isAtDefault() && attempts < 100) {
            param.randomize();
            attempts++;
        }

        // find ALL reset buttons and the side button (some parameters,
        // such as GroupedRangeParam, don't have a single reset button
        // for the group itself because the children have their own reset buttons)
        List<ResetButton> resetButtons = findAllChildComponents(gui, ResetButton.class);
        JButton sideButton = findChildComponent(gui, JButton.class, btn -> sideButtonName.equals(btn.getName()));

        // capture the expected state of each reset button BEFORE disabling
        List<Boolean> expectedResetStates = resetButtons.stream()
            .map(ResetButton::isEnabled)
            .toList();

        // disable the parameter and verify all buttons are disabled
        param.setEnabled(false);
        for (ResetButton btn : resetButtons) {
            assertThat(btn.isEnabled())
                .as(paramClass + ": ResetButton should be disabled when param is disabled")
                .isFalse();
        }
        if (sideButton != null) {
            assertThat(sideButton.isEnabled())
                .as(paramClass + ": Side button should be disabled when param is disabled")
                .isFalse();
        }

        // re-enable the parameter and verify all buttons return to their captured expected states
        param.setEnabled(true);
        for (int i = 0; i < resetButtons.size(); i++) {
            assertThat(resetButtons.get(i).isEnabled())
                .as(paramClass + ": ResetButton enabled state should be correctly restored")
                .isEqualTo(expectedResetStates.get(i));
        }
        if (sideButton != null) {
            assertThat(sideButton.isEnabled())
                .as(paramClass + ": Side button should be enabled when param is enabled")
                .isTrue();
        }
    }

    /**
     * Helper to find all components of a specific type in a Container hierarchy.
     */
    private static <T extends Component> List<T> findAllChildComponents(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component c : container.getComponents()) {
            if (type.isInstance(c)) {
                found.add(type.cast(c));
            }
            if (c instanceof Container childContainer) {
                found.addAll(findAllChildComponents(childContainer, type));
            }
        }
        return found;
    }

    /**
     * Helper to find a component of a specific type matching a condition in a Container hierarchy.
     */
    private static <T extends Component> T findChildComponent(Container container, Class<T> type, Predicate<T> matcher) {
        for (Component c : container.getComponents()) {
            if (type.isInstance(c)) {
                T typedComp = type.cast(c);
                if (matcher.test(typedComp)) {
                    return typedComp;
                }
            }
            if (c instanceof Container childContainer) {
                T found = findChildComponent(childContainer, type, matcher);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void verifyNoParamAdjustments() {
        verify(mockAdjustmentListener, never()).paramAdjusted();
    }

    private void assertIsEnabled(JComponent gui) {
        String paramDesc = param.toString();
        assertThat(param)
            .as(paramDesc)
            .isEnabled();
        assertThat(gui.isEnabled())
            .as("gui of " + paramDesc)
            .isTrue();
    }

    private void assertIsDisabled(JComponent gui) {
        String paramDesc = param.toString();
        assertThat(param)
            .as(paramDesc)
            .isDisabled();
        assertThat(gui.isEnabled())
            .as("gui of " + paramDesc)
            .isFalse();
    }
}
