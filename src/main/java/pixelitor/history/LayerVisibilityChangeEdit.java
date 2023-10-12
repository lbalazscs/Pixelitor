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
 * A PixelitorEdit that represents the hiding or showing of a layer
 */
public class LayerVisibilityChangeEdit extends PixelitorEdit {
    private final Layer layer;
    private final boolean newVisibility;

    public LayerVisibilityChangeEdit(Composition comp, Layer layer, boolean newVisibility) {
        super(newVisibility ? "Show " + layer.getTypeString() : "Hide " + layer.getTypeString(), comp);

        this.newVisibility = newVisibility;
        this.layer = layer;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setVisible(!newVisibility, false, true);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.setVisible(newVisibility, false, true);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.addBoolean("new visibility", newVisibility);

        return node;
    }
}