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
import pixelitor.ImageComponent;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Used when a file is reloaded.
 */
public class CompositionReplacedEdit extends PixelitorEdit {
    private Composition newComp;
    private ImageComponent ic;

    public CompositionReplacedEdit(Composition oldComp, Composition newComp) {
        super(oldComp, "Reload");
        this.newComp = newComp;
        ic = (ImageComponent) newComp.getIC();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        ic.replaceComp(comp, AddToHistory.NO);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        ic.replaceComp(newComp, AddToHistory.NO);
    }

    @Override
    public void die() {
        super.die();

        newComp = null;
        ic = null;
    }
}
