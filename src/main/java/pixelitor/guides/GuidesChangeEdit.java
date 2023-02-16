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

package pixelitor.guides;

import pixelitor.Composition;
import pixelitor.history.PixelitorEdit;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Represents the creation, deletion or change of guides.
 */
public class GuidesChangeEdit extends PixelitorEdit {
    private final Guides oldGuides;
    private final Guides newGuides;

    public GuidesChangeEdit(Composition comp, Guides oldGuides, Guides newGuides) {
        super(createEditName(oldGuides, newGuides), comp);
        this.oldGuides = oldGuides;
        this.newGuides = newGuides;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        set(oldGuides);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        set(newGuides);
    }

    private void set(Guides guides) {
        // the zooming might have changed since the last undo/redo
        if (guides != null) {
            guides.coCoordsChanged(comp.getView());
        }

        comp.setGuides(guides);

        if (!embedded) {
            comp.repaint();
        }
    }

    private static String createEditName(Guides oldGuides, Guides newGuides) {
        if (oldGuides == null) {
            assert newGuides != null;
            return "Create Guides";
        } else if (newGuides == null) {
            assert oldGuides != null;
            return "Clear Guides";
        } else {
            return "Change Guides";
        }
    }
}
