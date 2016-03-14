/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.utils.Messages;
import pixelitor.utils.test.RandomGUITest;

import java.awt.Rectangle;
import java.awt.Shape;

public class SelectionBuilder {
    private final SelectionType selectionType;
    private final SelectionInteraction selectionInteraction;
    private final Composition comp;

    private Shape replacedShape;

    private boolean finished = false;

    /**
     * Called in mousePressed
     */
    public SelectionBuilder(SelectionType selectionType, SelectionInteraction selectionInteraction, Composition comp) {
        this.selectionInteraction = selectionInteraction;
        this.selectionType = selectionType;
        this.comp = comp;
        Selection selection = comp.getSelection();

        if (selection == null) {
            return;
        }

        startNewShape(selectionInteraction, selection);
    }

    /**
     * Called if there is already a selection in mousePressed
     */
    private void startNewShape(SelectionInteraction selectionInteraction, Selection selection) {
        assert selection.isAlive() : "dead selection";

        comp.setBuiltSelection(new Selection(null, comp.getIC()));

        if (selectionInteraction == SelectionInteraction.REPLACE) {
            replacedShape = selection.getShape();
//            selection.stopMarching();
//            selection.repaint();
            comp.setSelection(null);
            selection.die();
        } else {
//            // the current shape becomes the previous shape
//            // and will be replaced as mouse dragged events come
//            selection.setLastShape(selection.getShape());

            selection.setFrozen(true);

//            selection.setHidden(false, false); // unhide
        }
    }

    /**
     * As the mouse is dragged or released, the current
     * selection shape is continuously updated
     */
    public void updateSelection(Object mouseInfo) {
        Selection builtSelection = comp.getBuiltSelection();
        boolean noPreviousSelection = builtSelection == null;

        if (noPreviousSelection) {
            Shape newShape = selectionType.createShape(mouseInfo, null);
            builtSelection = new Selection(newShape, comp.getIC());
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
    public void combineShapes() {
        Selection oldSelection = comp.getSelection();
        Selection builtSelection = comp.getBuiltSelection();

        Shape newShape = builtSelection.getShape();
        newShape = comp.clipShapeToCanvasSize(newShape);

        if (oldSelection != null) { // needs to combine the shapes
            Shape oldShape = oldSelection.getShape();
            Shape combinedShape = selectionInteraction.combine(oldShape, newShape);

            Rectangle newBounds = combinedShape.getBounds();

            if (newBounds.isEmpty()) { // nothing after combine
                builtSelection.setShape(oldShape); // for the correct deselect undo
                oldSelection.die();
                comp.promoteSelection();
                comp.deselect(AddToHistory.YES);

                if (!RandomGUITest.isRunning()) {
                    Messages.showInfo("Nothing selected", "As a result of the "
                            + selectionInteraction.toString().toLowerCase() + " operation, nothing is selected now.");
                }
            } else {
                oldSelection.die();
                builtSelection.setShape(combinedShape);
                comp.promoteSelection();

                PixelitorEdit edit = new SelectionChangeEdit(comp, oldShape, selectionInteraction.getNameForUndo());
                History.addEdit(edit);
            }
        } else {
            // we can get here if either (1) a new selection
            // was created or (2) a selection was replaced

            if (newShape.getBounds().isEmpty()) {
                // the new shape can be empty if it has width or height = 0
                comp.deselect(AddToHistory.NO);
                oldSelection = null;
            } else {
                builtSelection.setShape(newShape);
                comp.promoteSelection();

                PixelitorEdit edit;
                if (replacedShape != null) {
                    edit = new SelectionChangeEdit(comp, replacedShape, selectionInteraction.getNameForUndo());
                } else {
                    edit = new NewSelectionEdit(comp, builtSelection.getShape());
                }
                History.addEdit(edit);
            }
        }

        finished = true;
    }

    public void cancelIfNotFinished() {
        if(!finished) {
            Selection builtSelection = comp.getBuiltSelection();
            if (builtSelection != null) {
                builtSelection.die();
                comp.setBuiltSelection(null);
            }

            // if we had a frozen selection, unfreeze
            Selection selection = comp.getSelection();
            if (selection != null && selection.isFrozen()) {
                selection.setFrozen(false);
            }
        }
    }
}
