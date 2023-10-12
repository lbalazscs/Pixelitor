/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.Layer;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that is created when a new layer is selected
 */
public class LayerSelectionChangeEdit extends PixelitorEdit {
    private final Layer oldLayer;
    private final Layer newLayer;

    public LayerSelectionChangeEdit(String editName, Composition comp, Layer oldLayer, Layer newLayer) {
        super(editName == null ? "Layer Selection Change" : editName, comp);

        this.oldLayer = oldLayer;
        this.newLayer = newLayer;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.setActiveLayer(oldLayer);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.setActiveLayer(newLayer);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(oldLayer.createDebugNode("old layer"));
        node.add(newLayer.createDebugNode("new layer"));

        return node;
    }
}
