/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality common to all ContentLayer subclasses
 */
@RunWith(Parameterized.class)
public class ContentLayerTest {

    @Parameter
    public Class layerClass;

    @Parameter(value = 1)
    public WithMask withMask;

    private ContentLayer layer;
    private Composition comp;

    private IconUpdateChecker iconUpdates;

    @Parameters(name = "{index}: layer = {0}, mask = {1}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(new Object[][]{
                {ImageLayer.class, WithMask.NO},
                {ImageLayer.class, WithMask.YES},
                {TextLayer.class, WithMask.NO},
                {TextLayer.class, WithMask.YES},
        });
    }

    @Before
    public void setUp() {
        comp = TestHelper.createEmptyComposition();
        layer = (ContentLayer) TestHelper.createLayerOfClass(layerClass, comp);

        comp.addLayerNoGUI(layer);

        LayerGUI ui = mock(LayerGUI.class);
        layer.setUI(ui);

        withMask.init(layer);
        LayerMask mask = null;
        if (withMask.isYes()) {
            mask = layer.getMask();
        }

        iconUpdates = new IconUpdateChecker(ui, layer, mask, 0, 1);

        assert comp.getNrLayers() == 1 : "found " + comp.getNrLayers() + " layers";
        History.clear();
    }

    @Test
    public void testLayerMovingMethods() {
        assertThat(layer.getTX()).isEqualTo(0);
        assertThat(layer.getTY()).isEqualTo(0);

        layer.startMovement();

        assertThat(layer.getTX()).isEqualTo(0);
        assertThat(layer.getTY()).isEqualTo(0);

        layer.moveWhileDragging(2, 2);

        assertThat(layer.getTX()).isEqualTo(2);
        assertThat(layer.getTY()).isEqualTo(2);

        layer.moveWhileDragging(3, 3);

        assertThat(layer.getTX()).isEqualTo(3);
        assertThat(layer.getTY()).isEqualTo(3);

        // endMovement is called on the composition
        // so that we have history
        comp.endMovement();

        checkTranslationAfterPositiveDrag();
        iconUpdates.check(1, 1);

        // start another drag to the negative direction
        layer.startMovement();
        layer.moveWhileDragging(-1, -2);

        checkTranslationAfterNegativeDrag();

        comp.endMovement();

        // No change:
        // ImageLayer: this time the layer was not enlarged
        // TextLayer: endMovement does not change the tmpTranslation + translation sum
        checkTranslationAfterNegativeDrag();
        iconUpdates.check(2, 2);

        History.assertNumEditsIs(2);
        History.undo();
        checkTranslationAfterPositiveDrag();
        History.undo();
        assertThat(layer.getTX()).isEqualTo(0);
        assertThat(layer.getTY()).isEqualTo(0);
    }

    private void checkTranslationAfterPositiveDrag() {
        if (layer instanceof ImageLayer) {
            // the layer was enlarged in endMovement, and the translation reset to 0, 0
            assertThat(layer.getTX()).isEqualTo(0);
            assertThat(layer.getTY()).isEqualTo(0);
        } else if (layer instanceof TextLayer) {
            // text layers can have positive translations
            assertThat(layer.getTX()).isEqualTo(3);
            assertThat(layer.getTY()).isEqualTo(3);
        } else {
            throw new IllegalStateException("unexpected layer " + layer.getClass().getName());
        }
    }

    private void checkTranslationAfterNegativeDrag() {
        if (layer instanceof ImageLayer) {
            assertThat(layer.getTX()).isEqualTo(-1);
            assertThat(layer.getTY()).isEqualTo(-2);
        } else if (layer instanceof TextLayer) {
            assertThat(layer.getTX()).isEqualTo(2);
            assertThat(layer.getTY()).isEqualTo(1);
        }
    }

    @Test
    public void test_applyLayer() {
        Graphics2D g2 = TestHelper.createGraphics();
        BufferedImage image = TestHelper.createImage();

        layer.applyLayer(g2, true, image);
        layer.applyLayer(g2, false, image);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_paintLayerOnGraphics() {
        Graphics2D g2 = TestHelper.createGraphics();
        layer.paintLayerOnGraphics(g2, false);
        iconUpdates.check(0, 0);
    }

    @Test
    public void test_setupDrawingComposite() {
        Graphics2D g = TestHelper.createGraphics();
        layer.setupDrawingComposite(g, true);
        layer.setupDrawingComposite(g, false);
        iconUpdates.check(0, 0);
    }
}