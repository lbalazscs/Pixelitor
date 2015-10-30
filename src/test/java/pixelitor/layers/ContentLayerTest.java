/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

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
        Composition comp = TestHelper.createEmptyComposition();
        layer = (ContentLayer) TestHelper.createLayerOfClass(layerClass, comp);

        comp.addLayerNoGUI(layer);

        withMask.init(layer);

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

        layer.endMovement();

        checkTranslationAfterPositiveDrag();

        layer.startMovement();
        layer.moveWhileDragging(-1, -2);

        checkTranslationAfterNegativeDrag();

        layer.endMovement();

        // No change:
        // ImageLayer: this time the layer was not enlarged
        // TextLayer: endMovement does not change the tmpTranslation + translation sum
        checkTranslationAfterNegativeDrag();

        // TODO should have undo
//        History.assertNumEditsIs(2);
//        History.undo();
//        checkTranslationAfterPositiveDrag();
//        History.undo();
//        assertThat(layer.getTX()).isEqualTo(0);
//        assertThat(layer.getTY()).isEqualTo(0);
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
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testCreateTranslateEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);
        assertThat(edit).isNotNull();
    }

    @Test
    public void testEnlargeCanvas() {
        layer.enlargeCanvas(5, 5, 5, 10);
    }

    @Test
    public void testApplyLayer() {
        Graphics2D g2 = TestHelper.createGraphics();
        BufferedImage image = TestHelper.createImage();

        layer.applyLayer(g2, true, image);
        layer.applyLayer(g2, false, image);
    }

    @Test
    public void testPaintLayerOnGraphics() {
        Graphics2D g2 = TestHelper.createGraphics();
        layer.paintLayerOnGraphics(g2, false);
    }

    @Test
    public void testSetupDrawingComposite() {
        Graphics2D g = TestHelper.createGraphics();
        layer.setupDrawingComposite(g, true);
        layer.setupDrawingComposite(g, false);
    }
}