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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.NewSelectionEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.menus.SelectionActions;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.util.Optional;

import static pixelitor.selection.Selection.State.NO_SHAPE_YET;

/**
 * The selection tool
 */
public class SelectionTool extends Tool {
    private JComboBox<SelectionType> typeCombo;
    private JComboBox<SelectionInteraction> interactionCombo;

    private boolean altMeansSubtract = false;
    private SelectionInteraction originalSelectionInteraction;

    private Shape backupShape = null;

    SelectionTool() {
        super('m', "Selection", "selection_tool_icon.png",
                "Click and drag to select an area. Hold SPACE down to move the entire selection.",
                Cursor.getDefaultCursor(), false, true, false, ClipStrategy.FULL_AREA);
        spaceDragBehavior = true;
    }

    @Override
    public void initSettingsPanel() {
        typeCombo = new JComboBox<>(SelectionType.values());
        settingsPanel.addWithLabel("Type:", typeCombo, "selectionTypeCombo");

        settingsPanel.addSeparator();

        interactionCombo = new JComboBox<>(SelectionInteraction.values());
        settingsPanel.addWithLabel("New Selection:", interactionCombo, "selectionInteractionCombo");

        settingsPanel.addSeparator();

        settingsPanel.addButton(SelectionActions.getTraceWithBrush());

        settingsPanel.addButton(SelectionActions.getTraceWithEraser());

        settingsPanel.addButton(SelectionActions.getCropAction());
    }

    @Override
    public void mousePressed(MouseEvent e, ImageComponent ic) {
        boolean shiftDown = e.isShiftDown();
        boolean altDown = e.isAltDown();

        altMeansSubtract = altDown;

        if (shiftDown || altDown) {
            originalSelectionInteraction = (SelectionInteraction) interactionCombo.getSelectedItem();
            if (shiftDown) {
                if (altDown) {
                    interactionCombo.setSelectedItem(SelectionInteraction.INTERSECT);
                } else {
                    interactionCombo.setSelectedItem(SelectionInteraction.ADD);
                }
            } else if (altDown) {
                interactionCombo.setSelectedItem(SelectionInteraction.SUBTRACT);
            }
        }

        SelectionType selectionType = (SelectionType) typeCombo.getSelectedItem();
        SelectionInteraction selectionInteraction = (SelectionInteraction) interactionCombo.getSelectedItem();

        Composition comp = ic.getComp();
        Optional<Selection> optionalSelection = comp.getSelection();

        if (optionalSelection.isPresent()) {
            Selection selection = optionalSelection.get();
            if (selection.getState() == NO_SHAPE_YET) { // TODO it did happen on Mac during robot tests
                noSelectionYet(selectionType, selectionInteraction, comp);
            }
            backupShape = selection.getShape();
            selection.startNewShape(selectionType, selectionInteraction);
        } else {
            noSelectionYet(selectionType, selectionInteraction, comp);
        }
    }

    private void noSelectionYet(SelectionType selectionType, SelectionInteraction selectionInteraction, Composition comp) {
        backupShape = null;
        comp.startSelection(selectionType, selectionInteraction);
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        Composition comp = ic.getComp();
        Optional<Selection> selection = comp.getSelection();

        boolean altDown = e.isAltDown();
        boolean startFromCenter = (!altMeansSubtract) && altDown;
        if (!altDown) {
            altMeansSubtract = false;
        }

        userDrag.setStartFromCenter(startFromCenter);
        selection.get().updateSelection(userDrag);
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        if (userDrag.isClick()) { // will be handled by mouseClicked
            return;
        }

        Composition comp = ic.getComp();
        Optional<Selection> opt = comp.getSelection();
        if (!opt.isPresent()) {
            // TODO this should not happen, but it did happen on Mac
            // RandomGUITest
            System.out.println("SelectionTool::mouseReleased: no selection");
            return;
        }
        Selection selection = opt.get();

        if (originalSelectionInteraction != null) {
            interactionCombo.setSelectedItem(originalSelectionInteraction);
            originalSelectionInteraction = null;
        }

        boolean startFromCenter = (!altMeansSubtract) && e.isAltDown();

        userDrag.setStartFromCenter(startFromCenter);
        selection.updateSelection(userDrag);

        boolean stillSomethingSelected = selection.combineShapes();

        PixelitorEdit edit = null;

        if (stillSomethingSelected) {
            SelectionInteraction selectionInteraction = selection.getSelectionInteraction();
            boolean somethingSelectedAfterClipping = selection.clipToCompSize(comp);
            if (somethingSelectedAfterClipping) {
                if (newSelectionStarted()) {
                    edit = new NewSelectionEdit(comp, selection.getShape());
                } else {
                    edit = new SelectionChangeEdit(comp, backupShape, selectionInteraction.getNameForUndo());
                }
            } else { // the selection is outside the composition bounds
                deselect(ic, AddToHistory.NO); // don't create a DeselectEdit because the backup shape could be null
                if (!newSelectionStarted()) { // backupShape != null
                    // create a special DeselectEdit with the backupShape
                    edit = new DeselectEdit(ic.getComp(), backupShape, "SelectionTool.mouseReleased 1");
                    assert !comp.hasSelection();
                }
            }
        } else {
            // special case: it started like a selection change but nothing is selected now
            // we also get here if the selection is a single line (area = 0), but then backupShape is null
            deselect(ic, AddToHistory.NO); // don't create a DeselectEdit because the backup shape could be null
            if (!newSelectionStarted()) { // backupShape != null
                // create a special DeselectEdit with the backupShape
                edit = new DeselectEdit(ic.getComp(), backupShape, "SelectionTool.mouseReleased 2");
                assert !comp.hasSelection();
            }
        }

        if (edit != null) {
            History.addEdit(edit);
        }

        altMeansSubtract = false;
    }

    @Override
    public boolean dispatchMouseClicked(MouseEvent e, ImageComponent ic) {
        super.dispatchMouseClicked(e, ic);

//        if(typeCombo.getSelectedItem() == SelectionType.POLYGONAL_LASSO) {
//            addPolygonalLassoPoint(ic);
//
//            return false;
//        }

        deselect(ic, AddToHistory.YES);

        altMeansSubtract = false;

        return false;
    }

    private void addPolygonalLassoPoint(ImageComponent ic) {
        Composition comp = ic.getComp();
        Optional<Selection> selection = comp.getSelection();
        if (selection.isPresent()) {
            selection.get().addNewPolygonalLassoPoint(userDrag);
        }
    }

    private static void deselect(ImageComponent ic, AddToHistory addToHistory) {
        Composition comp = ic.getComp();

        if (comp.hasSelection()) {
            comp.deselect(addToHistory);
        }
    }

    private boolean newSelectionStarted() {
        return backupShape == null;
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        ImageComponent ic = ImageComponents.getActiveIC();
        if (ic != null) {
            Composition comp = ic.getComp();
            Optional<Selection> selection = comp.getSelection();
            if (selection.isPresent()) {
                selection.get().nudge(key.getTransform());
                return true;
            }
        }
        return false;
    }
}
