/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import org.assertj.core.util.Objects;
import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Custom AssertJ assertions for {@link Composition} objects.
 */
public class CompositionAssert extends AbstractAssert<CompositionAssert, Composition> {
    public CompositionAssert(Composition actual) {
        super(actual, CompositionAssert.class);
        actual.checkInvariant();
    }

    public CompositionAssert isEmpty() {
        isNotNull();

        if (!actual.isEmpty()) {
            failWithMessage("\nExpecting that actual Composition is empty but is not.");
        }

        return this;
    }

    public CompositionAssert isNotEmpty() {
        isNotNull();

        if (actual.isEmpty()) {
            failWithMessage("\nExpecting that actual Composition is not empty but is.");
        }

        return this;
    }

    public CompositionAssert isDirty() {
        isNotNull();

        // check that property call/field access is true
        if (!actual.isDirty()) {
            failWithMessage("\nExpecting that actual Composition is dirty but is not.");
        }

        return this;
    }

    public CompositionAssert isNotDirty() {
        isNotNull();

        // check that property call/field access is false
        if (actual.isDirty()) {
            failWithMessage("\nExpecting that actual Composition is not dirty but is.");
        }

        return this;
    }

    public CompositionAssert numLayersIs(int numLayers) {
        isNotNull();

        String msg = "\nExpecting number of layers of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        int actualNumLayers = actual.getNumLayers();
        if (actualNumLayers != numLayers) {
            failWithMessage(msg, actual, numLayers, actualNumLayers);
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


    public CompositionAssert activeLayerIs(Layer expected) {
        isNotNull();

        Layer active = actual.getActiveLayer();
        if (active != expected) {
            throw new AssertionError("expected " + expected.getName()
                    + ", found " + active.getName());
        }

        return this;
    }

    public CompositionAssert activeLayerNameIs(String expected) {
        isNotNull();

        assertThat(actual.getActiveLayer()
                .getName())
                .isEqualTo(expected);

        return this;
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

        String msg = "\nExpecting expected of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        int actualIndex = actual.getActiveLayerIndex();
        if (actualIndex != expected) {
            failWithMessage(msg, actual, expected, actualIndex);
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

    public CompositionAssert hasCanvasImWidth(int canvasWidth) {
        isNotNull();

        String msg = "\nExpecting canvasWidth of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        int actualCanvasWidth = actual.getCanvasImWidth();
        if (actualCanvasWidth != canvasWidth) {
            failWithMessage(msg, actual, canvasWidth, actualCanvasWidth);
        }

        return this;
    }

    public CompositionAssert hasCanvasImHeight(int canvasHeight) {
        isNotNull();

        String msg = "\nExpecting canvasHeight of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        int actualCanvasHeight = actual.getCanvasImHeight();
        if (actualCanvasHeight != canvasHeight) {
            failWithMessage(msg, actual, canvasHeight, actualCanvasHeight);
        }

        return this;
    }

    public CompositionAssert canvasSizeIs(int w, int h) {
        isNotNull();

        hasCanvasImWidth(w);
        hasCanvasImHeight(h);

        return this;
    }

    public CompositionAssert hasName(String name) {
        isNotNull();

        String msg = "\nExpecting name of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        String actualName = actual.getName();
        if (!Objects.areEqual(actualName, name)) {
            failWithMessage(msg, actual, name, actualName);
        }

        return this;
    }

    public CompositionAssert hasSelection() {
        isNotNull();

        if (!actual.hasSelection()) {
            failWithMessage("\nExpecting that actual Composition has selection but does not have.");
        }

        return this;
    }

    public CompositionAssert doesNotHaveSelection() {
        isNotNull();

        if (actual.hasSelection()) {
            failWithMessage("\nExpecting that actual Composition does not have selection but has.");
        }

        return this;
    }

    public CompositionAssert activeLayerTranslationIs(int tx, int ty) {
        isNotNull();

        ContentLayer layer = (ContentLayer) actual.getActiveLayer();
        assertEquals("tx", tx, layer.getTX());
        assertEquals("ty", ty, layer.getTY());

        return this;
    }

    public CompositionAssert activeLayerAndMaskImageSizeIs(int w, int h) {
        isNotNull();

        ImageLayer layer = (ImageLayer) actual.getActiveLayer();
        BufferedImage image = layer.getImage();
        assertEquals("width", w, image.getWidth());
        assertEquals("height", h, image.getHeight());

        if (layer.hasMask()) {
            BufferedImage maskImage = layer.getMask()
                    .getImage();
            assertEquals("mask width", w, maskImage.getWidth());
            assertEquals("mask height", h, maskImage.getHeight());
        }

        return this;
    }

    public CompositionAssert selectionBoundsIs(Rectangle rect) {
        isNotNull();

        Rectangle bounds = actual.getSelection()
                .getShapeBounds();
        assertEquals("selection bounds", rect, bounds);

        return this;
    }

    public CompositionAssert invariantIsOK() {
        isNotNull();

        actual.checkInvariant();

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
}
