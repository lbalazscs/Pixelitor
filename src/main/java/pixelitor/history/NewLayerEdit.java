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
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.UpdateGUI;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents the creation of a new layer
 * (either as a new empty layer or via layer duplication)
 */
public class NewLayerEdit extends PixelitorEdit {
    private Layer activeLayerBefore;
    private Layer newLayer;
    private final int newLayerIndex;
    private final MaskViewMode oldViewMode;

    public NewLayerEdit(Composition comp, Layer newLayer, Layer activeLayerBefore, String historyName, MaskViewMode oldViewMode) {
        super(comp, historyName);

        this.activeLayerBefore = activeLayerBefore;
        this.oldViewMode = oldViewMode;
        this.newLayer = newLayer;
        this.newLayerIndex = comp.getLayerIndex(newLayer);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        comp.deleteLayer(newLayer, AddToHistory.NO, UpdateGUI.YES);
        comp.setActiveLayer(activeLayerBefore, AddToHistory.NO);

        oldViewMode.activate(comp, activeLayerBefore);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.addLayer(newLayer, AddToHistory.NO, null, true, newLayerIndex);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        newLayer = null;
        activeLayerBefore = null;
    }
}
