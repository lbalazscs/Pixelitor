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
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import static java.lang.String.format;

/**
 * A PixelitorEdit that represents the renaming of a layer
 */
public class LayerRenameEdit extends PixelitorEdit {
    private final Layer layer;
    private final String nameBefore;
    private final String nameAfter;

    public LayerRenameEdit(Layer layer, String nameBefore, String nameAfter) {
        super(format("Rename Layer to \"%s\"", nameAfter), layer.getComp());

        this.layer = layer;
        this.nameBefore = nameBefore;
        this.nameAfter = nameAfter;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setName(nameBefore, false);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.setName(nameAfter, false);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.addQuotedString("name before", nameBefore);
        node.addQuotedString("name after", nameAfter);

        return node;
    }
}