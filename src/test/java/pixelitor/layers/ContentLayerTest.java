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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;
import pixelitor.tools.move.MoveMode;

import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Collection;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Tests the functionality common to all ContentLayer subclasses
 */
@RunWith(Parameterized.class)
public class ContentLayerTest {
    @Parameter
    public Class<? extends Layer> layerClass;

    @Parameter(value = 1)
    public WithMask withMask;

    private ContentLayer layer;
    private Composition comp;

    private IconUpdateChecker iconChecker;

    @Parameters(name = "{index}: layer = {0}, mask = {1}")
    public static Collection<Object[]> instancesToTest() {
        // this method runs before beforeAllTests
        TestHelper.setUnitTestingMode();

        return Arrays.asList(new Object[][]{
            {ImageLayer.class, WithMask.NO},
            {ImageLayer.class, WithMask.YES},
            {TextLayer.class, WithMask.NO},
            {TextLayer.class, WithMask.YES},
        });
    }

    @BeforeClass
    public static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Before
    public void beforeEachTest() {
        comp = TestHelper.createEmptyComp("ContentLayerTest");
        layer = (ContentLayer) TestHelper.createLayer(layerClass, comp);

        comp.addLayerWithoutUI(layer);

        withMask.configure(layer);
        LayerMask mask = null;
        if (withMask.isTrue()) {
            mask = layer.getMask();
        }

        iconChecker = new IconUpdateChecker(layer, mask);

        assert comp.getNumLayers() == 1 : "found " + comp.getNumLayers() + " layers";
        History.clear();
    }

    @Test
    public void movingTheLayer() {
        assertThat(layer).translationIs(0, 0);

        layer.startMovement();
        assertThat(layer).translationIs(0, 0);

        layer.moveWhileDragging(2, 2);
        assertThat(layer).translationIs(2, 2);

        layer.moveWhileDragging(3, 3);
        assertThat(layer).translationIs(3, 3);

        // endMovement is called on the composition
        // so that we have history
        comp.endMovement(MoveMode.MOVE_LAYER_ONLY);

        checkTranslationAfterPositiveDrag();
        int expectedLayerIconUpdates = layerClass == ImageLayer.class ? 1 : 0;
        iconChecker.verifyUpdateCounts(expectedLayerIconUpdates, 1);

        // start another drag to the negative direction
        layer.startMovement();
        layer.moveWhileDragging(-1, -2);

        checkTranslationAfterNegativeDrag();

        comp.endMovement(MoveMode.MOVE_LAYER_ONLY);

        // No change:
        // ImageLayer: this time the layer was not enlarged
        // TextLayer: endMovement does not change the tmpTranslation + translation sum
        checkTranslationAfterNegativeDrag();
        expectedLayerIconUpdates = layerClass == ImageLayer.class ? 2 : 0;
        iconChecker.verifyUpdateCounts(expectedLayerIconUpdates, 2);
        History.assertNumEditsIs(2);

        History.undo("Move Layer");
        checkTranslationAfterPositiveDrag();

        History.undo("Move Layer");
        assertThat(layer).translationIs(0, 0);

        History.redo("Move Layer");
        History.redo("Move Layer");
    }

    private void checkTranslationAfterPositiveDrag() {
        if (layer instanceof ImageLayer) {
            // the layer was enlarged in endMovement, and the translation reset to 0, 0
            assertThat(layer).translationIs(0, 0);
        } else if (layer instanceof TextLayer) {
            // text layers can have positive translations
            assertThat(layer).translationIs(3, 3);
        } else {
            throw new IllegalStateException("unexpected layer " + layer.getClass().getName());
        }
    }

    private void checkTranslationAfterNegativeDrag() {
        if (layer instanceof ImageLayer) {
            assertThat(layer).translationIs(-1, -2);
        } else if (layer instanceof TextLayer) {
            assertThat(layer).translationIs(2, 1);
        }
    }

    @Test
    public void render() {
        var g2 = TestHelper.createGraphics();
        var image = TestHelper.createImage();

        layer.render(g2, image, true);
        layer.render(g2, image, false);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void paintLayerOnGraphics() {
        Graphics2D g2 = TestHelper.createGraphics();
        layer.paint(g2, false);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    public void setupComposite() {
        Graphics2D g = TestHelper.createGraphics();
        layer.setupComposite(g, true);
        layer.setupComposite(g, false);
        iconChecker.verifyUpdateCounts(0, 0);
    }
}