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

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@DisplayName("Smart Object tests")
@TestMethodOrder(MethodOrderer.Random.class)
class SmartObjectTest {
    private SmartObject smartObject;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        Composition content = TestHelper.createComp(0, false, false);
        Composition comp = TestHelper.createEmptyComp();
        smartObject = new SmartObject(comp, content);
        comp.addLayerInInitMode(smartObject);
    }

    @Test
    void cloneSmartObject() {
        Composition comp = smartObject.getComp();
        assertThat(comp)
            .numLayersIs(1)
            .typeOfLayerNIs(0, SmartObject.class);

        comp.shallowDuplicate(smartObject);

        assertThat(comp)
            .numLayersIs(2)
            .typeOfLayerNIs(0, SmartObject.class)
            .typeOfLayerNIs(1, SmartObject.class);
        Composition firstContent = ((SmartObject) comp.getLayer(0)).getContent();
        Composition secondContent = ((SmartObject) comp.getLayer(1)).getContent();
        assert firstContent == secondContent;

        History.undo("Clone");
        assertThat(comp).numLayersIs(1);

        History.redo("Clone");
        assertThat(comp).numLayersIs(2);
    }
}
