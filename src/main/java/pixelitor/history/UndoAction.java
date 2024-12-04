/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.TaskAction;
import pixelitor.utils.Icons;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

public class UndoAction extends TaskAction implements UndoableEditListener {
    private static final String UNDO_TEXT = UIManager.getString(
        "AbstractUndoableEdit.undoText");

    public static final Action INSTANCE = new UndoAction();

    private UndoAction() {
        super(UNDO_TEXT, Icons.getUndoIcon(), History::undo);

        History.addUndoableEditListener(this);
        setEnabled(false);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        setEnabled(History.canUndo());
        setText(History.getUndoPresentationName());
    }
}
