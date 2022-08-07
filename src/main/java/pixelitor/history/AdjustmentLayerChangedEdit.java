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

import pixelitor.filters.Filter;
import pixelitor.layers.AdjustmentLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class AdjustmentLayerChangedEdit extends PixelitorEdit {
    private final AdjustmentLayer layer;
    private Filter backupFilter;

    public AdjustmentLayerChangedEdit(AdjustmentLayer layer, Filter backupFilter) {
        super(backupFilter.getName() + " Changed", layer.getComp());
        this.layer = layer;
        this.backupFilter = backupFilter;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapFilters();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapFilters();
    }

    private void swapFilters() {
        Filter tmp = layer.getFilter();
        layer.setFilter(backupFilter);
        backupFilter = tmp;
    }
}
