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
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.compactions.Outsets;
import pixelitor.filters.painters.TextSettings;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;
import pixelitor.utils.Rnd;

import java.awt.Shape;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@ParameterizedClass(name = "mask = {0}")
@EnumSource(WithMask.class)
@DisplayName("text layer tests")
@TestMethodOrder(MethodOrderer.Random.class)
class TextLayerTest {
    private TextLayer layer;
    private Composition comp;
    private IconUpdateChecker iconChecker;

    @Parameter
    private WithMask withMask;

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createEmptyComp("TextLayerTest");
        layer = TestHelper.createTextLayer(comp, "Text Layer");
        layer.updateLayerName();
        comp.add(layer);

        withMask.configure(layer);
        LayerMask mask = null;
        if (withMask.isTrue()) {
            mask = layer.getMask();
        }

        iconChecker = new IconUpdateChecker(layer, mask);

        assert layer.getComp().checkInvariants();
        History.clear();
    }

    @Test
    void replaceWithRasterized() {
        checkBeforeRasterizationState();

        layer.replaceWithRasterized();
        checkAfterRasterizationState();
        History.assertNumEditsIs(1);

        History.undo("Rasterize Text Layer");
        checkBeforeRasterizationState();

        History.redo("Rasterize Text Layer");
        checkAfterRasterizationState();

        // the layer icon is updated when rasterizing, but not
        // through an external updateLayerIconImageAsync call
        iconChecker.verifyUpdateCounts(0, 0);
    }

    private void checkBeforeRasterizationState() {
        assertThat(comp)
            .numLayersIs(1)
            .typeOfLayerNIs(0, TextLayer.class);
        assertThat(layer).hasUI();
        if (withMask.isTrue()) {
            assertThat(layer).hasMask();
            assertThat(layer.getMask()).hasUI();
        }
    }

    private void checkAfterRasterizationState() {
        assertThat(comp)
            .numLayersIs(1)
            .typeOfLayerNIs(0, ImageLayer.class);
        assertThat(layer)
            .hasNoUI()
            .hasNoMask();
        if (withMask.isTrue()) {
            ImageLayer raster = (ImageLayer) comp.getActiveLayer();
            assertThat(raster)
                .hasMask()
                .hasUI();
            assertThat(raster.getMask()).hasUI();
        }
    }

    @Test
    void getTextShape() {
        Shape textShape = layer.getTextShape();
        assertThat(textShape).isNotNull();
    }

    @Test
    void enlargeCanvas() {
        layer.enlargeCanvas(new Outsets(5, 5, 5, 10));

        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void createMovementEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);

        assertThat(edit).isNotNull();
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void commitSettings() {
        TextSettings oldSettings = layer.getSettings();
        String oldText = oldSettings.getText();
        String expectedOldName = TextLayer.nameFromText(oldText);

        assertThat(layer).nameIs(expectedOldName);
        String newText = Rnd.createRandomString(10);
        String expectedNewName = TextLayer.nameFromText(newText);

        TextSettings newSettings = oldSettings.copy();
        newSettings.setText(newText);
        layer.applySettings(newSettings);
        layer.commitSettings(oldSettings);

        assertThat(layer)
            .textIs(newText)
            .nameIs(expectedNewName);
        History.assertNumEditsIs(1);

        History.undo("Edit Text Layer");
        assertThat(layer)
            .textIs(oldText)
            .nameIs(expectedOldName);

        History.redo("Edit Text Layer");
        assertThat(layer)
            .textIs(newText)
            .nameIs(expectedNewName);

        iconChecker.verifyUpdateCounts(0, 0);
    }
}
