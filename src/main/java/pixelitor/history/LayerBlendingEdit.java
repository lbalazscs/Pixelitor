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

import pixelitor.layers.BlendingMode;
import pixelitor.layers.Layer;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the changes made to the blending mode of a layer
 */
public class LayerBlendingEdit extends PixelitorEdit {
    private final Layer layer;
    private final BlendingMode oldMode;
    private final BlendingMode newMode;

    public LayerBlendingEdit(Layer layer, BlendingMode oldMode) {
        super("Blending Mode Change", layer.getComp());

        this.layer = layer;
        this.oldMode = oldMode;
        this.newMode = layer.getBlendingMode();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setBlendingMode(oldMode, false, true);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.setBlendingMode(newMode, false, true);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.addAsString("oldMode", oldMode);
        node.addAsString("newMode", newMode);

        return node;
    }
}
