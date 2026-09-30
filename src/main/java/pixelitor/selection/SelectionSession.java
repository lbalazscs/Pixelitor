/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.selection;

import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionShapeChangeEdit;
import pixelitor.tools.util.Drag;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Messages;

import java.awt.Shape;
import java.util.Locale;

/**
 * A short-lived, single-use helper for building one selection shape
 * interactively. It maintains a draft selection while the user is
 * interacting, then either combines the draft with the existing
 * selection (commit) or restores the previous state (cancel).
 * <p>
 * The selection construction session may span a single continuous drag
 * gesture (Marquee/Freehand), a single click operation (Magic Wand),
 * or a multi-gesture interaction spanning multiple clicks (Polygonal).
 */
public class SelectionSession {
    private final SelectionType selectionType;
    private final ShapeCombinator combinator;
    private Composition comp;

    // preserved for history tracking if replacing an existing selection
    private Shape origSelShape;

    private boolean committed = false;

    // preserved state of the existing selection to restore if drafting is aborted
    private boolean wasHidden = false;
    private boolean wasFrozen = false;

    public SelectionSession(SelectionType selectionType, ShapeCombinator combinator, Composition comp) {
        this.combinator = combinator;
        this.selectionType = selectionType;
        this.comp = comp;

        Selection origSelection = comp.getSelection();
        if (origSelection == null) {
            // nothing to hide or freeze if there's no existing selection to combine with
            return;
        }

        assert origSelection.isValid() : "disposed selection";

        // remember the original display state so it can be restored on cancellation
        wasHidden = origSelection.isHidden();
        wasFrozen = origSelection.isFrozen();

        if (combinator == ShapeCombinator.REPLACE) {
            origSelShape = origSelection.getShape();
            // At this point the mouse was pressed, and it's clear that the
            // existing selection should go away, but we don't know yet whether the
            // mouse will be released at the same point (Deselect) or another
            // point (Replace Selection).
            // Therefore, we don't deselect yet (the selection information
            // will be needed when the mouse is released), only hide.
            origSelection.setHidden(true);
        } else {
            // for ADD, SUBTRACT, or INTERSECT, freeze the marching
            // ants animation to avoid distracting visual noise
            origSelection.setFrozen(true);
        }
    }

    /**
     * Updates the draft selection shape based on drag information.
     * Used by drag-based tools (marquee, freehand).
     */
    public void updateFromDrag(Drag drag) {
        Selection draftSelection = comp.getDraftSelection();

        if (draftSelection == null) { // first update of this interaction
            createDraftFromDrag(drag);
        } else {
            assert draftSelection.isValid() : "disposed draft selection";
            updateDraftFromDrag(draftSelection, drag);
        }
    }

    /**
     * Updates the draft selection shape based on a mouse event.
     * Used by event-based tools (polygonal, magic wand).
     */
    public void updateFromEvent(PMouseEvent e) {
        // update the composition reference, because in a polygonal selection
        // session an undo of a previous CompAction could change it
        // (possibly it would be better to store the view in this class)
        comp = e.getComp();

        Selection draftSelection = comp.getDraftSelection();

        if (draftSelection == null) {
            createDraftFromEvent(e);
        } else {
            assert draftSelection.isValid() : "disposed draft selection";
            updateDraftFromEvent(draftSelection, e);
        }
    }

    // creates a new draft selection from a drag
    private void createDraftFromDrag(Drag drag) {
        Shape newShape = selectionType.createShapeFromDrag(drag, null);
        comp.setDraftSelection(new Selection(newShape, comp.getView()));
    }

    // updates an existing draft selection from a drag
    private void updateDraftFromDrag(Selection draftSelection, Drag drag) {
        Shape currentShape = draftSelection.getShape();
        // passing the current shape lets incremental types (freehand)
        // extend it, while rectangles and ellipses ignore it
        Shape newShape = selectionType.createShapeFromDrag(drag, currentShape);
        applyShapeToDraft(draftSelection, newShape);
    }

    // creates a new draft selection from a mouse event
    private void createDraftFromEvent(PMouseEvent e) {
        assert e != null;
        Shape newShape = selectionType.createShapeFromEvent(e, null);
        comp.setDraftSelection(new Selection(newShape, comp.getView()));
    }

    // updates an existing draft selection from a mouse event
    private void updateDraftFromEvent(Selection draftSelection, PMouseEvent e) {
        Shape currentShape = draftSelection.getShape();
        Shape newShape = selectionType.createShapeFromEvent(e, currentShape);
        applyShapeToDraft(draftSelection, newShape);
    }

    private static void applyShapeToDraft(Selection draftSelection, Shape newShape) {
        draftSelection.setShape(newShape);

        // normally already marching (the Selection constructor starts it)
        if (!draftSelection.isMarching()) {
            draftSelection.startMarching();
        }
    }

    /**
     * Finalizes the selection by combining the draft shape with
     * any existing selection according to the combination mode.
     */
    public void commit() {
        Selection draftSelection = comp.getDraftSelection();

        Shape newShape = draftSelection.getShape();
        newShape = comp.clipToCanvasBounds(newShape);
        if (newShape.getBounds2D().isEmpty()) {
            // leave committed false so cancelIfNotCommitted()
            // cleans up and restores the prior selection state
            return;
        }

        if (comp.hasSelection()) {
            commitWithExistingSelection(draftSelection, newShape);
        } else {
            commitAsFirstSelection(draftSelection, newShape);
        }

        committed = true;
    }

    private void commitWithExistingSelection(Selection draftSelection,
                                             Shape newShape) {
        Selection origSelection = comp.getSelection();
        Shape origShape = origSelection.getShape();
        Shape combinedShape = combinator.combine(origShape, newShape);

        if (combinedShape.getBounds().isEmpty()) {
            commitEmptyCombination(draftSelection, origShape);
        } else {
            commitNonEmptyCombination(draftSelection, combinedShape, origShape);
        }
    }

    private void commitEmptyCombination(Selection draftSelection,
                                        Shape origShape) {
        // restore the original shape here so that the undo edit
        // in deselect(true) captures the correct backup
        draftSelection.setShape(origShape);

        comp.promoteSelection();
        comp.deselect(true);

        Messages.showInfo("Nothing Selected",
            "As a result of the "
                + combinator.toString().toLowerCase(Locale.ENGLISH)
                + " operation, nothing is selected now.",
            comp.getDialogParent());
    }

    private void commitNonEmptyCombination(Selection draftSelection,
                                           Shape combinedShape,
                                           Shape origShape) {
        draftSelection.setShape(combinedShape);
        comp.promoteSelection();

        History.add(new SelectionShapeChangeEdit(
            combinator.getHistoryName(), comp, origShape));
    }

    private void commitAsFirstSelection(Selection draftSelection,
                                        Shape newShape) {
        // we can get here if either (1) a new selection
        // was created or (2) a selection was replaced
        if (newShape.getBounds().isEmpty()) {
            // the new shape can be empty if it has width or height = 0
            comp.deselect(false);
            return;
        }

        draftSelection.setShape(newShape);
        comp.promoteSelection();

        PixelitorEdit edit = (origSelShape != null)
            ? new SelectionShapeChangeEdit(combinator.getHistoryName(), comp, origSelShape)
            : new NewSelectionEdit(comp, newShape);
        History.add(edit);
    }

    /**
     * The rollback step; safe to call unconditionally as cleanup.
     */
    public void cancelIfNotCommitted() {
        if (committed) {
            // after a commit the draft has become the real selection,
            // so there is nothing to discard or restore
            return;
        }

        // clean up the temporary draft selection
        Selection draftSelection = comp.getDraftSelection();
        if (draftSelection != null) {
            draftSelection.dispose();
            comp.setDraftSelection(null);
        }

        // restore original selection display state
        var selection = comp.getSelection();
        if (selection != null) {
            selection.setFrozen(wasFrozen);
            selection.setHidden(wasHidden);
        }
    }
}
