/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.GradientFillLayer;
import pixelitor.tools.gradient.Gradient;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class GradientFillLayerChangeEdit extends PixelitorEdit {
    private final GradientFillLayer layer;
    private final Gradient before;
    private final Gradient after;

    public GradientFillLayerChangeEdit(GradientFillLayer layer, Gradient before, Gradient after) {
        super("Gradient Fill Layer Change", layer.getComp());

        this.layer = layer;
        this.before = before;
        this.after = after;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setGradient(before, false);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.setGradient(after, false);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.addNullableChild("before", before);
        node.addNullableChild("after", after);

        return node;
    }
}
