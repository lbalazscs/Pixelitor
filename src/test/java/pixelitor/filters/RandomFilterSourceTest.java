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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RandomFilterSourceTest {
    private RandomFilterSource source;

    @BeforeClass
    public static void globalInit() {
        for (int i = 'A'; i < 'Z' + 1; i++) {
            char[] charsInFilterName = {(char) i};
            FilterUtils.addFilter(new FilterSpy(new String(charsInFilterName)));
        }
    }

    @Before
    public void setUp() {
        source = new RandomFilterSource();
    }

    @Test
    public void testNewSource() {
        checkHasNeitherPreviousOrNext();

        assertNull(source.getLastFilter());
    }

    @Test
    public void testAfterOne() {
        Filter one = source.getRandom();
        checkHasNeitherPreviousOrNext();

        Filter lastFilter = source.getLastFilter();
        assertNotNull(lastFilter);
        assertEquals(lastFilter, one);
    }

    @Test
    public void testAfterTwo() {
        Filter one = source.getRandom();
        Filter two = source.getRandom();

        checkHasPreviousButNoNext();

        checkPreviousIs(one);
        checkHasNextButNoPrevious();

        checkNextIs(two);
        checkHasPreviousButNoNext();

        Filter three = source.getRandom();
        checkHasPreviousButNoNext();

        checkPreviousIs(two);
        checkHasBothPreviousAndNext();
    }

    @Test
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

    @Test
    public void testGenerateWhenBackInHistory() {
        Filter one = source.getRandom();
        Filter two = source.getRandom();
        Filter three = source.getRandom();
        Filter four = source.getRandom();

        checkPreviousIs(three);
        checkPreviousIs(two);

        // after going back a while start generating new filters
        Filter five = source.getRandom();
        checkHasPreviousButNoNext();

        Filter six = source.getRandom();
        checkHasPreviousButNoNext();

        // and now go back to verify the history
        checkPreviousIs(five);
        checkPreviousIs(two);
        checkPreviousIs(one);

        checkHasNextButNoPrevious();
    }

    private void checkPreviousIs(Filter filter) {
        Filter prev = source.getPrevious();
        if (!prev.equals(filter)) {
            System.out.println("RandomFilterSourceTest::checkPreviousIs: source = " + source);
        }
        assertEquals(filter, prev);
    }

    private void checkNextIs(Filter filter) {
        Filter next = source.getNext();
        assertEquals(filter, next);
    }

    private void checkHasNextButNoPrevious() {
        assertFalse(source.hasPrevious());
        assertTrue(source.hasNext());
    }

    private void checkHasNeitherPreviousOrNext() {
        assertFalse(source.hasPrevious());
        assertFalse(source.hasNext());
    }

    private void checkHasPreviousButNoNext() {
        assertTrue(source.hasPrevious());
        assertFalse(source.hasNext());
    }

    private void checkHasBothPreviousAndNext() {
        assertTrue(source.hasPrevious());
        assertTrue(source.hasNext());
    }
}