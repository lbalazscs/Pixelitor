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

import pixelitor.Composition;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.Shape;
import java.util.Objects;

/**
 * Represents a deselect
 */
public class DeselectEdit extends PixelitorEdit {
    private final Shape backupShape;
    private final String reason;

    public DeselectEdit(Composition comp, Shape backupShape, String reason) {
        super("Deselect", comp);

        this.reason = reason;
        this.backupShape = Objects.requireNonNull(backupShape);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        assert !comp.hasSelection();

        comp.setSelectionFromShape(backupShape);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        comp.deselect(false);
    }

    @Override
    public String getDebugName() {
        return super.getDebugName() + " (reason = \"" + reason + "\")";
    }
}
