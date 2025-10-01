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
import pixelitor.colors.Colors;
import pixelitor.history.History;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

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
        Composition content = TestHelper.createComp("SmartObjectTest Content", 1, false, true);
        Composition comp = TestHelper.createEmptyComp("SmartObjectTest");
        smartObject = new SmartObject(comp, content);
        comp.addLayerWithoutUI(smartObject);

        assertThat(smartObject).invariantsAreOK();
    }

    @Test
    void shallowDuplicate_shouldCreateCloneWithSharedContent() {
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
        assertThat(firstContent).isSameAs(secondContent);

        History.undo("Clone");
        assertThat(comp).numLayersIs(1);

        History.redo("Clone");
        assertThat(comp).numLayersIs(2);
    }

    @Test
    void whenSmartObjectContentIsModified_thenParentLayerUpdates() {
        // capture the initial state of the smart object's rendered image
        BufferedImage beforeImage = ImageUtils.copyImage(smartObject.getVisibleImage());
        int initialPixel = beforeImage.getRGB(0, 0);

        // modify the content of the smart object
        Composition content = smartObject.getContent();
        ImageLayer contentLayer = (ImageLayer) content.getLayer(0);
        Colors.fillWith(Color.RED, contentLayer.getImage());

        // propagate the changes from the content to its smart object owner
        smartObject.propagateContentChanges(content, true);

        // capture the final state
        BufferedImage afterImage = smartObject.getVisibleImage();
        int finalPixel = afterImage.getRGB(0, 0);

        // verify that the smart object's image has been updated
        assertThat(finalPixel).isNotEqualTo(initialPixel);
        assertThat(finalPixel).isEqualTo(Color.RED.getRGB());
    }
}
