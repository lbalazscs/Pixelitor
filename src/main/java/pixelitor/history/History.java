/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.ConsistencyChecks;
import pixelitor.GUIMode;
import pixelitor.Views;
import pixelitor.layers.Drawable;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.Events;
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
    private static final UndoableEditSupport undoableEditSupport = new UndoableEditSupport();
    private static final PixelitorUndoManager undoManager = new PixelitorUndoManager();
    private static int numUndoneEdits = 0;
    private static boolean ignoreEdits = false;
    private static boolean forbidEdits = false;

    static {
        setUndoLevels(AppPreferences.loadUndoLevels());
    }

    private History() {
    }

    public static void notifyMenus(PixelitorEdit edit) {
        undoableEditSupport.postEdit(edit);
    }

    public static void add(PixelitorEdit edit) {
//        Debug.debugCall(edit.getDebugName(), 4);

        assert edit != null;
        if (forbidEdits) {
            // TODO we can get here if undoing something activates a view, and this in turn
            //   creates another edit, such as an auto-rasterization in the shapes tool
            if (GUIMode.isDevelopment()) {
                throw new IllegalStateException();
            } else {
                return;
            }
        }
        if (ignoreEdits) {
            return;
        }

        var comp = edit.getComp();

        if (edit.makesDirty()) {
            comp.setDirty(true);
        }

        if (edit.canUndo()) {
            undoManager.addEdit(edit);
        } else {
            undoManager.discardAllEdits();
        }

        // reset BEFORE posting, so that the fade menu item can become enabled
        numUndoneEdits = 0;
        undoableEditSupport.postEdit(edit);

        if (GUIMode.isDevelopment()) {
            Events.postAddToHistoryEvent(edit);

            ConsistencyChecks.checkAll(comp, false);
        }
    }

    public static String getUndoPresentationName() {
        return undoManager.getUndoPresentationName();
    }

    public static String getRedoPresentationName() {
        return undoManager.getRedoPresentationName();
    }

    public static void undo() {
        if (GUIMode.isDevelopment()) {
            PixelitorEdit edit = undoManager.getEditToBeUndone();
            Events.postUndoEvent(edit);
//            Debug.call(edit.getDebugName());
        }

        try {
            // increase it before calling undoManager.undo()
            // so that the result of undo is not fadeable
            numUndoneEdits++;
            undoManager.undo();
        } catch (CannotUndoException e) {
            handleCannotException(e, "undo");
        }
    }

    public static void redo() {
        if (GUIMode.isDevelopment()) {
            PixelitorEdit edit = undoManager.getEditToBeRedone();
            Events.postRedoEvent(edit);
//            Debug.call(edit.getDebugName());
        }

        try {
            numUndoneEdits--; // after redo we should be fadeable again
            undoManager.redo();
        } catch (CannotRedoException e) {
            handleCannotException(e, "redo");
        }
    }

    private static void handleCannotException(RuntimeException e, String type) {
        if (RandomGUITest.isRunning()) {
            throw new RuntimeException("No " + type + " available", e);
        } else {
            Messages.showWarning("No " + type + " available",
                "<html>No " + type + " is available, possible reasons are:<ul>" +
                "<li>The edited image was closed" +
                "<li>The " + type + " image was discarded by Pixelitor in order to save memory");
            clear();
        }
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

    @VisibleForTesting
    public static PixelitorEdit getLastEdit() {
        return undoManager.getLastEdit();
    }

    public static PixelitorEdit getEditToBeUndone() {
        return undoManager.getEditToBeUndone();
    }

    /**
     * If the last edit in the history is a FadeableEdit for the given
     * image layer, return it, otherwise return an empty Optional
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
        var comp = Views.getActiveComp();
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
        undoableEditSupport.postEdit(null);
    }

    public static void showHistory() {
        undoManager.showHistory();
    }

    @VisibleForTesting
    public static void clear() {
        undoManager.discardAllEdits();
        assertNumEditsIs(0);

        UndoAction.INSTANCE.setEnabled(false);
        RedoAction.INSTANCE.setEnabled(false);
    }

    @VisibleForTesting
    public static void assertNumEditsIs(int expected) {
        int numEdits = undoManager.getSize();
        if (numEdits != expected) {
            throw new AssertionError(format(
                "Expected %d edits, but found %d", expected, numEdits));
        }
    }

    @VisibleForTesting
    public static void assertLastEditNameIs(String expected) {
        String lastEditName = undoManager.getLastEdit().getName();
        if (!lastEditName.equals(expected)) {
            throw new AssertionError(format(
                "Expected '%s' as the last edit name, but found '%s'",
                expected, lastEditName));
        }
    }

    @VisibleForTesting
    public static void assertEditToBeUndoneNameIs(String expected) {
        String name = getEditToBeUndoneName();
        if (!name.equals(expected)) {
            throw new AssertionError(format(
                "Expected '%s', found '%s'", expected, name));
        }
    }

    @VisibleForTesting
    public static String getEditToBeUndoneName() {
        PixelitorEdit editToBeUndone = undoManager.getEditToBeUndone();
        if (editToBeUndone == null) {
            throw new AssertionError("there is no edit to be undone");
        }
        return editToBeUndone.getName();
    }

    @VisibleForTesting
    public static void assertEditToBeRedoneNameIs(String expected) {
        String name = getEditToBeRedoneName();
        if (!name.equals(expected)) {
            throw new AssertionError(format(
                "Expected '%s', found '%s'", expected, name));
        }
    }

    @VisibleForTesting
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

    public static void setForbidEdits(boolean forbidEdits) {
        History.forbidEdits = forbidEdits;
    }

    public static DebugNode createDebugNode() {
        var node = new DebugNode("history", undoManager);

        node.addInt("num edits", undoManager.getSize());
        if (undoManager.hasEdits()) {
            node.add(undoManager.createDebugNode());
        }

        node.addInt("num undone edits", numUndoneEdits);
        node.addBoolean("ignore edits", ignoreEdits);
        node.addBoolean("can undo", canUndo());
        node.addBoolean("can redo", canRedo());
        node.addBoolean("can fade", canFade());

        return node;
    }

    @VisibleForTesting
    public static void dump() {
        undoManager.dump();
    }

    @VisibleForTesting
    public static List<String> getEditNames() {
        return undoManager.getEditNames();
    }

    @VisibleForTesting
    public static void undo(String editName) {
        assertEditToBeUndoneNameIs(editName);
        undo();
    }

    @VisibleForTesting
    public static void redo(String editName) {
        assertEditToBeRedoneNameIs(editName);
        redo();
    }
}
