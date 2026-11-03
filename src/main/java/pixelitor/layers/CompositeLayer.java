/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.View;

import java.awt.EventQueue;
import java.io.Serial;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A {@link ContentLayer} that is also a {@link LayerHolder},
 * and therefore can contain nested layers.
 */
public abstract class CompositeLayer extends ContentLayer implements LayerHolder {
    @Serial
    private static final long serialVersionUID = 8962987501686783478L;

    CompositeLayer(Composition comp, String name) {
        super(comp, name);
    }

    // initialization that requires a deserialized canvas
    public abstract void afterDeserialization();

    public void updateChildrenUI() {
        if (!hasUI()) { // in some unit tests
            return;
        }
        getUI().updateChildrenPanel();
        EventQueue.invokeLater(this::revalidateUI);
    }

    @Override
    public void reorderLayerUI(int oldIndex, int newIndex) {
        // do a full update
        updateChildrenUI();
    }

    private void revalidateUI() {
        View view = comp.getView();
        if (view == null) {
            // the method runs via invokeLater, so the
            // composition might have been closed in the meantime
            return;
        }
        LayersPanel layersPanel = view.getLayersPanel();
        if (layersPanel != null) { // null in unit tests
            layersPanel.revalidate();
        }
    }

    // Layer's concrete implementation would win over the LayerHolder
    // default, so the two must be combined explicitly: children first,
    // then this layer and its mask.
    @Override
    public final void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        LayerHolder.super.forEachNestedLayer(action, includeMasks); // children
        super.forEachNestedLayer(action, includeMasks);             // self, then mask
    }

    @Override
    public final Layer findFirstLayerWhere(Predicate<Layer> predicate, boolean includeMasks) {
        Layer found = LayerHolder.super.findFirstLayerWhere(predicate, includeMasks);
        if (found != null) {
            return found;
        }
        return super.findFirstLayerWhere(predicate, includeMasks);
    }

    @Override
    public final boolean contains(Layer layer) {
        return super.contains(layer) || LayerHolder.super.contains(layer);
    }

    @Override
    public void setComp(Composition comp) {
        super.setComp(comp);

        for (Layer child : directChildren()) {
            child.setComp(comp);
        }
    }

    @Override
    public void updateUIAfterLayerInsertion(Layer newLayer, int index) {
        updateChildrenUI();
    }

    @Override
    public void updateUIAfterLayerDeletion(Layer deletedLayer) {
        updateChildrenUI(); // not used by SmartObject, which has its own deleteLayer
    }
}
