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

package pixelitor.selection;

import pixelitor.Composition;
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionShapeChangeEdit;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Messages;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Locale;

/**
 * A utility class for creating selections
 */
public class SelectionBuilder {
    private final SelectionType selectionType;
    private final ShapeCombinator combinator;

    private Shape replacedShape;

    private boolean finished = false;

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

        assert existingSelection.isAlive() : "dead selection";

        comp.setInProgressSelection(new Selection(null, comp.getView()));

        if (combinator == ShapeCombinator.REPLACE) {
            replacedShape = existingSelection.getShape();
            // At this point the mouse was pressed, and it's clear that the
            // old selection should go away, but we don't know yet whether the
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
     * As the mouse is dragged or released, the current
     * in-progress-selection shape is continuously updated
     */
    public void updateInProgressSelection(Object mouseInfo, Composition comp, PMouseEvent pm) {
        Selection inProgressSelection = comp.getInProgressSelection();
        boolean noPreviousSelection = inProgressSelection == null;
        Shape newShape;

        if (noPreviousSelection) {
            if (selectionType == SelectionType.SELECTION_MAGIC_WAND) {
                newShape = selectionType.createShape(pm, null);
            } else {
                newShape = selectionType.createShape(mouseInfo, null);
            }
            inProgressSelection = new Selection(newShape, comp.getView());
            comp.setInProgressSelection(inProgressSelection);
        } else {
            assert inProgressSelection.isAlive() : "dead selection";

            Shape shape = inProgressSelection.getShape();
            if (selectionType == SelectionType.SELECTION_MAGIC_WAND) {
                newShape = selectionType.createShape(pm, shape);
            } else {
                newShape = selectionType.createShape(mouseInfo, shape);
            }
            inProgressSelection.setShape(newShape);
            if (!inProgressSelection.isMarching()) {
                inProgressSelection.startMarching();
            }
        }
    }

    public void updateInProgressSelection(PMouseEvent pm, Composition comp) {
        updateInProgressSelection(pm, comp, pm);
    }

    /**
     * The mouse has been released and the currently drawn shape must be combined
     * with the already existing shape according to the selection interaction type
     */
    public void combineShapes(Composition comp) {
        Selection oldSelection = comp.getSelection();
        Selection inProgressSelection = comp.getInProgressSelection();

        Shape newShape = inProgressSelection.getShape();
        newShape = comp.clipToCanvasBounds(newShape);
        if (newShape.getBounds2D().isEmpty()) {
            return;
        }

        if (oldSelection != null) { // needs to combine the shapes
            Shape oldShape = oldSelection.getShape();
            Shape combinedShape = combinator.combine(oldShape, newShape);

            Rectangle newBounds = combinedShape.getBounds();

            if (newBounds.isEmpty()) { // nothing after combine
                inProgressSelection.setShape(oldShape); // for the correct deselect undo
                oldSelection.die();
                comp.promoteSelection();
                comp.deselect(true);

                String msg = "As a result of the "
                    + combinator.toString().toLowerCase(Locale.ENGLISH)
                    + " operation, nothing is selected now.";
                Messages.showInfo("Nothing selected", msg, comp.getDialogParent());
            } else {
                oldSelection.die();
                inProgressSelection.setShape(combinedShape);
                comp.promoteSelection();

                History.add(new SelectionShapeChangeEdit(
                    combinator.getNameForUndo(), comp, oldShape));
            }
        } else {
            // we can get here if either (1) a new selection
            // was created or (2) a selection was replaced
            if (newShape.getBounds().isEmpty()) {
                // the new shape can be empty if it has width or height = 0
                comp.deselect(false);
            } else {
                inProgressSelection.setShape(newShape);
                comp.promoteSelection();

                PixelitorEdit edit;
                if (replacedShape != null) {
                    edit = new SelectionShapeChangeEdit(combinator.getNameForUndo(), comp, replacedShape);
                } else {
                    edit = new NewSelectionEdit(comp, newShape);
                }
                History.add(edit);
            }
        }

        finished = true;
    }

    public void cancelIfNotFinished(Composition comp) {
        if (!finished) {
            Selection inProgressSelection = comp.getInProgressSelection();
            if (inProgressSelection != null) {
                inProgressSelection.die();
                comp.setInProgressSelection(null);
            }

            // if we had a frozen selection, unfreeze
            var selection = comp.getSelection();
            if (selection != null && selection.isFrozen()) {
                selection.setFrozen(false);
            }
        }
    }
}
