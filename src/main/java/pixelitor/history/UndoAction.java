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

import pixelitor.gui.utils.PAction;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

public class UndoAction extends PAction implements UndoableEditListener {
    private static final String UNDO_TEXT = UIManager.getString(
        "AbstractUndoableEdit.undoText");

    public static final Action INSTANCE = new UndoAction();

    private UndoAction() {
        super(UNDO_TEXT);

        History.addUndoableEditListener(this);
        setEnabled(false);
    }

    @Override
    protected void onClick() {
        History.undo();
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        setEnabled(History.canUndo());
        setText(History.getUndoPresentationName());
    }
}
