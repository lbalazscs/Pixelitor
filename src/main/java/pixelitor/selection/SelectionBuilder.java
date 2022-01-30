/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Messages;

import java.awt.Rectangle;
import java.awt.Shape;

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

        comp.setBuiltSelection(new Selection(null, comp.getView()));

        if (combinator == ShapeCombinator.REPLACE) {
            replacedShape = existingSelection.getShape();
            // At this point the mouse was pressed, and it is clear that the
            // old selection should go away, but we don't know yet whether the
            // mouse will be released at the same point (Deselect) or another
            // point (Replace Selection)
            // Therefore we don't deselect yet (the selection information
            // will be needed when the mouse will be released), only hide.
            existingSelection.setHidden(true, true);
        } else {
            existingSelection.setFrozen(true);
        }
    }

    /**
     * As the mouse is dragged or released, the current
     * built selection shape is continuously updated
     */
    public void updateBuiltSelection(Object mouseInfo, Composition comp) {
        Selection builtSelection = comp.getBuiltSelection();
        boolean noPreviousSelection = builtSelection == null;

        if (noPreviousSelection) {
            Shape newShape = selectionType.createShape(mouseInfo, null);
            builtSelection = new Selection(newShape, comp.getView());
            comp.setBuiltSelection(builtSelection);
        } else {
            assert builtSelection.isAlive() : "dead selection";

            Shape shape = builtSelection.getShape();
            Shape newShape = selectionType.createShape(mouseInfo, shape);
            builtSelection.setShape(newShape);
            if (!builtSelection.isMarching()) {
                builtSelection.startMarching();
            }
        }
    }

    /**
     * The mouse has been released and the currently drawn shape must be combined
     * with the already existing shape according to the selection interaction type
     */
    public void combineShapes(Composition comp) {
        Selection oldSelection = comp.getSelection();
        Selection builtSelection = comp.getBuiltSelection();

        Shape newShape = builtSelection.getShape();
        newShape = comp.clipToCanvasBounds(newShape);
        if (newShape.getBounds2D().isEmpty()) {
            return;
        }

        if (oldSelection != null) { // needs to combine the shapes
            Shape oldShape = oldSelection.getShape();
            Shape combinedShape = combinator.combine(oldShape, newShape);

            Rectangle newBounds = combinedShape.getBounds();

            if (newBounds.isEmpty()) { // nothing after combine
                builtSelection.setShape(oldShape); // for the correct deselect undo
                oldSelection.die();
                comp.promoteSelection();
                comp.deselect(true);

                Messages.showInfo("Nothing selected", "As a result of the "
                                                      + combinator.toString().toLowerCase()
                                                      + " operation, nothing is selected now.", comp.getView());
            } else {
                oldSelection.die();
                builtSelection.setShape(combinedShape);
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
                builtSelection.setShape(newShape);
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
            Selection builtSelection = comp.getBuiltSelection();
            if (builtSelection != null) {
                builtSelection.die();
                comp.setBuiltSelection(null);
            }

            // if we had a frozen selection, unfreeze
            var selection = comp.getSelection();
            if (selection != null && selection.isFrozen()) {
                selection.setFrozen(false);
            }
        }
    }
}
