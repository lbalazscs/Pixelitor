/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.PixelitorWindow;
import pixelitor.utils.GUIUtils;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.Optional;

/**
 * An undo manager that is also a list model for debugging history
 */
public class PixelitorUndoManager extends UndoManager implements ListModel<UndoableEdit> {
    private final DefaultListSelectionModel selectionModel;
    private final EventListenerList listenerList = new EventListenerList();
    private JDialog historyDialog;

    public PixelitorUndoManager() {
        selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * This method is necessary mostly because lastEdit() in CompoundEdit is protected
     */
    public Optional<PixelitorEdit> getLastEdit() {
        UndoableEdit edit = super.lastEdit();
        if (edit != null) {
            return Optional.of((PixelitorEdit) edit);
        }

        return Optional.empty();
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        boolean retVal = super.addEdit(anEdit);

        int index = edits.size() - 1;
        fireIntervalAdded(this, index, index);
        selectionModel.setSelectionInterval(index, index);

        return retVal;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        int index = selectionModel.getLeadSelectionIndex();
        if (index > 0) {
            selectionModel.setSelectionInterval(index - 1, index - 1);
        } else {
            selectionModel.clearSelection();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (selectionModel.isSelectionEmpty()) {
            selectionModel.setSelectionInterval(0, 0);
        } else {
            int index = selectionModel.getLeadSelectionIndex();
            selectionModel.setSelectionInterval(index + 1, index + 1);
        }
    }

    // ListModel methods

    @Override
    public int getSize() {
        return edits.size();
    }

    @Override
    public UndoableEdit getElementAt(int index) {
        return edits.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    protected void fireIntervalAdded(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalAdded(e);
            }
        }
    }

    public void showHistory() {
        JList<UndoableEdit> historyList = new JList<>(this);
        historyList.setSelectionModel(selectionModel);

        if (historyDialog == null) {
            historyDialog = new JDialog(PixelitorWindow.getInstance(), "History", false);
            historyDialog.getContentPane().add(new JScrollPane(historyList));
            historyDialog.setSize(200, 300);
            GUIUtils.centerOnScreen(historyDialog);
        }

        if (!historyDialog.isVisible()) {
            historyDialog.setVisible(true);
        }
    }

    public void dumpHistory() {
        int numEdits = edits.size();
        System.out.println("PixelitorUndoManager.dumpHistory:");
        for (int i = 0; i < numEdits; i++) {
            PixelitorEdit edit = (PixelitorEdit) edits.get(i);
            System.out.println("edit [" + i + "] = " + edit.dump());
        }
    }
}
