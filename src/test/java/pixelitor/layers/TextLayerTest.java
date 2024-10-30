/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.compactions.Outsets;
import pixelitor.filters.painters.TextSettings;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;
import pixelitor.utils.Rnd;

import java.util.Arrays;
import java.util.Collection;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

@RunWith(Parameterized.class)
public class TextLayerTest {
    private TextLayer layer;
    private Composition comp;
    private IconUpdateChecker iconUpdates;

    @Parameter
    public WithMask withMask;

    @Parameters(name = "{index}: mask = {0}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
            {WithMask.NO},
            {WithMask.YES},
        });
    }

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        comp = TestHelper.createEmptyComp("TextLayerTest");
        layer = TestHelper.createTextLayer(comp, "Text Layer");
        layer.updateLayerName();
        comp.add(layer);

        withMask.setupForLayer(layer);
        LayerMask mask = null;
        if (withMask.isTrue()) {
            mask = layer.getMask();
        }

        iconUpdates = new IconUpdateChecker(layer, mask);

        assert layer.getComp().checkInvariants();
        History.clear();
    }

    @Test
    public void replaceWithRasterized() {
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
        iconUpdates.check(0, 0);
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

//    @Test
//    public void createSelectionFromText() {
//        assertThat(comp).doesNotHaveSelection();
//
//        layer.createSelectionFromText();
//
//        assertThat(comp).hasSelection();
//        iconUpdates.check(0, 0);
//    }

    @Test
    public void enlargeCanvas() {
        layer.enlargeCanvas(new Outsets(5, 5, 5, 10));

        iconUpdates.check(0, 0);
    }

    @Test
    public void createMovementEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);

        assertThat(edit).isNotNull();
        iconUpdates.check(0, 0);
    }

    @Test
    public void commitSettings() {
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

        iconUpdates.check(0, 0);
    }
}