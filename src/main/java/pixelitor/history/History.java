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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.layers.Drawable;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;
import java.util.List;

import static java.lang.String.format;

/**
 * Static methods for managing the editing history and undo/redo
 */
public class History {
    private static final UndoableEditSupport editSupport = new UndoableEditSupport();
    private static final PixelitorUndoManager undoManager = new PixelitorUndoManager();
    private static int numUndoneEdits = 0;

    // quietly ignores new edits if true
    private static boolean ignoreEdits = false;

    // it's a program error to add edits if true
    private static boolean rejectEdits = false;

    static {
        setUndoLevels(AppPreferences.loadUndoLevels());
    }

    private History() {
    }

    /**
     * Adds a new edit to the history.
     */
    public static void add(PixelitorEdit edit) {
        assert edit != null;
        if (rejectEdits) {
            // TODO we can get here if undoing something activates a view, and this in turn
            //   creates another edit, such as an auto-rasterization in the shapes tool
            if (AppMode.isDevelopment()) {
                throw new IllegalStateException();
            } else {
                return;
            }
        }
        if (ignoreEdits) {
            return;
        }

        if (edit.makesDirty()) {
            edit.getComp().setDirty(true);
        }

        if (edit.canUndo()) {
            undoManager.addEdit(edit);
        } else {
            undoManager.discardAllEdits();
        }

        // reset BEFORE posting, so that the fade menu item can become enabled
        numUndoneEdits = 0;
        notifyMenus(edit);

        if (AppMode.isDevelopment()) {
            ConsistencyChecks.checkAll(edit.getComp());
        }
    }

    public static void undo() {
        try {
            // increase it before calling undoManager.undo()
            // so that the result of undo is not fadeable
            numUndoneEdits++;
            undoManager.undo();
        } catch (CannotUndoException e) {
            handleUndoRedoException(e, "undo");
        }
    }

    public static void redo() {
        try {
            numUndoneEdits--; // after redo we should be fadeable again
            undoManager.redo();
        } catch (CannotRedoException e) {
            handleUndoRedoException(e, "redo");
        }
    }

    private static void handleUndoRedoException(RuntimeException e, String action) {
        if (RandomGUITest.isRunning()) {
            throw new RuntimeException("No " + action + " available", e);
        }
        Messages.showWarning("Can't " + action,
            "<html>No " + action + " is available. Possible reasons:<ul>" +
                "<li>The edited image was closed" +
                "<li>The " + action + " image was discarded by Pixelitor in order to save memory");
        clear();
    }

    public static void compClosed(Composition closedComp) {
        // Try to minimize the number "no undo/redo is available" dialogs
        // by proactively discarding the edits if the next attempted edit
        // would result in such a message.
        PixelitorEdit nextUndo = undoManager.getEditToBeUndone();
        if (nextUndo != null && nextUndo.getComp() == closedComp) {
            clear();
            return;
        }
        PixelitorEdit nextRedo = undoManager.getEditToBeRedone();
        if (nextRedo != null && nextRedo.getComp() == closedComp) {
            clear();
        }
    }

    public static void notifyMenus(PixelitorEdit edit) {
        editSupport.postEdit(edit);
    }

    public static void notifyMenus() {
        notifyMenus(null);
    }

    public static String getUndoPresentationName() {
        return undoManager.getUndoPresentationName();
    }

    public static String getRedoPresentationName() {
        return undoManager.getRedoPresentationName();
    }

    public static boolean canUndo() {
        return undoManager.canUndo();
    }

    public static boolean canRedo() {
        return undoManager.canRedo();
    }

    public static void addUndoableEditListener(UndoableEditListener listener) {
        editSupport.addUndoableEditListener(listener);
    }

    public static void setUndoLevels(int undoLevels) {
        undoManager.setLimit(undoLevels);
    }

    public static int getUndoLevels() {
        return undoManager.getHeavyEditLimit();
    }

    /**
     * Used for the name of the fade/repeat menu items
     */
    public static String getLastEditName() {
        PixelitorEdit lastEdit = undoManager.getLastEdit();
        if (lastEdit != null) {
            return lastEdit.getName();
        }
        return "";
    }

    public static PixelitorEdit getLastEdit() {
        return undoManager.getLastEdit();
    }

    public static PixelitorEdit getEditToBeUndone() {
        return undoManager.getEditToBeUndone();
    }

    public static int getNumEdits() {
        return undoManager.getSize();
    }

    /**
     * If the last edit in the history is a FadeableEdit for the given
     * {@link Drawable}, return it, otherwise return null.
     */
    public static FadeableEdit getPreviousEditForFade(Drawable dr) {
        if (numUndoneEdits > 0 || dr == null) {
            return null;
        }
        PixelitorEdit lastEdit = undoManager.getLastEdit();
        if (lastEdit instanceof FadeableEdit fadeableEdit) {
            if (!fadeableEdit.isFadeable()) {
                return null;
            }

            Drawable lastLayer = fadeableEdit.getFadingLayer();
            if (dr != lastLayer) {
                // this happens if the active image layer has changed
                // since the last edit, for example by going to mask edit
                return null;
            }
            return fadeableEdit;
        }
        return null;
    }

    public static boolean canFade() {
        Composition comp = Views.getActiveComp();
        if (comp == null) {
            return false;
        }
        Drawable dr = comp.getActiveDrawable();
        if (dr == null) {
            return false;
        }

        return canFade(dr);
    }

    public static boolean canFade(Drawable dr) {
        return getPreviousEditForFade(dr) != null;
    }

    public static void onAllViewsClosed() {
        numUndoneEdits = 0;

        undoManager.discardAllEdits();
        notifyMenus();
    }

    public static void showHistoryDialog() {
        undoManager.showHistoryDialog();
    }

    public static void clear() {
        undoManager.discardAllEdits();
        assertNumEditsIs(0);

        UndoAction.INSTANCE.setEnabled(false);
        RedoAction.INSTANCE.setEnabled(false);
    }

    public static void assertNumEditsIs(int expected) {
        int numEdits = undoManager.getSize();
        if (numEdits != expected) {
            throw new AssertionError(format(
                "Expected %d edits, but found %d", expected, numEdits));
        }
    }

    public static void assertLastEditNameIs(String expected) {
        String lastEditName = undoManager.getLastEdit().getName();
        if (!lastEditName.equals(expected)) {
            throw new AssertionError(format(
                "Expected '%s' as the last edit name, but found '%s'",
                expected, lastEditName));
        }
    }

    public static void assertEditToBeUndoneNameIs(String expected) {
        String name = getEditToBeUndoneName();
        if (!name.equals(expected)) {
            throw new AssertionError(format(
                "Expected '%s', found '%s'", expected, name));
        }
    }

    public static String getEditToBeUndoneName() {
        PixelitorEdit editToBeUndone = undoManager.getEditToBeUndone();
        if (editToBeUndone == null) {
            throw new AssertionError("there is no edit to be undone");
        }
        return editToBeUndone.getName();
    }

    public static void assertEditToBeRedoneNameIs(String expected) {
        String name = getEditToBeRedoneName();
        if (!name.equals(expected)) {
            throw new AssertionError(format(
                "Expected '%s', found '%s'", expected, name));
        }
    }

    public static String getEditToBeRedoneName() {
        PixelitorEdit editToBeRedone = undoManager.getEditToBeRedone();
        if (editToBeRedone == null) {
            throw new AssertionError("there is no edit to be redone");
        }
        return editToBeRedone.getName();
    }

    public static void setIgnoreEdits(boolean ignoreEdits) {
        History.ignoreEdits = ignoreEdits;
    }

    public static void setRejectEdits(boolean rejectEdits) {
        History.rejectEdits = rejectEdits;
    }

    public static DebugNode createDebugNode() {
        var node = new DebugNode("history", undoManager);

        node.addInt("num edits", undoManager.getSize());
        if (undoManager.hasEdits()) {
            node.add(undoManager.createDebugNode("edits"));
        }

        node.addInt("num undone edits", numUndoneEdits);
        node.addBoolean("ignore edits", ignoreEdits);
        node.addBoolean("can undo", canUndo());
        node.addBoolean("can redo", canRedo());
        node.addBoolean("can fade", canFade());

        return node;
    }

    public static void dump() {
        undoManager.dump();
    }

    public static List<String> getEditNames() {
        return undoManager.getEditNames();
    }

    public static void undo(String editName) {
        assertEditToBeUndoneNameIs(editName);
        undo();
    }

    public static void redo(String editName) {
        assertEditToBeRedoneNameIs(editName);
        redo();
    }
}
