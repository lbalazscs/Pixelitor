/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.Composition.LayerAdder;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the creation of a new layer
 */
public class NewLayerEdit extends PixelitorEdit {
    private Layer activeLayerBefore;
    private Layer newLayer;
    private final int newLayerIndex;
    private final MaskViewMode viewModeBefore;

    public NewLayerEdit(String historyName, Composition comp,
                        Layer newLayer, Layer activeLayerBefore,
                        MaskViewMode viewModeBefore) {
        super(historyName, comp);

        this.activeLayerBefore = activeLayerBefore;
        this.viewModeBefore = viewModeBefore;
        this.newLayer = newLayer;
        newLayerIndex = comp.getLayerIndex(newLayer);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.deleteLayer(newLayer, false);
        comp.setActiveLayer(activeLayerBefore);
        viewModeBefore.activate(comp, activeLayerBefore);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        new LayerAdder(comp)
            .atIndex(newLayerIndex)
            .add(newLayer);
    }

    @Override
    public void die() {
        super.die();

        newLayer = null;
        activeLayerBefore = null;
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = super.createDebugNode();

        node.add(activeLayerBefore.createDebugNode("active layer before"));
        node.add(newLayer.createDebugNode("new layer"));
        node.addInt("new layer index", newLayerIndex);
        node.addString("view mode before", viewModeBefore.toString());

        return node;
    }
}
