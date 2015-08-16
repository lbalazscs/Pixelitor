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
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.ContentLayerMoveEdit;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentLayerTest {
    private Composition comp;
    private ContentLayer layer;

    @Before
    public void setUp() {
        comp = TestHelper.createEmptyTestComposition();
        layer = TestHelper.createTestImageLayer("imageLayer 1", comp);
    }

    @Test
    public void testLayerMovingMethods() {
        assertThat(layer.getTranslationX()).isEqualTo(0);
        assertThat(layer.getTranslationY()).isEqualTo(0);

        layer.startMovement();

        assertThat(layer.getTranslationX()).isEqualTo(0);
        assertThat(layer.getTranslationY()).isEqualTo(0);

        layer.moveWhileDragging(2, 2);

        assertThat(layer.getTranslationX()).isEqualTo(2);
        assertThat(layer.getTranslationY()).isEqualTo(2);

        layer.moveWhileDragging(3, 3);

        assertThat(layer.getTranslationX()).isEqualTo(3);
        assertThat(layer.getTranslationY()).isEqualTo(3);

        layer.endMovement();

        // the layer was enlarged in endMovement, and the translation reset to 0, 0
        assertThat(layer.getTranslationX()).isEqualTo(0);
        assertThat(layer.getTranslationY()).isEqualTo(0);

        layer.startMovement();
        layer.moveWhileDragging(-1, -2);

        assertThat(layer.getTranslationX()).isEqualTo(-1);
        assertThat(layer.getTranslationY()).isEqualTo(-2);

        layer.endMovement();

        // this time the layer was not enlarged
        assertThat(layer.getTranslationX()).isEqualTo(-1);
        assertThat(layer.getTranslationY()).isEqualTo(-2);
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testCreateTranslateEdit() {
        ContentLayerMoveEdit edit = layer.createMovementEdit(5, 5);
        assertThat(edit).isNotNull();
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
    public void testEnlargeCanvas() {
        layer.enlargeCanvas(5, 5, 5, 10);
    }

    @Test
    public void testPaintLayer() {
        Graphics2D g2 = TestHelper.createGraphics();
        BufferedImage image = TestHelper.createTestImage();

        BufferedImage resultImage = layer.applyLayer(g2, true, image);
        assertThat(resultImage).isNull(); // content layers return null

        resultImage = layer.applyLayer(g2, false, image);
        assertThat(resultImage).isNull(); // content layers return null
    }

    @Test
    // this method is abstract in ImageLayer, test separately for subclasses
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