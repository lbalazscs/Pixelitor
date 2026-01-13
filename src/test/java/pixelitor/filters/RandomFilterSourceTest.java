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

package pixelitor.filters;

import org.junit.jupiter.api.*;
import pixelitor.TestHelper;
import pixelitor.filters.util.FilterAction;
import pixelitor.filters.util.Filters;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("RandomFilterSource tests")
@TestMethodOrder(MethodOrderer.Random.class)
class RandomFilterSourceTest {
    private RandomFilterSource source;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();

        // generate mock filters with the names "A", "B", ... "Z"
        //noinspection CharacterComparison
        for (char c = 'A'; c <= 'Z'; c++) {
            String filterName = String.valueOf(c);
            Filter filter = mock(Filter.class);
            FilterAction filterAction = mock(FilterAction.class);

            when(filterAction.getName()).thenReturn(filterName);
            when(filter.getName()).thenReturn(filterName);
            when(filterAction.getFilter()).thenReturn(filter);

            Filters.register(filterAction);
        }
        Filters.finishedRegistering();
    }

    @BeforeEach
    void beforeEachTest() {
        source = new RandomFilterSource();
    }

    @Test
    void newSource() {
        assertThat(source)
            .doesNotHavePrevious()
            .doesNotHaveNext()
            .currentFilterIsNull();
    }

    @Test
    @Timeout(value = 1, unit = SECONDS) // ensure there is no infinite loop
    void afterOne() {
        Filter one = source.selectNewFilter();

        assertThat(source)
            .doesNotHavePrevious()
            .doesNotHaveNext()
            .currentFilterIsNotNull()
            .currentFilterIs(one);
    }

    @Test
    @Timeout(value = 1, unit = SECONDS) // ensure there is no infinite loop
    void afterTwo() {
        Filter one = source.selectNewFilter();
        Filter two = source.selectNewFilter();

        assertThat(source)
            .hasPrevious()
            .doesNotHaveNext()
            .previousFilterIs(one) // this calls getPrevious, and changes the state
            .doesNotHavePrevious()
            .hasNext()
            .nextFilterIs(two) // this calls getNext, and changes the state
            .hasPrevious()
            .doesNotHaveNext();

        source.selectNewFilter(); // three
        assertThat(source)
            .hasPrevious()
            .doesNotHaveNext()
            .previousFilterIs(two) // this calls getPrevious, and changes the state
            .hasPrevious()
            .hasNext();
    }

    @Test
    @Timeout(value = 1, unit = SECONDS) // ensure there is no infinite loop
    void multipleBackForward() {
        Filter one = source.selectNewFilter();
        Filter two = source.selectNewFilter();

        for (int i = 0; i < 3; i++) {
            assertThat(source)
                .previousFilterIs(one) // this calls getPrevious, and changes the state
                .doesNotHavePrevious()
                .hasNext()
                .nextFilterIs(two) // this calls getNext, and changes the state
                .hasPrevious()
                .doesNotHaveNext();
        }
    }

    @Test
    @Timeout(value = 1, unit = SECONDS) // ensure there is no infinite loop
    void generateWhenBackInHistory() {
        Filter one = source.selectNewFilter();
        Filter two = source.selectNewFilter();
        Filter three = source.selectNewFilter();
        source.selectNewFilter(); // four

        // each call goes back in history
        assertThat(source)
            .previousFilterIs(three)
            .previousFilterIs(two);

        // start generating new filters
        Filter five = source.selectNewFilter();
        assertThat(source)
            .hasPrevious()
            .doesNotHaveNext();

        source.selectNewFilter(); // six
        assertThat(source)
            .hasPrevious()
            .doesNotHaveNext();

        // go back to verify the history
        assertThat(source)
            .previousFilterIs(five)
            .previousFilterIs(two)
            .previousFilterIs(one)
            .doesNotHavePrevious()
            .hasNext();
    }
}
