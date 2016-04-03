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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A PixelitorEdit that represents two edits
 * that need to be undone/redone together.
 * Similar in purpose to javax.swing.undo.CompoundEdit
 */
public class LinkedEdit extends PixelitorEdit {
    private final PixelitorEdit first;
    private final PixelitorEdit second;

    public LinkedEdit(Composition comp, String name, PixelitorEdit first, PixelitorEdit second) {
        super(comp, name);
        this.first = first;
        this.second = second;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        second.undo();
        first.undo();

        finish();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        first.redo();
        second.redo();

        finish();
    }

    private void finish() {
        // cleanup is actually not necessary because
        // the member edits usually are not embedded

//        comp.imageChanged(FULL);
//        Layer layer = comp.getActiveLayer();
//        if(layer instanceof ImageLayer) {
//            ((ImageLayer) layer).updateIconImage();
//        }
//        if(layer.hasMask()) {
//            layer.getMask().updateIconImage();
//        }
        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        comp = null;
        first.die();
        second.die();
    }

    @Override
    public boolean canRepeat() {
        return first.canRepeat() && second.canRepeat();
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.add(first.getDebugNode());
        node.add(second.getDebugNode());

        return node;
    }
}