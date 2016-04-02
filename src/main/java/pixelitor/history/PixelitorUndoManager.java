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

import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * An undo manager that is also a list model for debugging history
 */
public class PixelitorUndoManager extends UndoManager implements ListModel<PixelitorEdit> {
    private final HistoryListSelectionModel selectionModel;
    private final EventListenerList listenerList = new EventListenerList();
    private JDialog historyDialog;

    private PixelitorEdit selectedEdit;

    /**
     * When we get a selection event and this variable is true,
     * we can be sure that the change was initiated by the user
     * through the history GUI, and not through addEdit, undo, redo calls
     */
    private boolean manualSelectionChange = true;

    public PixelitorUndoManager() {
        selectionModel = new HistoryListSelectionModel();
        selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);

        selectionModel.addListSelectionListener(e -> {
            if (!manualSelectionChange) {
                return;
            }

            // if the selection was changed by clicking on the JList
            // in the history panel, then jump to the correct state
            int selectedIndex = getSelectedIndex();
            assert selectedIndex != -1;
            PixelitorEdit newSelectedEdit = getElementAt(selectedIndex);

            if (newSelectedEdit != selectedEdit) {
                jumpTo(newSelectedEdit);
            }
        });
    }

    /**
     * This method is necessary mostly because lastEdit() in CompoundEdit is protected
     */
    public PixelitorEdit getLastEdit() {
        UndoableEdit edit = super.lastEdit();
        return (PixelitorEdit) edit;
    }

    @Override
    public boolean addEdit(UndoableEdit edit) {
        assert edit instanceof PixelitorEdit;

        // 1. do the actual addEdit
        boolean retVal = super.addEdit(edit);

        // 2. update the selection model
        manualSelectionChange = false;
        int index = edits.size() - 1;
        fireIntervalAdded(this, index, index);
        selectionModel.setSelectionInterval(index, index);
        manualSelectionChange = true;

        selectedEdit = (PixelitorEdit) edit;

        return retVal;
    }

    @Override
    public void undo() throws CannotUndoException {
        String editName = selectedEdit.getName();

        // 1. do the actual undo
        super.undo();

        // 2. update the selection model
        manualSelectionChange = false;
        int index = getSelectedIndex();
        if (index > 0) {
            int prevIndex = index - 1;
            selectionModel.setSelectionInterval(prevIndex, prevIndex);
            selectedEdit = (PixelitorEdit) edits.get(prevIndex);
        } else {
            selectionModel.setAllowDeselect(true);
            selectionModel.clearSelection();
            selectionModel.setAllowDeselect(false);
            selectedEdit = null;
        }
        manualSelectionChange = true;

        // 3. show status message
        Messages.showStatusMessage(editName + " undone.");
    }

    @Override
    public void redo() throws CannotRedoException {
        // 1. do the actual redo
        super.redo();

        // 2. update the selection model
        manualSelectionChange = false;
        if (selectionModel.isSelectionEmpty()) {
            // the first gets selected
            selectionModel.setSelectionInterval(0, 0);
            selectedEdit = (PixelitorEdit) edits.get(0);
        } else {
            int index = getSelectedIndex();
            int nextIndex = index + 1;
            selectionModel.setSelectionInterval(nextIndex, nextIndex);
            selectedEdit = (PixelitorEdit) edits.get(nextIndex);
        }
        manualSelectionChange = true;

        // this will be true only after the redo is done!
        String editName = selectedEdit.getName();

        // 3. show status message
        Messages.showStatusMessage(editName + " redone.");
    }

    public int getSelectedIndex() {
        if (selectionModel.isSelectionEmpty()) {
            return -1;
        }

        return selectionModel.getLeadSelectionIndex();
    }

    // ListModel methods

    @Override
    public int getSize() {
        return edits.size();
    }

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

    /**
     * Jumps in the history so that we have the state after the given edit
     */
    private void jumpTo(PixelitorEdit targetEdit) {
        assert targetEdit != selectedEdit;

        int targetIndex = edits.indexOf(targetEdit);
        int currentIndex = edits.indexOf(selectedEdit);

        assert targetIndex != currentIndex;

        if (targetIndex > currentIndex) {
            // redo until necessary
            while (currentIndex < targetIndex) {
                super.redo();
                currentIndex++;
            }
        } else {
            // undo until necessary
            while (currentIndex > targetIndex) {
                super.undo();
                currentIndex--;
            }
        }
        selectedEdit = targetEdit;
    }

    public void showHistory() {
        if (historyDialog == null) {
            JList<PixelitorEdit> historyList = new JList<>(this);
            historyList.setSelectionModel(selectionModel);

            historyDialog = new JDialog(PixelitorWindow.getInstance(), "History", false);
            JPanel p = new HistoryPanel(this, historyList);
            historyDialog.getContentPane().add(p);

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

    @VisibleForTesting
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

    // this method is called whenever a not undoable edit was added
    @Override
    public synchronized void discardAllEdits() {
        int numEdits = edits.size();
        if (numEdits == 0) {
            return;
        }

        // discard form the history
        super.discardAllEdits();

        // discard from the GUI
        manualSelectionChange = false;
        int maxIndex = numEdits - 1;
        fireIntervalRemoved(this, 0, maxIndex);
        manualSelectionChange = true;
    }

    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("Edits", this);

        for (int i = 0; i < getSize(); i++) {
            PixelitorEdit edit = getElementAt(i);
            node.add(edit.getDebugNode());
        }

        return node;
    }
}
