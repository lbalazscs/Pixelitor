/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.history;

import pixelitor.Composition;

import javax.swing.undo.AbstractUndoableEdit;

/**
 * The abstract superclass for all edits in Pixelitor
 */
public abstract class PixelitorEdit extends AbstractUndoableEdit {
    Composition comp;
    private final String name;

    PixelitorEdit(Composition comp, String name) {
        this.comp = comp;
        this.name = name;
    }

    public Composition getComp() {
        return comp;
    }

    @Override
    public void die() {
        super.die();

        comp = null;
    }

    @Override
    public String getPresentationName() {
        return name;
    }

    public String getDebugName() {
        return getPresentationName();
    }

    public abstract boolean canRepeat();

    public String dump() {
        String compDescr = comp == null ? "null" : comp.getName();
        return name + " (" + compDescr + ')';
    }

    @Override
    public String toString() {
        return name;
    }
}
