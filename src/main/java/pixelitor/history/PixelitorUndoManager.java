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
import java.awt.BorderLayout;

/**
 * An undo manager that is also a list model for debugging history
 */
public class PixelitorUndoManager extends UndoManager implements ListModel<PixelitorEdit> {
    private final DefaultListSelectionModel selectionModel;
    private final EventListenerList listenerList = new EventListenerList();
    private JDialog historyDialog;
    private PixelitorEdit selectedEdit;
    private boolean manualUserJump = true;

    public PixelitorUndoManager() {
        selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
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

        boolean retVal = super.addEdit(edit);

        manualUserJump = false;
        int index = edits.size() - 1;
        fireIntervalAdded(this, index, index);
        selectionModel.setSelectionInterval(index, index);
        manualUserJump = true;

        selectedEdit = (PixelitorEdit) edit;

        return retVal;
    }

    @Override
    public void undo() throws CannotUndoException {
        // 1. do the actual undo
        super.undo();

        // 2. update the selection model
        manualUserJump = false;
        int index = selectionModel.getLeadSelectionIndex();
        if (index > 0) {
            selectionModel.setSelectionInterval(index - 1, index - 1);
        } else {
            selectionModel.clearSelection();
        }
        manualUserJump = true;
    }

    @Override
    public void redo() throws CannotRedoException {
        // 1. do the actual redo
        super.redo();

        // 2. update the selection model
        manualUserJump = false;
        if (selectionModel.isSelectionEmpty()) {
            selectionModel.setSelectionInterval(0, 0);
        } else {
            int index = selectionModel.getLeadSelectionIndex();
            selectionModel.setSelectionInterval(index + 1, index + 1);
        }
        manualUserJump = true;
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

    /**
     * Jumps in the history so that we have the state after the given edit
     */
    private void jumpTo(PixelitorEdit edit) {
        assert edit != selectedEdit;

        int targetIndex = edits.indexOf(edit);
        int currentIndex = edits.indexOf(selectedEdit);

        assert targetIndex != currentIndex;
//        assert currentIndex == indexOfNextAdd - 1;

        System.out.println(String.format("PixelitorUndoManager::jumpTo: " +
                        "name = '%s' currentIndex = %d, targetIndex = %d",
                edit.getName(), currentIndex, targetIndex));

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
    }

    public void showHistory() {
        JList<PixelitorEdit> historyList = new JList<>(this);
        historyList.setSelectionModel(selectionModel);
        historyList.addListSelectionListener(e -> {
            if (!manualUserJump) {
                return;
            }
            PixelitorEdit newSelectedEdit = historyList.getSelectedValue();
            if (newSelectedEdit != selectedEdit) {
                jumpTo(newSelectedEdit);
                selectedEdit = newSelectedEdit;
            }
        });

        if (historyDialog == null) {
            historyDialog = new JDialog(PixelitorWindow.getInstance(), "History", false);
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(new JScrollPane(historyList), BorderLayout.CENTER);
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.add(new JButton(History.UNDO_ACTION));
            buttonsPanel.add(new JButton(History.REDO_ACTION));
            p.add(buttonsPanel, BorderLayout.SOUTH);
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
}
