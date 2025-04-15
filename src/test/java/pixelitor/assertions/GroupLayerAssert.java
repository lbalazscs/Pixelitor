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

import pixelitor.layers.Layer;
import pixelitor.layers.LayerGroup;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertions for {@link LayerGroup} objects.
 */
public class GroupLayerAssert extends ContentLayerAssert<GroupLayerAssert, LayerGroup> {

    public GroupLayerAssert(LayerGroup actual) {
        super(actual, GroupLayerAssert.class);
    }

    public GroupLayerAssert isPassThrough() {
        isNotNull();
        assertThat(actual.isPassThrough())
            .withFailMessage("Expected layer group '%s' to be in pass-through mode, but it was not.", actual.getName())
            .isTrue();
        return myself;
    }

    public GroupLayerAssert isNotPassThrough() {
        isNotNull();
        assertThat(actual.isPassThrough())
            .withFailMessage("Expected layer group '%s' not to be in pass-through mode, but it was.", actual.getName())
            .isFalse();
        return myself;
    }

    public GroupLayerAssert hasNumLayers(int expected) {
        isNotNull();
        assertThat(actual.getNumLayers())
            .withFailMessage("Expected layer group '%s' to have %d layers, but found %d.",
                actual.getName(), expected, actual.getNumLayers())
            .isEqualTo(expected);
        return myself;
    }

    public GroupLayerAssert isEmpty() {
        isNotNull();
        hasNumLayers(0);
        return myself;
    }

    public GroupLayerAssert isNotEmpty() {
        isNotNull();
        assertThat(actual.getNumLayers())
            .withFailMessage("Expected layer group '%s' to be non-empty, but it was empty.", actual.getName())
            .isPositive();
        return myself;
    }

    public GroupLayerAssert listContainsLayer(Layer layer) {
        isNotNull();
        assertThat(actual.listContainsLayer(layer))
            .withFailMessage("Expected layer group '%s' to contain layer '%s', but it did not.",
                actual.getName(), layer.getName())
            .isTrue();
        // check specifically if it's a direct child
        assertThat(actual.indexOf(layer))
            .withFailMessage("Expected layer '%s' to be a direct child of group '%s', but it was not found (indexOf returned -1).",
                layer.getName(), actual.getName())
            .isNotEqualTo(-1);
        return myself;
    }

    /**
     * Verifies that the layer group contains exactly the given direct child layers, in the specified order.
     */
    public GroupLayerAssert layersAre(Layer... expectedLayers) {
        isNotNull();
        List<Layer> actualLayers = actual.levelStream().collect(Collectors.toList());
        assertThat(actualLayers)
            .withFailMessage("Expected layer group '%s' to contain exactly layers %s in order, but found %s.",
                actual.getName(), Arrays.toString(expectedLayers), actualLayers)
            .containsExactly(expectedLayers);
        return myself;
    }

    /**
     * Allows chained assertions on a specific child layer by index.
     */
    public LayerAssert<?, Layer> layer(int index) {
        isNotNull();
        Layer layer = actual.getLayer(index);
        // Return a basic LayerAssert; specific types would require more complex switching logic
        return new LayerAssert<>(layer, LayerAssert.class);
    }
}
