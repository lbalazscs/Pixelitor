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

package pixelitor.guides;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Guides tests")
@TestMethodOrder(MethodOrderer.Random.class)
class GuidesTest {
    private Guides guides;
    private Composition comp;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createMockComp();
        guides = new Guides();
    }

    @Test
    @DisplayName("horizontal line, relative")
    void addHorRelative() {
        guides.addHorRelative(0.25);
        assertThat(guides.getHorizontals()).containsOnly(0.25);
    }

    @Test
    @DisplayName("horizontal line, absolute")
    void addHorAbsolute() {
        guides.addHorAbsolute((int) (TestHelper.TEST_WIDTH * 0.75), comp.getCanvas());
        assertThat(guides.getHorizontals()).containsOnly(0.75);
    }

    @Test
    @DisplayName("vertical line, relative")
    void addVerRelative() {
        guides.addVerRelative(0.33);
        assertThat(guides.getVerticals()).containsOnly(0.33);
    }

    @Test
    @DisplayName("vertical line, absolute")
    void addVerAbsolute() {
        guides.addVerAbsolute((int) (TestHelper.TEST_HEIGHT * 0.5), comp.getCanvas());
        assertThat(guides.getVerticals()).containsOnly(0.5);
    }

    @Test
    @DisplayName("relative grid")
    void addRelativeGrid() {
        guides.addRelativeGrid(4, 4);
        assertThat(guides.getHorizontals()).containsOnly(0.25, 0.5, 0.75);
        assertThat(guides.getVerticals()).containsOnly(0.25, 0.5, 0.75);
    }
}