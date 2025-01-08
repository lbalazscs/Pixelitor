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

package pixelitor.layers;

import org.junit.jupiter.api.*;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.utils.MockFilter;

@DisplayName("Adjustment layer tests")
@TestMethodOrder(MethodOrderer.Random.class)
class AdjustmentLayerTest {
    private MockFilter filter;
    private AdjustmentLayer layer;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        filter = new MockFilter("Filter");
        Composition comp = TestHelper.createComp("AdjustmentLayerTest", 1, false, true);
        layer = new AdjustmentLayer(comp, "adjustment", filter);
        comp.addLayerWithoutUI(layer);
        comp.getCompositeImage();
        assert filter.getNumTransformCalls() == 1;
    }

    @Test
    void hiddenLayerDoesNotRunFilter() {
        layer.setVisible(false, false, true);
        layer.getComp().getCompositeImage();
        checkFilterRuns(0);

        // on the other hand a visible adjustment layer
        // runs its filter exactly once
        layer.setVisible(true, false, true);
        layer.getComp().getCompositeImage();
        checkFilterRuns(1);
    }

    private void checkFilterRuns(int expected) {
        // once it was run during setup
        int actual = filter.getNumTransformCalls() - 1;
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + ", found " + actual);
        }
    }
}
