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

import pixelitor.layers.Layer;
import pixelitor.layers.LayerHolder;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * An edit for moving a layer from one holder to another one.
 *
 * Used when moving a layer across a group boundary
 * during the rearranging of the stack order.
 */
public class ChangeHolderEdit extends PixelitorEdit {
    private final Layer layer;

    private final LayerHolder oldHolder;
    private final int oldIndex;

    private final LayerHolder newHolder;
    private final int newIndex;

    public ChangeHolderEdit(String editName, Layer layer, LayerHolder oldHolder, int oldIndex, LayerHolder newHolder, int newIndex) {
        super(editName, layer.getComp());
        this.layer = layer;
        this.oldHolder = oldHolder;
        this.oldIndex = oldIndex;
        this.newHolder = newHolder;
        this.newIndex = newIndex;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        newHolder.moveLayerInto(layer, oldHolder, oldIndex, null);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        oldHolder.moveLayerInto(layer, newHolder, newIndex, null);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.add(oldHolder.createDebugNode("old holder"));
        node.add(newHolder.createDebugNode("new holder"));
        node.addInt("oldIndex", oldIndex);
        node.addInt("newIndex", newIndex);

        return node;
    }
}
