/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guides;

import org.junit.Before;
import org.junit.Test;
import pixelitor.Composition;
import pixelitor.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class GuidesTest {
    private Guides guides;

    @Before
    public void setUp() {
        Composition comp = TestHelper.createMockComposition();
        guides = new Guides(comp);
    }

    @Test
    public void testAddHorRelative() {
        guides.addHorRelative(0.25);
        assertThat(guides.getHorizontals()).containsOnly(0.25);
    }

    @Test
    public void testAddHorAbsolute() {
        guides.addHorAbsolute((int) (TestHelper.TEST_WIDTH * 0.75));
        assertThat(guides.getHorizontals()).containsOnly(0.75);
    }

    @Test
    public void testAddVerRelative() {
        guides.addVerRelative(0.33);
        assertThat(guides.getVerticals()).containsOnly(0.33);
    }

    @Test
    public void testAddVerAbsolute() {
        guides.addVerAbsolute((int) (TestHelper.TEST_HEIGHT * 0.5));
        assertThat(guides.getVerticals()).containsOnly(0.5);
    }

    @Test
    public void testAddRelativeGrid() {
        guides.addRelativeGrid(4, 4);
        assertThat(guides.getHorizontals()).containsOnly(0.25, 0.5, 0.75);
        assertThat(guides.getVerticals()).containsOnly(0.25, 0.5, 0.75);
    }

    @Test
    public void testAddAbsoluteGrid() {
        int horDist = (int) (TestHelper.TEST_HEIGHT * 0.2);
        int verDist = (int) (TestHelper.TEST_WIDTH * 0.25);
        guides.addAbsoluteGrid(2, horDist, 2, verDist);
        assertThat(guides.getHorizontals()).containsOnly(0.2, 0.4);
        assertThat(guides.getVerticals()).containsOnly(0.25, 0.5);
    }
}