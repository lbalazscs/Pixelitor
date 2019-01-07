/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.View;
import pixelitor.layers.MaskViewMode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Used when a file is reloaded.
 */
public class CompositionReplacedEdit extends PixelitorEdit {
    private Composition newComp;
    private final MaskViewMode oldMode;
    private View view;

    public CompositionReplacedEdit(String name, View view,
                                   Composition oldComp, Composition newComp,
                                   MaskViewMode oldMode) {
        super(name, oldComp);
        assert oldComp.getFile().equals(newComp.getFile())
                : "old file = " + oldComp.getFile()
                + ", new file = " + newComp.getFile();

        this.newComp = newComp;
        this.oldMode = oldMode;
        this.view = view;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        view.replaceComp(comp, oldMode, false);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        view.replaceComp(newComp, MaskViewMode.NORMAL, false);
    }

    @Override
    public void die() {
        super.die();

        newComp = null;
        view = null;
    }
}
