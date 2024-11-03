/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Messages;

import java.awt.Shape;
import java.util.Locale;

/**
 * Manages the creation and modification of selections.
 * Handles the lifecycle of a selection (mouse events,
 * combining with an existing selection).
 */
public class SelectionBuilder {
    private final SelectionType selectionType;
    private final ShapeCombinator combinator;

    private Shape prevSelShape;

    private boolean complete = false;

    /**
     * Called in mousePressed (or mouseReleased for polygonal selection)
     */
    public SelectionBuilder(SelectionType selectionType, ShapeCombinator combinator, Composition comp) {
        this.combinator = combinator;
        this.selectionType = selectionType;

        Selection existingSelection = comp.getSelection();
        if (existingSelection == null) {
            return;
        }

        assert existingSelection.isUsable() : "disposed selection";

        comp.setDraftSelection(new Selection(null, comp.getView()));

        if (combinator == ShapeCombinator.REPLACE) {
            prevSelShape = existingSelection.getShape();
            // At this point the mouse was pressed, and it's clear that the
            // existing selection should go away, but we don't know yet whether the
            // mouse will be released at the same point (Deselect) or another
            // point (Replace Selection).
            // Therefore, we don't deselect yet (the selection information
            // will be needed when the mouse will be released), only hide.
            existingSelection.setHidden(true, true);
        } else {
            existingSelection.setFrozen(true);
        }
    }

    /**
     * Updates the draft selection shape based on current mouse position.
     */
    public void updateDraftSelection(Object mouseInfo, Composition comp, PMouseEvent mouseEvent) {
        Selection draftSelection = comp.getDraftSelection();

        if (draftSelection == null) {
            createNewDraftSelection(mouseInfo, comp, mouseEvent);
            return;
        }

        assert draftSelection.isUsable() : "disposed selection";
        updateExistingDraftSelection(draftSelection, mouseInfo, comp, mouseEvent);
    }

    private void createNewDraftSelection(Object mouseInfo, Composition comp, PMouseEvent pm) {
        Shape newShape = (selectionType == SelectionType.SELECTION_MAGIC_WAND)
            ? selectionType.createShape(pm, null)
            : selectionType.createShape(mouseInfo, null);
        comp.setDraftSelection(new Selection(newShape, comp.getView()));
    }

    private void updateExistingDraftSelection(Selection draftSelection,
                                              Object mouseInfo,
                                              Composition comp,
                                              PMouseEvent pm) {
        Shape currentShape = draftSelection.getShape();
        Shape newShape = (selectionType == SelectionType.SELECTION_MAGIC_WAND)
            ? updateMagicWandShape(draftSelection, comp, pm, currentShape)
            : selectionType.createShape(mouseInfo, currentShape);

        draftSelection.setShape(newShape);

        if (!draftSelection.isMarching()) {
            draftSelection.startMarching();
        }
    }

    private Shape updateMagicWandShape(Selection draftSelection, Composition comp, PMouseEvent pm, Shape currentShape) {
        Shape newShape = selectionType.createShape(pm, currentShape);

        if (comp.getDraftSelection() == null) {
            comp.setDraftSelection(new Selection(newShape, pm.getView()));
        }
        if (draftSelection.getView() == null) {
            draftSelection.setView(comp.getView());
        }

        return newShape;
    }

    /**
     * Convenience method for updating a selection with a mouse event.
     */
    public void updateDraftSelection(PMouseEvent mouseEvent, Composition comp) {
        updateDraftSelection(mouseEvent, comp, mouseEvent);
    }

    /**
     * Finalizes the selection by combining the draft shape with
     * any existing selection according to the combination mode.
     */
    public void combineShapes(Composition comp) {
        Selection origSelection = comp.getSelection();
        Selection draftSelection = comp.getDraftSelection();

        Shape newShape = draftSelection.getShape();
        newShape = comp.clipToCanvasBounds(newShape);
        if (newShape.getBounds2D().isEmpty()) {
            return;
        }

        if (origSelection != null) {
            combineWithExistingSelection(origSelection, draftSelection, newShape, comp);
        } else {
            finalizeNewSelection(draftSelection, newShape, comp);
        }

        complete = true;
    }

    private void combineWithExistingSelection(Selection origSelection,
                                              Selection draftSelection,
                                              Shape newShape,
                                              Composition comp) {
        Shape origShape = origSelection.getShape();
        Shape combinedShape = combinator.combine(origShape, newShape);

        if (combinedShape.getBounds().isEmpty()) { // nothing after combine
            handleEmptyCombinedShape(origSelection, draftSelection, origShape, comp);
        } else {
            finalizeShapeCombination(origSelection, draftSelection, combinedShape, origShape, comp);
        }
    }

    private void handleEmptyCombinedShape(Selection origSelection,
                                          Selection draftSelection,
                                          Shape origShape,
                                          Composition comp) {
        draftSelection.setShape(origShape); // for the correct deselect undo
        origSelection.dispose();
        comp.promoteSelection();
        comp.deselect(true);

        Messages.showInfo("Nothing selected",
            "As a result of the "
                + combinator.toString().toLowerCase(Locale.ENGLISH)
                + " operation, nothing is selected now.",
            comp.getDialogParent());
    }

    private void finalizeShapeCombination(Selection origSelection,
                                          Selection draftSelection,
                                          Shape combinedShape,
                                          Shape origShape,
                                          Composition comp) {
        origSelection.dispose();
        draftSelection.setShape(combinedShape);
        comp.promoteSelection();

        History.add(new SelectionShapeChangeEdit(
            combinator.getHistoryName(), comp, origShape));
    }

    private void finalizeNewSelection(Selection draftSelection,
                                      Shape newShape,
                                      Composition comp) {
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
    public void cancelIfNotFinished(Composition comp) {
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
