/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the hiding or showing of a layer
 */
public class LayerVisibilityChangeEdit extends PixelitorEdit {
    private Layer layer;
    private final boolean newVisibility;

    public LayerVisibilityChangeEdit(Composition comp, Layer layer, boolean newVisibility) {
        super(comp, newVisibility ? "Show Layer" : "Hide Layer");

        this.newVisibility = newVisibility;
        this.layer = layer;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setVisible(!newVisibility, AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.setVisible(newVisibility, AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }
}