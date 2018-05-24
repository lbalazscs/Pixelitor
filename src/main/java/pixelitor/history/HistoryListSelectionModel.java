/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;

/**
 * The ListSelectionModel used by the history JList
 */
public class HistoryListSelectionModel extends DefaultListSelectionModel {
    /**
     * We don't want to allow the user to simply deselect an item in
     * the history list (for example by Ctrl-clicking on it), on the
     * other hand the list should be able to be deselected in one special
     * case: if all edits are undone.
     */
    private boolean allowDeselect = false;

    public HistoryListSelectionModel() {
        setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public void clearSelection() {
        if (allowDeselect) {
            super.clearSelection();
        }
    }

    @Override
    public void removeSelectionInterval(int index0, int index1) {
        if (allowDeselect) {
            super.removeSelectionInterval(index0, index1);
        }
    }

    public void setAllowDeselect(boolean allowDeselect) {
        this.allowDeselect = allowDeselect;
    }

    public void setSelectedIndex(int index) {
        setSelectionInterval(index, index);
    }

    public int getSelectedIndex() {
        if (isSelectionEmpty()) {
            return -1;
        }

        return getLeadSelectionIndex();
    }
}
