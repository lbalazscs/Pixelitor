/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.Drawable;
import pixelitor.menus.MenuAction;
import pixelitor.menus.MenuAction.AllowedLayerType;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Icons;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.test.Events;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Static methods for managing history and undo/redo
 */
public class History {
    private static final UndoableEditSupport undoableEditSupport = new UndoableEditSupport();
    private static final PixelitorUndoManager undoManager = new PixelitorUndoManager();
    private static int numUndoneEdits = 0;
    private static boolean ignoreEdits = false;

    static {
        if (Build.isTesting()) {
            // make sure we have enough undo for the tests
            setUndoLevels(15);
        } else {
            setUndoLevels(AppPreferences.loadUndoLevels());
        }
    }

    public static final Action UNDO_ACTION = new MenuAction("Undo",
            Icons.getUndoIcon(), AllowedLayerType.ANY) {
        @Override
        public void onClick() {
            History.undo();
        }
    };

    public static final Action REDO_ACTION = new MenuAction("Redo",
            Icons.getRedoIcon(), AllowedLayerType.ANY) {
        @Override
        public void onClick() {
            History.redo();
        }
    };

    private History() {
    }

    public static void notifyMenus(PixelitorEdit edit) {
        undoableEditSupport.postEdit(edit);
    }

    public static void addEdit(PixelitorEdit edit) {
        assert edit != null;
        if (ignoreEdits) {
            return;
        }
        edit.getComp().setDirty(true);

        if (edit.canUndo()) {
            undoManager.addEdit(edit);
        } else {
            undoManager.discardAllEdits();
        }

        // reset BEFORE posting, so that the fade menu item can become enabled
        numUndoneEdits = 0;
        undoableEditSupport.postEdit(edit);

        if (Build.CURRENT != Build.FINAL) {
            Events.postAddToHistoryEvent(edit);
            ConsistencyChecks.checkAll(edit.getComp(), false);
        }
    }

    /**
     * Save only the affected area for undo.
     */
    public static void addToolArea(Rectangle rect, BufferedImage origImage,
                                   Drawable dr, boolean relativeToImage,
                                   String toolName) {
        assert rect.width > 0 : "rectangle.width = " + rect.width;
        assert rect.height > 0 : "rectangle.height = " + rect.height;

        if (!relativeToImage) {
            // if the coordinates are relative to the canvas,
            // translate them to be relative to the image
            int dx = -dr.getTX();
            int dy = -dr.getTY();
            rect.translate(dx, dy);
        }

        rect = SwingUtilities.computeIntersection(0, 0,
                origImage.getWidth(), origImage.getHeight(), // full image bounds
                rect
        );

        assert (origImage != null);
        if (rect.isEmpty()) {
            return;
        }

        Composition comp = dr.getComp();

        // we could also intersect with the selection bounds,
        // but typically the extra savings would be minimal

        PartialImageEdit edit = new PartialImageEdit(toolName, comp,
                dr, origImage, rect, false);
        addEdit(edit);
    }

    public static String getUndoPresentationName() {
        return undoManager.getUndoPresentationName();
    }

    public static String getRedoPresentationName() {
        return undoManager.getRedoPresentationName();
    }

    public static void undo() {
        if (Build.CURRENT != Build.FINAL) {
            Events.postUndoEvent(undoManager.getEditToBeUndone());
        }

        try {
            // increase it before calling undoManager.undo()
            // so that the result of undo is not fadeable
            numUndoneEdits++;
            undoManager.undo();
        } catch (CannotUndoException e) {
            Messages.showInfo("No undo available",
                    "No undo available, probably because the undo image was discarded in order to save memory");
        }
    }

    public static void redo() {
        if (Build.CURRENT != Build.FINAL) {
            Events.postRedoEvent(undoManager.getEditToBeRedone());
        }

        try {
            numUndoneEdits--; // after redo we should be fadeable again
            undoManager.redo();
        } catch (CannotRedoException e) {
            Messages.showException(e);
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
        if (numUndoneEdits > 0) {
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

    @VisibleForTesting
    public static String getEditToBeUndoneName() {
        PixelitorEdit edit = undoManager.getEditToBeUndone();
        if (edit != null) {
            return edit.getName();
        }
        return "";
    }

    /**
     * If the last edit in the history is a FadeableEdit for the given
     * image layer, return it, otherwise return an empty Optional
     */
    public static Optional<FadeableEdit> getPreviousEditForFade(Drawable dr) {
        if (numUndoneEdits > 0 || dr == null) {
            return Optional.empty();
        }
        PixelitorEdit lastEdit = undoManager.getLastEdit();
        if (lastEdit != null) {
            if (lastEdit instanceof FadeableEdit) {
                FadeableEdit fadeableEdit = (FadeableEdit) lastEdit;
                if (!fadeableEdit.isFadeable()) {
                    return Optional.empty();
                }

                Drawable lastLayer = fadeableEdit.getFadingLayer();
                if (dr != lastLayer) {
                    // this happens if the active image layer has changed
                    // since the last edit, for example by going to mask edit
                    return Optional.empty();
                }
                return Optional.of(fadeableEdit);
            }
        }
        return Optional.empty();
    }

    public static boolean canFade() {
        Composition comp = ImageComponents.getActiveCompOrNull();
        if (comp == null) {
            return false;
        }
        Drawable dr = comp.getActiveDrawableOrNull();
        if (dr == null) {
            return false;
        }

        return canFade(dr);
    }

    public static boolean canFade(Drawable dr) {
        return getPreviousEditForFade(dr).isPresent();
    }

    public static void onAllImagesClosed() {
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
        String name = undoManager.getEditToBeUndone().getName();
        if (!name.equals(expected)) {
            throw new AssertionError(format(
                    "Expected '%s' as the edit to be undone name, but found '%s'",
                    expected, name));
        }
    }

    @VisibleForTesting
    public static void assertEditToBeRedoneNameIs(String expected) {
        String name = undoManager.getEditToBeRedone().getName();
        if (!name.equals(expected)) {
            throw new AssertionError(format(
                    "Expected '%s' as the edit to be redone name, but found '%s'",
                    expected, name));
        }
    }

    public static void setIgnoreEdits(boolean ignoreEdits) {
        History.ignoreEdits = ignoreEdits;
    }

    public static DebugNode getDebugNode() {
        DebugNode node = new DebugNode("History", undoManager);

        node.addInt("Num edits", undoManager.getSize());
        if (undoManager.hasEdits()) {
            node.add(undoManager.getDebugNode());
        }

        node.addInt("Num undone edits", numUndoneEdits);
        node.addBoolean("Ignore edits", ignoreEdits);
        node.addBoolean("Can undo", History.canUndo());
        node.addBoolean("Can redo", History.canRedo());
        node.addBoolean("Can fade", History.canFade());
        node.addBoolean("Can repeat", History.canRepeatOperation());

        return node;
    }

    @VisibleForTesting
    public static void dump() {
        undoManager.dump();
    }

    @VisibleForTesting
    public static List<String> asStringList() {
        return undoManager.asStringList();
    }
}
