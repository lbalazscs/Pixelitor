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
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * The abstract superclass for all edits in Pixelitor
 */
public abstract class PixelitorEdit extends AbstractUndoableEdit {
    protected Composition comp;
    private final String name;

    // if true, this is part of another edit,
    // not added to the history by itself
    protected boolean embedded;

    protected PixelitorEdit(String name, Composition comp) {
        assert comp != null;
        assert name != null;

        this.comp = comp;
        this.name = name;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (!embedded) {
            History.notifyMenus(this);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (!embedded) {
            History.notifyMenus(this);
        }
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

    // same as the presentation name, but shorter method name...
    public String getName() {
        return name;
    }

    public String getDebugName() {
        // by default only the presentation name, but
        // can be overridden to include more information
        return name;
    }

    public boolean canRepeat() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public PixelitorEdit setEmbedded(boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    public DebugNode getDebugNode() {
        String nodeName = name;

        boolean noNodeName = nodeName == null || nodeName.trim().isEmpty();
        if (noNodeName) { // can happen with embedded edits
            nodeName = getClass().getSimpleName();
        }

        DebugNode node = new DebugNode(nodeName, this);
        node.addClass();
        node.addQuotedString("Comp", comp.getName());
        node.addBoolean("Embedded", embedded);
        return node;
    }
}
