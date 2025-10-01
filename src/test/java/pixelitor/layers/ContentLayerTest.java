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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.history.History;
import pixelitor.testutils.WithMask;
import pixelitor.tools.move.MoveMode;

import java.awt.Graphics2D;
import java.util.stream.Stream;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Tests the functionality common to all {@link ContentLayer} subclasses.
 */
@ParameterizedClass(name = "class = {0}, with mask = {1}")
@MethodSource("instancesToTest")
@DisplayName("content layer tests")
@TestMethodOrder(MethodOrderer.Random.class)
class ContentLayerTest {
    @Parameter(0)
    private Class<? extends Layer> layerClass;

    @Parameter(1)
    private WithMask withMask;

    private ContentLayer layer;
    private Composition comp;

    private IconUpdateChecker iconChecker;

    static Stream<Arguments> instancesToTest() {
        return Stream.of(
            Arguments.of(ImageLayer.class, WithMask.NO),
            Arguments.of(ImageLayer.class, WithMask.YES),
            Arguments.of(TextLayer.class, WithMask.NO),
            Arguments.of(TextLayer.class, WithMask.YES)
        );
    }

    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @BeforeEach
    void beforeEachTest() {
        comp = TestHelper.createEmptyComp("ContentLayerTest");
        layer = (ContentLayer) TestHelper.createLayer(layerClass, comp);
        comp.addLayerWithoutUI(layer);

        withMask.configure(layer);
        iconChecker = new IconUpdateChecker(layer);

        assertThat(comp)
            .numLayersIs(1)
            .invariantsAreOK();
        History.clear();
    }

    @Test
    void movingTheLayer() {
        assertThat(layer).hasNoTranslation();

        layer.prepareMovement();
        assertThat(layer).hasNoTranslation();

        layer.moveWhileDragging(2, 2);
        assertThat(layer).translationIs(2, 2);

        layer.moveWhileDragging(3, 3);
        assertThat(layer).translationIs(3, 3);

        // finalizeMovement is called on the composition
        // so that we have history
        comp.finalizeMovement(MoveMode.MOVE_LAYER_ONLY);

        checkTranslationAfterPositiveDrag();
        int expectedLayerIconUpdates = layerClass == ImageLayer.class ? 1 : 0;
        iconChecker.verifyUpdateCounts(expectedLayerIconUpdates, 1);

        // start another drag to the negative direction
        layer.prepareMovement();
        layer.moveWhileDragging(-1, -2);

        checkTranslationAfterNegativeDrag();

        comp.finalizeMovement(MoveMode.MOVE_LAYER_ONLY);

        // No change:
        // ImageLayer: this time the layer was not enlarged
        // TextLayer: finalizeMovement does not change the tmpTranslation + translation sum
        checkTranslationAfterNegativeDrag();
        expectedLayerIconUpdates = layerClass == ImageLayer.class ? 2 : 0;
        iconChecker.verifyUpdateCounts(expectedLayerIconUpdates, 2);
        History.assertNumEditsIs(2);

        History.undo("Move Layer");
        checkTranslationAfterPositiveDrag();

        History.undo("Move Layer");
        assertThat(layer).hasNoTranslation();

        History.redo("Move Layer");
        History.redo("Move Layer");
    }

    private void checkTranslationAfterPositiveDrag() {
        if (layer instanceof ImageLayer) {
            // the layer was enlarged in finalizeMovement, and the translation was reset to 0, 0
            assertThat(layer).hasNoTranslation();
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
    void render() {
        var g2 = TestHelper.createGraphics();
        var image = TestHelper.createImage();

        layer.render(g2, image, true);
        layer.render(g2, image, false);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void paintLayerOnGraphics() {
        Graphics2D g2 = TestHelper.createGraphics();
        layer.paint(g2, false);
        iconChecker.verifyUpdateCounts(0, 0);
    }

    @Test
    void setupComposite() {
        Graphics2D g = TestHelper.createGraphics();
        layer.setupComposite(g, true);
        layer.setupComposite(g, false);
        iconChecker.verifyUpdateCounts(0, 0);
    }
}
