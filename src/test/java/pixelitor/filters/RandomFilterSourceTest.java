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

package pixelitor.filters;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("RandomFilterSource tests")
@TestMethodOrder(MethodOrderer.Random.class)
public class RandomFilterSourceTest {
    private RandomFilterSource source;

    @BeforeAll
    static void beforeAllTests() {
        // generate mock filters with the names "A", "B" ... "Z"
        for (int i = 'A'; i < 'Z' + 1; i++) {
            char[] charsInFilterName = {(char) i};
            String filterName = new String(charsInFilterName);
            Filter filter = mock(Filter.class);
            FilterAction filterAction = mock(FilterAction.class);

            when(filterAction.getName()).thenReturn(filterName);
            when(filter.getName()).thenReturn(filterName);
            when(filterAction.getFilter()).thenReturn(filter);

            FilterUtils.addFilter(filterAction);
        }
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
                .lastFilterIsNull();
    }

    @Test
    @Timeout(value = 1, unit = SECONDS)
        // make sure there is no infinite loop
    void afterOne() {
        Filter one = source.choose();

        assertThat(source)
                .doesNotHavePrevious()
                .doesNotHaveNext()
                .lastFilterIsNotNull()
                .lastFilterIs(one);
    }

    @Test
    @Timeout(value = 1, unit = SECONDS)
        // make sure there is no infinite loop
    void afterTwo() {
        Filter one = source.choose();
        Filter two = source.choose();

        assertThat(source)
                .hasPrevious()
                .doesNotHaveNext()
                .previousFilterIs(one) // this calls getPrevious, and changes the state
                .doesNotHavePrevious()
                .hasNext()
                .nextFilterIs(two) // this calls getNext, and changes the state
                .hasPrevious()
                .doesNotHaveNext();

        source.choose(); // three
        assertThat(source)
                .hasPrevious()
                .doesNotHaveNext()
                .previousFilterIs(two) // this calls getPrevious, and changes the state
                .hasPrevious()
                .hasNext();
    }

    @Test
    @Timeout(value = 1, unit = SECONDS)
        // make sure there is no infinite loop
    void multipleBackForward() {
        Filter one = source.choose();
        Filter two = source.choose();

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
    @Timeout(value = 1, unit = SECONDS)
        // make sure there is no infinite loop
    void generateWhenBackInHistory() {
        Filter one = source.choose();
        Filter two = source.choose();
        Filter three = source.choose();
        source.choose(); // four

        assertThat(source)
                .previousFilterIs(three)
                .previousFilterIs(two);

        // after going back a while start generating new filters
        Filter five = source.choose();
        assertThat(source)
                .hasPrevious()
                .doesNotHaveNext();

        source.choose(); // six
        assertThat(source)
                .hasPrevious()
                .doesNotHaveNext();

        // and now go back to verify the history
        assertThat(source)
                .previousFilterIs(five)
                .previousFilterIs(two)
                .previousFilterIs(one)
                .doesNotHavePrevious()
                .hasNext();
    }
}