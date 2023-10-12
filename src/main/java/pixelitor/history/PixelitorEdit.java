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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.GUIMode;
import pixelitor.Views;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * The abstract superclass for all edits in Pixelitor
 */
public abstract class PixelitorEdit extends AbstractUndoableEdit implements Debuggable {
    protected Composition comp;
    private final String name;
    private final boolean isHeavy;
    private final boolean wasDirty;
    private boolean cleanedByUndo = false;

    // if true, this is part of another edit,
    // not added to the history by itself
    protected boolean embedded;

    protected PixelitorEdit(String name, Composition comp) {
        this(name, comp, false);
    }

    protected PixelitorEdit(String name, Composition comp, boolean isHeavy) {
        assert comp != null;
        assert name != null;
        assert comp.checkInvariants();

        this.comp = comp;
        this.name = name;
        this.isHeavy = isHeavy;

        wasDirty = comp.isDirty();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (GUIMode.isUnitTesting()) {
            return;
        }

        if (!comp.isOpen()) {
            throw new CannotUndoException();
        }

        // any action triggered from this method
        // must not add something to the history
        try {
            History.setForbidEdits(true);

            afterActions();
            cleanIfThisEditMadeItDirty();
        } finally {
            History.setForbidEdits(false);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (GUIMode.isUnitTesting()) {
            return;
        }

        if (!comp.isOpen()) {
            throw new CannotRedoException();
        }

        try {
            History.setForbidEdits(true);

            afterActions();
            makeDirtyAgain();
        } finally {
            History.setForbidEdits(false);
        }
    }

    private void afterActions() {
        activateComp();

        if (!embedded) {
            History.notifyMenus(this);
        }
    }

    private void cleanIfThisEditMadeItDirty() {
        if (!wasDirty && !embedded) {
            comp.setDirty(false);
            cleanedByUndo = true;
        }
    }

    private void makeDirtyAgain() {
        if (cleanedByUndo && makesDirty()) {
            comp.setDirty(true);
        }
    }

    /**
     * Whether this edit should mark the composition as dirty.
     * This method should be called only after full initialization
     * of this object, because for some subclasses it is state-dependent.
     */
    public boolean makesDirty() {
        return true;
    }

    private void activateComp() {
        Views.activate(comp.getView());
    }

    public Composition getComp() {
        return comp;
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
        return getClass().getSimpleName() + "/" + name;
    }

    public boolean isHeavy() {
        return isHeavy;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public DebugNode createDebugNode() {
        String nodeKey = name;

        boolean noNodeName = nodeKey == null || nodeKey.trim().isEmpty();
        if (noNodeName) { // can happen with embedded edits
            nodeKey = getClass().getSimpleName();
        }

        return createDebugNode(nodeKey);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);
        node.addClass();
        node.addQuotedString("comp debug name", comp.getDebugName());
        node.addBoolean("embedded", embedded);
        return node;
    }
}
