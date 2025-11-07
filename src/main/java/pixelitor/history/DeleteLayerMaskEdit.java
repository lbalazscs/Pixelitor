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

package pixelitor.history;

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the deletion of a layer mask
 */
public class DeleteLayerMaskEdit extends PixelitorEdit {
    private final LayerMask prevMask;
    private final Layer layer;
    private final MaskViewMode prevMode;

    public DeleteLayerMaskEdit(Composition comp, Layer layer, LayerMask prevMask, MaskViewMode prevMode) {
        super("Delete Layer Mask", comp);
        this.prevMode = prevMode;

        assert layer.isActive() || AppMode.isUnitTesting();
        this.layer = layer;
        this.prevMask = prevMask;
    }

    @Override
    public void undo() throws CannotUndoException {
        assert layer.isActive() || AppMode.isUnitTesting();

        super.undo();

        layer.addConfiguredMask(prevMask);
        comp.setMaskViewMode(prevMode, layer);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.deleteMask(false);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.add(prevMask.createDebugNode("previous mask"));
        node.addAsString("previous mode", prevMode);

        return node;
    }
}
