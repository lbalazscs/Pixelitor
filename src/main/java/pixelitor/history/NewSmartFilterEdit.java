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

import pixelitor.layers.SmartFilter;
import pixelitor.layers.SmartObject;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class NewSmartFilterEdit extends PixelitorEdit {
    private final SmartObject smartObject;
    private final SmartFilter smartFilter;

    public NewSmartFilterEdit(SmartObject smartObject, SmartFilter smartFilter) {
        super("Add Smart " + smartFilter.getName(), smartObject.getComp());

        this.smartObject = smartObject;
        this.smartFilter = smartFilter;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        smartObject.deleteSmartFilter(smartFilter, false);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        smartObject.addSmartFilter(smartFilter, false, true);
    }
}
