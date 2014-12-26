/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.history;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.ImageComponents;
import pixelitor.selection.Selection;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Dialogs;
import pixelitor.utils.test.DebugEventQueue;
import pixelitor.utils.test.HistoryEvent;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;

/**
 * Manages history and undo/redo for all open images
 */
public final class History {
    private static final UndoableEditSupport undoableEditSupport = new UndoableEditSupport();
    private static final PixelitorUndoManager undoManager = new PixelitorUndoManager();
    private static int undoDepth = 0; // how deep we are back in time

    static {
        setUndoLevels(AppPreferences.loadUndoLevels());
    }

    /**
     * Utility class with static methods
     */
    private History() {
    }

    /**
     * This is used to notify the menu items
     */
    public static void postEdit(PixelitorEdit edit) {
        undoableEditSupport.postEdit(edit);
    }

    public static void addEdit(PixelitorEdit edit) {
        if (edit == null) {
            // TODO temporarily only return because ShapeLayer doesn't implement createTranslateEdit properly
            return;

            // throw new IllegalArgumentException("edit is null");
        }

        if (edit.canUndo()) {
            undoManager.addEdit(edit);
        } else {
            undoManager.discardAllEdits();
        }

        undoDepth = 0; // reset before calling postEdit, so that the fade menu item can become enabled
        undoableEditSupport.postEdit(edit);

        if (Build.CURRENT != Build.FINAL) {
            ConsistencyChecks.checkAll();
            DebugEventQueue.post(new HistoryEvent(edit));
        }
    }

    public static String getUndoPresentationName() {
        return undoManager.getUndoPresentationName();
    }

    public static String getRedoPresentationName() {
        return undoManager.getRedoPresentationName();
    }

    public static void redo() {
        if (Build.CURRENT != Build.FINAL) {
            DebugEventQueue.post(HistoryEvent.createRedoEvent());
        }

        try {
            undoManager.redo();
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
        undoDepth--;
    }

    public static void undo() {
        if (Build.CURRENT != Build.FINAL) {
            DebugEventQueue.post(HistoryEvent.createUndoEvent());
        }

        try {
            undoDepth++; // increase it before calling undoManager.undo() so that the result of undo is not fadeable
            undoManager.undo();
        } catch (CannotUndoException e) {
            Dialogs.showInfoDialog("No undo available", "No undo available, probably because the undo image was discarded in order to save memory");
        } catch (Exception ex) {
            Dialogs.showExceptionDialog(ex);
        }
    }

    public static boolean canUndo() {
        return undoManager.canUndo();
    }

    public static boolean canRedo() {
        return undoManager.canRedo();
    }

    public static void addUndoableEditListener(UndoableEditListener listener) {
        undoableEditSupport.addUndoableEditListener(listener);
    }

    public static void setUndoLevels(int undoLevels) {
        undoManager.setLimit(undoLevels);
    }

    public static int getUndoLevels() {
        return undoManager.getLimit();
    }

    public static boolean canRepeatOperation() {
        if (undoDepth > 0) {
            return false;
        }

        PixelitorEdit lastEdit = undoManager.getLastEdit();
        if (lastEdit != null) {
            return lastEdit.canRepeat();
        }
        return false;
    }

    /**
     * Used for the name of the fade/repeat menu items
     */
    public static String getLastPresentationName() {
        PixelitorEdit lastEdit = undoManager.getLastEdit();
        if (lastEdit != null) {
            return lastEdit.getPresentationName();
        }
        return "";
    }

    /**
     * If the last edit in the history is a FadeableEdit for the given composition,
     * return it
     */
    public static FadeableEdit getPreviousEditForFade(Composition comp) {
        if (undoDepth > 0) {
            return null;
        }
        PixelitorEdit lastEdit = undoManager.getLastEdit();
        if (lastEdit instanceof FadeableEdit) {
            Composition lastComp = lastEdit.getComp();
            if (comp != lastComp) {
                return null;
            }
            return (FadeableEdit) lastEdit;
        }
        return null;
    }

    public static boolean canFade() {
        Composition comp = ImageComponents.getActiveComp();
        return (getPreviousEditForFade(comp) != null);
    }

    public static void allImagesAreClosed() {
        undoDepth = 0;
//        lastFadeableEdit = null;

        undoManager.discardAllEdits();
        undoableEditSupport.postEdit(null);
    }

    public static void showHistory() {
        undoManager.showHistory();
    }

    public static void dumpHistory() {
        showHistory();
        System.out.println("HISTORY DUMP undoDepth = " + undoDepth);
        Composition comp = ImageComponents.getActiveComp();
        if (comp != null) {
            Selection selection = comp.getSelection();
            System.out.println("History.dumpHistory selection = " + selection);
        }

        undoManager.dumpHistory();
    }

    // for debugging only
    public static PixelitorEdit getLastEdit() {
        return undoManager.getLastEdit();
    }
}

