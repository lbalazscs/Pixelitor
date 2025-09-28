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
 * Manages the interactive creation and modification of a selection shape.
 */
public class SelectionBuilder {
    private final SelectionType selectionType;
    private final ShapeCombinator combinator;
    private Composition comp;

    private Shape prevSelShape;

    private boolean complete = false;

    public SelectionBuilder(SelectionType selectionType, ShapeCombinator combinator, Composition comp) {
        this.combinator = combinator;
        this.selectionType = selectionType;
        this.comp = comp;

        Selection existingSelection = comp.getSelection();
        if (existingSelection == null) {
            // if there is no existing selection, we don't use a draft selection
            return;
        }

        assert existingSelection.isUsable() : "disposed selection";

        // the shape itself will be set in updateDraftSelection
        comp.setDraftSelection(new Selection(null, comp.getView()));

        if (combinator == ShapeCombinator.REPLACE) {
            prevSelShape = existingSelection.getShape();
            // At this point the mouse was pressed, and it's clear that the
            // existing selection should go away, but we don't know yet whether the
            // mouse will be released at the same point (Deselect) or another
            // point (Replace Selection).
            // Therefore, we don't deselect yet (the selection information
            // will be needed when the mouse will be released), only hide.
            existingSelection.setHidden(true);
        } else {
            existingSelection.setFrozen(true);
        }
    }

    /**
     * Updates the draft selection shape based on drag information.
     */
    public void updateDraftSelection(Drag drag) {
        Selection draftSelection = comp.getDraftSelection();

        if (draftSelection == null) {
            createNewDraftSelectionFromDrag(drag);
        } else {
            assert draftSelection.isUsable() : "disposed draft selection";
            updateExistingDraftSelectionFromDrag(draftSelection, drag);
        }
    }

    /**
     * Updates the draft selection shape based on a mouse event.
     */
    public void updateDraftSelection(PMouseEvent mouseEvent) {
        // update the composition reference, because in a polygonal lasso
        // selection session an undo of a previous CompAction could change it
        // (possibly it would be better to store the view in this class)
        comp = mouseEvent.getComp();

        Selection draftSelection = comp.getDraftSelection();

        if (draftSelection == null) {
            createNewDraftSelectionFromEvent(mouseEvent);
        } else {
            assert draftSelection.isUsable() : "disposed draft selection";
            updateExistingDraftSelectionFromEvent(draftSelection, mouseEvent);
        }
    }

    private void createNewDraftSelectionFromDrag(Drag drag) {
        Shape newShape = selectionType.createShapeFromDrag(drag, null);
        comp.setDraftSelection(new Selection(newShape, comp.getView()));
    }

    private void updateExistingDraftSelectionFromDrag(Selection draftSelection, Drag drag) {
        Shape currentShape = draftSelection.getShape();
        Shape newShape = selectionType.createShapeFromDrag(drag, currentShape);
        updateDraftInternal(draftSelection, newShape);
    }

    private void createNewDraftSelectionFromEvent(PMouseEvent pm) {
        assert pm != null;
        Shape newShape = selectionType.createShapeFromEvent(pm, null);
        comp.setDraftSelection(new Selection(newShape, comp.getView()));
    }

    private void updateExistingDraftSelectionFromEvent(Selection draftSelection, PMouseEvent pm) {
        Shape currentShape = draftSelection.getShape();
        Shape newShape = selectionType.createShapeFromEvent(pm, currentShape);
        updateDraftInternal(draftSelection, newShape);
    }

    private static void updateDraftInternal(Selection draftSelection, Shape newShape) {
        draftSelection.setShape(newShape);

        if (!draftSelection.isMarching()) {
            draftSelection.startMarching();
        }
    }

    /**
     * Finalizes the selection by combining the draft shape with
     * any existing selection according to the combination mode.
     */
    public void combineShapes() {
        Selection draftSelection = comp.getDraftSelection();

        Shape newShape = draftSelection.getShape();
        newShape = comp.clipToCanvasBounds(newShape);
        if (newShape.getBounds2D().isEmpty()) {
            return;
        }

        if (comp.hasSelection()) {
            combineWithExistingSelection(draftSelection, newShape);
        } else {
            finalizeNewSelection(draftSelection, newShape);
        }

        complete = true;
    }

    private void combineWithExistingSelection(Selection draftSelection,
                                              Shape newShape) {
        Selection origSelection = comp.getSelection();
        Shape origShape = origSelection.getShape();
        Shape combinedShape = combinator.combine(origShape, newShape);

        if (combinedShape.getBounds().isEmpty()) { // nothing after combine
            handleEmptyCombinedShape(origSelection, draftSelection, origShape);
        } else {
            finalizeShapeCombination(origSelection, draftSelection, combinedShape, origShape);
        }
    }

    private void handleEmptyCombinedShape(Selection origSelection,
                                          Selection draftSelection,
                                          Shape origShape) {
        draftSelection.setShape(origShape); // for the correct deselect undo
        origSelection.dispose();
        comp.promoteSelection();
        comp.deselect(true);

        Messages.showInfo("Nothing Selected",
            "As a result of the "
                + combinator.toString().toLowerCase(Locale.ENGLISH)
                + " operation, nothing is selected now.",
            comp.getDialogParent());
    }

    private void finalizeShapeCombination(Selection origSelection,
                                          Selection draftSelection,
                                          Shape combinedShape,
                                          Shape origShape) {
        draftSelection.setShape(combinedShape);
        origSelection.dispose();
        comp.promoteSelection();

        History.add(new SelectionShapeChangeEdit(
            combinator.getHistoryName(), comp, origShape));
    }

    private void finalizeNewSelection(Selection draftSelection,
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

        PixelitorEdit edit = (prevSelShape != null)
            ? new SelectionShapeChangeEdit(combinator.getHistoryName(), comp, prevSelShape)
            : new NewSelectionEdit(comp, newShape);
        History.add(edit);
    }

    /**
     * Cancels the selection building process if it hasn't been completed.
     */
    public void cancelIfNotFinished() {
        if (complete) {
            return;
        }

        Selection draftSelection = comp.getDraftSelection();
        if (draftSelection != null) {
            draftSelection.dispose();
            comp.setDraftSelection(null);
        }

        // if we had a frozen selection, unfreeze
        var selection = comp.getSelection();
        if (selection != null && selection.isFrozen()) {
            selection.setFrozen(false);
        }
    }
}
