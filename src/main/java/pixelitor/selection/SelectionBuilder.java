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
    private Selection selection;

    private Shape replacedShape;

    /**
     * Called in mousePressed
     */
    public SelectionBuilder(SelectionType selectionType, SelectionInteraction selectionInteraction, Composition comp) {
        this.selectionInteraction = selectionInteraction;
        this.selectionType = selectionType;
        this.comp = comp;
        this.selection = comp.getSelectionOrNull();

        if (selection == null) {
            return;
        }

        startNewShape(selectionInteraction);
    }

    /**
     * Called if there is already a selection in mousePressed
     */
    private void startNewShape(SelectionInteraction selectionInteraction) {
        assert selection.isAlive() : "dead selection";

        if (selectionInteraction == SelectionInteraction.REPLACE) {
            replacedShape = selection.getShape();
            selection.stopMarching();
            selection.setNewShape(null);
            selection.repaint();
        } else {
            // the current shape becomes the previous shape
            // and will be replaced as mouse dragged events come
            selection.setLastShape(selection.getShape());
        }

        selection.setHidden(false, false);
    }

    /**
     * As the mouse is dragged or released, the current
     * selection shape is continuously updated
     */
    public void updateSelection(Object mouseInfo) {
        boolean noPreviousSelection = selection == null;

        if (noPreviousSelection) {
            Shape newShape = selectionType.createShape(mouseInfo, null);
            selection = new Selection(newShape, comp.getIC());
            comp.setNewSelection(selection);
        } else {
            Shape shape = selection.getShape();
            Shape newShape = selectionType.createShape(mouseInfo, shape);
            selection.setShape(newShape);
            if (!selection.isMarching()) {
                selection.startMarching();
            }
        }
    }

    /**
     * The mouse has been released and the currently drawn shape must be combined
     * with the already existing shape according to the selection interaction type
     */
    public void combineShapes() {
        Shape lastShape = selection.getLastShape();
        Shape shape = selection.getShape();
        shape = comp.clipShapeToCanvasSize(shape);

        if (lastShape != null) { // needs to combine the shapes
            Shape combinedShape = selectionInteraction.combine(lastShape, shape);

            Rectangle newBounds = combinedShape.getBounds();

            if (newBounds.isEmpty()) { // nothing after combine
                selection.setShape(lastShape); // for the correct deselect undo
                comp.deselect(AddToHistory.YES);

                if (!RandomGUITest.isRunning()) {
                    Messages.showInfo("Nothing selected", "As a result of the "
                            + selectionInteraction.toString().toLowerCase() + " operation, nothing is selected now.");
                }
                return;
            } else {
                selection.setNewShape(combinedShape);
                PixelitorEdit edit = new SelectionChangeEdit(comp, lastShape, selectionInteraction.getNameForUndo());
                History.addEdit(edit);
                return;
            }
        } else {
            // we can get here if either (1) a new selection
            // was created or (2) a selection was replaced

            if (shape.getBounds().isEmpty()) {
                // the new shape can be empty if it has width or height = 0
                comp.deselect(AddToHistory.NO);
                return;
            } else {
                selection.setShape(shape);

                PixelitorEdit edit;
                if (replacedShape != null) {
                    edit = new SelectionChangeEdit(comp, replacedShape, selectionInteraction.getNameForUndo());
                } else {
                    edit = new NewSelectionEdit(comp, selection.getShape());
                }
                History.addEdit(edit);
                return;
            }
        }
    }
}
