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

package pixelitor.filters;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.Composition;
import pixelitor.FilterContext;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.layers.ImageLayer;
import pixelitor.testutils.WithSelection;
import pixelitor.testutils.WithTranslation;

import java.awt.Component;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * Tests to verify that the filter undo works
 * correctly under all circumstances (translation, selection)
 */
@DisplayName("Filter History Test")
class FilterHistoryTest {
    private final Filter filter = new Invert();
    private final Component busyCursorParent = mock(Component.class);

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(WithTranslation.NO, WithSelection.NO),
            Arguments.of(WithTranslation.YES, WithSelection.NO),
            Arguments.of(WithTranslation.NO, WithSelection.YES),
            Arguments.of(WithTranslation.YES, WithSelection.YES)
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void filterHistory(WithTranslation withTranslation, WithSelection withSelection) {
        Composition comp = TestHelper.createComp("FilterHistoryTest", 1, false);
        withTranslation.move(comp);
        withSelection.configure(comp);

        History.clear();
        ImageLayer layer = (ImageLayer) comp.getActiveLayer();
        layer.startFilter(filter, FilterContext.FILTER_WITHOUT_DIALOG, busyCursorParent);

        TestHelper.assertHistoryEditsAre("Invert");
    }
}
