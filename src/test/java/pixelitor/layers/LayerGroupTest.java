/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.layers;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;

@DisplayName("layer group tests")
@TestMethodOrder(MethodOrderer.Random.class)
class LayerGroupTest {
    private Composition comp;

    private LayerGroup shallowGroup;
    private LayerGroup deepGroup;
    private LayerGroup nestedGroupA;
    private LayerGroup nestedGroupB;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        History.clear();
    }

    @Test
    void copyCompWithGroups() {
    }

}
