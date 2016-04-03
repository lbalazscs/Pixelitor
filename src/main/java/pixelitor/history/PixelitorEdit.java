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
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.AbstractUndoableEdit;

/**
 * The abstract superclass for all edits in Pixelitor
 */
public abstract class PixelitorEdit extends AbstractUndoableEdit {
    protected Composition comp;
    private final String name;

    protected boolean embedded; // if true, this is not a standalone edit

    PixelitorEdit(Composition comp, String name) {
        assert comp != null;
        assert name != null;

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

    /**
     * Can be overridden to include more information
     */
    public String getDebugName() {
        return name;
    }

    public boolean canRepeat() {
        return false;
    }

    public String dump() {
        String compDescr = comp == null ? "null" : comp.getName();
        return name + " (" + compDescr + ')';
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
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
        node.addClassChild();
        node.addQuotedStringChild("Comp", comp.getName());
        node.addBooleanChild("Embedded", embedded);
        return node;
    }
}
