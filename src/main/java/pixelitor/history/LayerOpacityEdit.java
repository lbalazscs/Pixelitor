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

import pixelitor.layers.Layer;
import pixelitor.utils.UpdateGUI;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the changes made to the opacity of a layer
 */
public class LayerOpacityEdit extends PixelitorEdit {
    private Layer layer;
    private float backupOpacity;

    public LayerOpacityEdit(Layer layer, float backupOpacity) {
        super(layer.getComp(), "Layer Opacity Change");

        this.layer = layer;
        this.backupOpacity = backupOpacity;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapOpacity();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapOpacity();
    }

    private void swapOpacity() {
        float tmp = layer.getOpacity();

        layer.setOpacity(backupOpacity, UpdateGUI.YES, AddToHistory.NO, true);
        assert layer.getOpacity() == backupOpacity : "backupOpacity = " + backupOpacity + ", current = " + layer.getOpacity();

        backupOpacity = tmp;

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }

    public Layer getLayer() {
        return layer;
    }
}
