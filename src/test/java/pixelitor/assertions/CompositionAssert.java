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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerUI;
import pixelitor.tools.pen.Path;

import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Custom AssertJ assertions for {@link Composition} objects.
 */
public class CompositionAssert extends AbstractAssert<CompositionAssert, Composition> {
    public CompositionAssert(Composition actual) {
        super(actual, CompositionAssert.class);
        assert actual.checkInvariants();
    }

    public CompositionAssert isDirty() {
        isNotNull();

        if (!actual.isDirty()) {
            failWithMessage("\nExpecting that actual Composition is dirty but is not.");
        }

        return this;
    }

    public CompositionAssert isNotDirty() {
        isNotNull();

        if (actual.isDirty()) {
            failWithMessage("\nExpecting that actual Composition is not dirty but is.");
        }

        return this;
    }

    public CompositionAssert isOpen() {
        isNotNull();

        if (!actual.isOpen()) {
            failWithMessage("\nExpecting that actual Composition is open but is not.");
        }

        return this;
    }

    public CompositionAssert isNotOpen() {
        isNotNull();

        if (actual.isOpen()) {
            failWithMessage("\nExpecting that actual Composition is not open but is.");
        }

        return this;
    }

    public CompositionAssert numLayersIs(int numLayers) {
        isNotNull();

        int actualNumLayers = actual.getNumLayers();
        if (actualNumLayers != numLayers) {
            failWithMessage("""
                    
                Expecting number of layers of:
                  <%s>
                to be:
                  <%s>
                but was:
                  <%s>""", actual, numLayers, actualNumLayers);
        }

        return this;
    }

    public CompositionAssert layerNamesAre(String... expected) {
        isNotNull();

        int expectedLayerCount = expected.length;
        if (expectedLayerCount != actual.getNumLayers()) {
            failWithMessage(String.format(
                "\nFound %d layers instead of the expected %d.",
                actual.getNumLayers(), expectedLayerCount));
        }
        for (int i = 0; i < expectedLayerCount; i++) {
            String layerName = actual.getLayer(i).getName();
            if (!layerName.equals(expected[i])) {
                failWithMessage(String.format(
                    "\nIn layer nr. %d the layer name was '%s', while expecting '%s'.",
                    i, layerName, expected[i]));
            }
        }

        return this;
    }

    /**
     * Verifies that the composition contains exactly the given direct child layers, in the specified order.
     */
    public CompositionAssert layersAre(Layer... expectedLayers) {
        isNotNull();
        List<Layer> actualLayers = actual.directChildrenStream().collect(Collectors.toList());
        assertThat(actualLayers)
            .withFailMessage("Expected composition '%s' to contain exactly layers %s in order, but found %s.",
                actual.getName(), Arrays.toString(expectedLayers), actualLayers)
            .containsExactly(expectedLayers);
        return myself;
    }

    public CompositionAssert layerNHasMask(int n) {
        isNotNull();

        assertThat(actual.getLayer(n)
            .hasMask())
            .isTrue();

        return this;
    }

    public CompositionAssert firstLayerHasMask() {
        return layerNHasMask(0);
    }

    public CompositionAssert secondLayerHasMask() {
        return layerNHasMask(1);
    }

    public CompositionAssert activeLayerIndexIs(int expected) {
        isNotNull();

        int actualIndex = actual.indexOf(actual.getActiveTopLevelLayer());
        if (actualIndex != expected) {
            failWithMessage("""
                    
                Expecting active layer index of:
                  <%s>
                to be:
                  <%s>
                but was:
                  <%s>""", actual, expected, actualIndex);
        }

        return this;
    }

    public CompositionAssert firstLayerIsActive() {
        return activeLayerIndexIs(0);
    }

    public CompositionAssert secondLayerIsActive() {
        return activeLayerIndexIs(1);
    }

    public CompositionAssert thirdLayerIsActive() {
        return activeLayerIndexIs(2);
    }

    public CompositionAssert canvasSizeIs(int w, int h) {
        isNotNull();

        int actualCanvasWidth = actual.getCanvasWidth();
        int actualCanvasHeight = actual.getCanvasHeight();
        if (actualCanvasWidth != w || actualCanvasHeight != h) {
            String msg = """

                Expecting canvas size of:
                  <%s>
                to be:
                  <%dx%d>
                but was:
                  <%dx%d>""";
            failWithMessage(msg, actual, w, h, actualCanvasWidth, actualCanvasHeight);
        }

        return this;
    }

    public CompositionAssert hasName(String name) {
        isNotNull();

        String actualName = actual.getName();
        if (!Objects.equals(actualName, name)) {
            failWithMessage("""
                    
                Expecting name of:
                  <%s>
                to be:
                  <%s>
                but was:
                  <%s>""", actual, name, actualName);
        }

        return this;
    }

    public CompositionAssert hasSelection() {
        isNotNull();

        if (!actual.hasSelection()) {
            failWithMessage("No selection");
        }

        return this;
    }

    public CompositionAssert doesNotHaveSelection() {
        isNotNull();

        if (actual.hasSelection()) {
            failWithMessage("Has selection");
        }

        return this;
    }

    public CompositionAssert activeLayerTranslationIs(Point p) {
        return activeLayerTranslationIs(p.x, p.y);
    }

    public CompositionAssert activeLayerTranslationIs(int tx, int ty) {
        isNotNull();

        ContentLayer layer = (ContentLayer) actual.getActiveLayer();
        assertEquals("tx", tx, layer.getTx());
        assertEquals("ty", ty, layer.getTy());

        return this;
    }

    public CompositionAssert activeLayerAndMaskImageSizeIs(int w, int h) {
        isNotNull();

        var layer = (ImageLayer) actual.getActiveLayer();
        var image = layer.getImage();
        assertEquals("width", w, image.getWidth());
        assertEquals("height", h, image.getHeight());

        if (layer.hasMask()) {
            var maskImage = layer.getMask().getImage();
            assertEquals("mask width", w, maskImage.getWidth());
            assertEquals("mask height", h, maskImage.getHeight());
        }

        return this;
    }

    public CompositionAssert selectionBoundsIs(Rectangle2D rect) {
        isNotNull();

        var selection = actual.getSelection();
        var selShapeBounds = selection.getShapeBounds2D();
        assertEquals("selection bounds", rect, selShapeBounds);

        return this;
    }

    public CompositionAssert invariantsAreOK() {
        isNotNull();

        assert actual.checkInvariants();

        return this;
    }

    public CompositionAssert typeOfLayerNIs(int index, Class<? extends Layer> type) {
        isNotNull();

        Layer layer = actual.getLayer(index);
        assertThat(layer).isInstanceOf(type);

        return this;
    }

    public CompositionAssert selectionShapeIs(Shape shape) {
        isNotNull();

        assertThat(actual.getSelectionShape()).isEqualTo(shape);

        return this;
    }

    public CompositionAssert allLayerUIsAreOK() {
        isNotNull();

        for (int i = 0; i < actual.getNumLayers(); i++) {
            checkLayerUI(i);
        }

        return this;
    }

    private void checkLayerUI(int i) {
        Layer layer = actual.getLayer(i);
        String layerClassName = layer.getClass().getSimpleName();
        LayerUI layerUI = layer.getUI();
        if (layerUI == null) {
            failWithMessage("%s #%d ('%s') has no UI",
                layerClassName, i, layer.getName());
        }
        if (layer.hasMask()) {
            LayerUI maskUI = layer.getMask().getUI();
            if (maskUI == null) {
                failWithMessage("The mask of %s #%d ('%s') has no UI",
                    layerClassName, i, layer.getName());
            }
            if (maskUI != layerUI) {
                failWithMessage("The mask of the %s #%d ('%s') has a different UI than the layer",
                    layerClassName, i, layer.getName());
            }
            if (!maskUI.hasMaskIcon()) {
                failWithMessage("The mask UI of the %s #%d ('%s') has no mask icon",
                    layerClassName, i, layer.getName());
            }
        } else { // the layer has no mask
            if (layerUI.hasMaskIcon()) {
                failWithMessage("The UI of the %s #%d ('%s') has an unexpected mask icon",
                    layerClassName, i, layer.getName());
            }
        }
    }

    public CompositionAssert hasGuides() {
        isNotNull();

        assertThat(actual.getGuides()).isNotNull();

        return this;
    }

    public CompositionAssert hasPath() {
        isNotNull();

        assertThat(actual.getActivePath()).isNotNull();

        return this;
    }

    public CompositionAssert hasNoPath() {
        isNotNull();

        assertThat(actual.getActivePath()).isNull();

        return this;
    }

    public CompositionAssert activePathIs(Path expected) {
        isNotNull();

        assertThat(actual.getActivePath()).isSameAs(expected);

        return this;
    }

    public CompositionAssert activeLayerIs(Layer expected) {
        isNotNull();

        assertThat(actual.getActiveLayer()).isSameAs(expected);

        return this;
    }

    public CompositionAssert activeLayerNameIs(String expected) {
        isNotNull();

        assertThat(actual.getActiveLayer().getName()).isEqualTo(expected);

        return this;
    }

    public CompositionAssert hasDpi(int expectedDpi) {
        isNotNull();
        int actualDpi = actual.getDpi();
        if (actualDpi != expectedDpi) {
            failWithMessage("Expected DPI <%d> but was <%d>",
                expectedDpi, actualDpi);
        }
        return this;
    }

    public CompositionAssert hasSolidColor(Color expectedColor) {
        isNotNull();
        BufferedImage image = actual.getCompositeImage();
        int actualRgb = image.getRGB(0, 0);
        if (actualRgb != expectedColor.getRGB()) {
            failWithMessage("Expected image to be filled with color <%s> but was <%s>",
                expectedColor, new Color(actualRgb, true));
        }
        return this;
    }
}
