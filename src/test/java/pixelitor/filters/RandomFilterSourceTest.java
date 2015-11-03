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

package pixelitor.filters;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandomFilterSourceTest {
    private RandomFilterSource source;

    @BeforeClass
    public static void globalInit() {
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

    @Before
    public void setUp() {
        source = new RandomFilterSource();
    }

    @Test
    public void testNewSource() {
        checkHasNeitherPreviousOrNext();

        assertThat(source.getLastFilter()).isNull();
    }

    @Test(timeout=1000)
    public void testAfterOne() {
        Filter one = source.getRandom();
        checkHasNeitherPreviousOrNext();

        Filter lastFilter = source.getLastFilter();
        assertThat(lastFilter).isNotNull();
        assertThat(one).isEqualTo(lastFilter);
    }

    @Test(timeout=1000)
    public void testAfterTwo() {
        Filter one = source.getRandom();
        Filter two = source.getRandom();

        checkHasPreviousButNoNext();

        checkPreviousIs(one);
        checkHasNextButNoPrevious();

        checkNextIs(two);
        checkHasPreviousButNoNext();

        source.getRandom(); // three
        checkHasPreviousButNoNext();

        checkPreviousIs(two);
        checkHasBothPreviousAndNext();
    }

    @Test(timeout=1000)
    public void testMultipleBackForward() {
        Filter one = source.getRandom();
        Filter two = source.getRandom();

        for (int i = 0; i < 3; i++) {
            checkPreviousIs(one);
            checkHasNextButNoPrevious();

            checkNextIs(two);
            checkHasPreviousButNoNext();
        }
    }

    @Test(timeout=1000)
    public void testGenerateWhenBackInHistory() {
        Filter one = source.getRandom();
        Filter two = source.getRandom();
        Filter three = source.getRandom();
        source.getRandom(); // four

        checkPreviousIs(three);
        checkPreviousIs(two);

        // after going back a while start generating new filters
        Filter five = source.getRandom();
        checkHasPreviousButNoNext();

        source.getRandom(); // six
        checkHasPreviousButNoNext();

        // and now go back to verify the history
        checkPreviousIs(five);
        checkPreviousIs(two);
        checkPreviousIs(one);

        checkHasNextButNoPrevious();
    }

    private void checkPreviousIs(Filter filter) {
        Filter prev = source.getPrevious();
        assertThat(prev).isEqualTo(filter);
    }

    private void checkNextIs(Filter filter) {
        Filter next = source.getNext();
        assertThat(next).isEqualTo(filter);
    }

    private void checkHasNextButNoPrevious() {
        assertThat(source.hasPrevious()).isFalse();
        assertThat(source.hasNext()).isTrue();
    }

    private void checkHasNeitherPreviousOrNext() {
        assertThat(source.hasPrevious()).isFalse();
        assertThat(source.hasNext()).isFalse();
    }

    private void checkHasPreviousButNoNext() {
        assertThat(source.hasPrevious()).isTrue();
        assertThat(source.hasNext()).isFalse();
    }

    private void checkHasBothPreviousAndNext() {
        assertThat(source.hasPrevious()).isTrue();
        assertThat(source.hasNext()).isTrue();
    }
}