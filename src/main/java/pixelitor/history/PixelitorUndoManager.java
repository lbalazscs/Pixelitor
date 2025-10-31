/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.Threads;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * The undo manager, and also the list model for the history GUI.
 */
public class PixelitorUndoManager extends TwoLimitsUndoManager implements ListModel<PixelitorEdit>, Debuggable {
    private final HistoryListSelectionModel selectionModel;
    private final EventListenerList listenerList = new EventListenerList();
    private JDialog historyDialog;
    private PixelitorEdit selectedEdit;

    // indicates that a selection change was initiated by the user
    // through the history GUI, and not through addEdit, undo, redo calls
    private boolean userInitiatedSelection = true;

    public PixelitorUndoManager() {
        selectionModel = new HistoryListSelectionModel();
        selectionModel.addListSelectionListener(e -> selectionChanged());
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        assert edit instanceof PixelitorEdit;

        // 1. do the actual addEdit
        boolean retVal = super.addEdit(edit);

        // 2. update the GUI
        userInitiatedSelection = false;
        int index = edits.size() - 1;
        fireIntervalAdded(this, index, index);
        selectionModel.setSelectedIndex(index);
        userInitiatedSelection = true;

        selectedEdit = (PixelitorEdit) edit;

        return retVal;
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        String editName = selectedEdit.getName();

        // 1. do the actual undo
        super.undo();

        // 2. update the selection model
        userInitiatedSelection = false;
        int index = getSelectedIndex();
        if (index > 0) {
            int prevIndex = index - 1;
            selectionModel.setSelectedIndex(prevIndex);
            selectedEdit = (PixelitorEdit) edits.get(prevIndex);
        } else {
            selectionModel.setAllowDeselect(true);
            selectionModel.clearSelection();
            selectionModel.setAllowDeselect(false);
            selectedEdit = null;
        }
        userInitiatedSelection = true;

        // 3. show status message
        Messages.showPlainStatusMessage(editName + " undone.");
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        // 1. do the actual redo
        super.redo();

        // 2. update the selection model
        userInitiatedSelection = false;
        if (selectionModel.isSelectionEmpty()) {
            // the first gets selected
            selectionModel.setSelectedIndex(0);
            selectedEdit = (PixelitorEdit) edits.getFirst();
        } else {
            int index = getSelectedIndex();
            int nextIndex = index + 1;
            selectionModel.setSelectedIndex(nextIndex);
            selectedEdit = (PixelitorEdit) edits.get(nextIndex);
        }
        userInitiatedSelection = true;

        // this will be true only after the redo is done!
        String editName = selectedEdit.getName();

        // 3. show status message
        Messages.showPlainStatusMessage(editName + " redone.");
    }

    private synchronized void selectionChanged() {
        if (!userInitiatedSelection) {
            return;
        }

        // if the selection was changed by clicking on the JList
        // in the history panel, then jump to the correct state
        PixelitorEdit newSelectedEdit = getElementAt(getSelectedIndex());
        if (newSelectedEdit != selectedEdit) {
            navigateTo(newSelectedEdit);
        }
    }

    /**
     * Navigates the undo history to to the state after the given
     * edit, performing the necessary undo or redo operations.
     */
    private void navigateTo(PixelitorEdit targetEdit) {
        int targetIndex = edits.indexOf(targetEdit);
        int currentIndex = edits.indexOf(selectedEdit);

        if (targetIndex > currentIndex) {
            // redo until reaching the target edit
            navigateForward(currentIndex, targetIndex);
        } else {
            // undo until reaching the target edit
            navigateBackward(currentIndex, targetIndex);
        }
        selectedEdit = targetEdit;
    }

    private void navigateForward(int currentIndex, int targetIndex) {
        while (currentIndex < targetIndex) {
            super.redo();
            currentIndex++;
        }
    }

    private void navigateBackward(int currentIndex, int targetIndex) {
        while (currentIndex > targetIndex) {
            super.undo();
            currentIndex--;
        }
    }

    /**
     * This method is necessary mostly because lastEdit() in CompoundEdit is protected
     */
    public PixelitorEdit getLastEdit() {
        return (PixelitorEdit) super.lastEdit();
    }

    public int getSelectedIndex() {
        return selectionModel.getSelectedIndex();
    }

    public boolean hasEdits() {
        return !edits.isEmpty();
    }

    // ListModel methods

    @Override
    public PixelitorEdit getElementAt(int index) {
        return (PixelitorEdit) edits.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    private void fireIntervalAdded(Object source, int index0, int index1) {
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

    private void fireIntervalRemoved(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalRemoved(e);
            }
        }
    }

    public void showHistoryDialog() {
        assert Threads.calledOnEDT() : Threads.callInfo();

        if (historyDialog == null) {
            createHistoryDialog();
        }

        if (!historyDialog.isVisible()) {
            GUIUtils.showDialog(historyDialog);
        }
    }

    private void createHistoryDialog() {
        historyDialog = new JDialog(PixelitorWindow.get(),
            "History", false);
        JPanel p = new HistoryPanel(this);
        historyDialog.getContentPane().add(p);

        historyDialog.setSize(200, 300);
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    // the super method is not public
    public PixelitorEdit getEditToBeUndone() {
        return (PixelitorEdit) super.editToBeUndone();
    }

    // the super method is not public
    protected PixelitorEdit getEditToBeRedone() {
        return (PixelitorEdit) super.editToBeRedone();
    }

    // called whenever a not undoable edit is added
    @Override
    public synchronized void discardAllEdits() {
        int numEdits = edits.size();
        if (numEdits == 0) {
            return;
        }

        // discard form the history
        super.discardAllEdits();

        // discard from the GUI
        userInitiatedSelection = false;
        fireIntervalRemoved(this, 0, numEdits - 1);
        userInitiatedSelection = true;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

        for (int i = 0, numEdits = getSize(); i < numEdits; i++) {
            PixelitorEdit edit = getElementAt(i);
            node.add(edit.createDebugNode());
        }

        return node;
    }

    public List<String> getEditNames() {
        return edits.stream()
            .map(UndoableEdit::getPresentationName)
            .collect(toList());
    }
}
