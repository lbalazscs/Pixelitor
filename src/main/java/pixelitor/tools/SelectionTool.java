/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
import pixelitor.ImageDisplay;
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
        super('m', "Selection", "selection_tool_icon.gif",
                "Click and drag to select an area. Hold SPACE down to move the entire selection.",
                Cursor.getDefaultCursor(), false, true, false, ClipStrategy.FULL_AREA);
        spaceDragBehavior = true;
    }

    @Override
    public void initSettingsPanel() {
        toolSettingsPanel.add(new JLabel("Type:"));
        typeCombo = new JComboBox<>(SelectionType.values());
        typeCombo.setName("selectionTypeCombo");
        toolSettingsPanel.add(typeCombo);

        toolSettingsPanel.addSeparator();

        toolSettingsPanel.add(new JLabel("New Selection:"));
        interactionCombo = new JComboBox<>(SelectionInteraction.values());
        interactionCombo.setName("selectionInteractionCombo");
        toolSettingsPanel.add(interactionCombo);

        toolSettingsPanel.addSeparator();

        JButton brushTraceButton = new JButton(SelectionActions.getTraceWithBrush());
        brushTraceButton.setName("brushTraceButton");
        toolSettingsPanel.add(brushTraceButton);

        JButton eraserTraceButton = new JButton(SelectionActions.getTraceWithEraser());
        eraserTraceButton.setName("eraserTraceButton");
        toolSettingsPanel.add(eraserTraceButton);

        JButton cropButton = new JButton(SelectionActions.getCropAction());
        cropButton.setName("cropSelectionButton");
        toolSettingsPanel.add(cropButton);
    }

    @Override
    public void mousePressed(MouseEvent e, ImageDisplay ic) {
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
        Optional<Selection> selection = comp.getSelection();
        if (!selection.isPresent()) {
            backupShape = null;
            comp.startSelection(selectionType, selectionInteraction);
        } else {
            backupShape = selection.get().getShape();
            selection.get().startNewShape(selectionType, selectionInteraction);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageDisplay ic) {
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
    public void mouseReleased(MouseEvent e, ImageDisplay ic) {
        if (userDrag.isClick()) { // will be handled by mouseClicked
            return;
        }

        Composition comp = ic.getComp();
        Optional<Selection> opt = comp.getSelection();
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
                deselect(ic, false); // don't create a DeselectEdit because the backup shape could be null
                if (!newSelectionStarted()) { // backupShape != null
                    // create a special DeselectEdit with the backupShape
                    edit = new DeselectEdit(ic.getComp(), backupShape, "SelectionTool.mouseReleased 1");
                    assert !comp.hasSelection();
                }
            }
        } else {
            // special case: it started like a selection change but nothing is selected now
            // we also get here if the selection is a single line (area = 0), but then backupShape is null
            deselect(ic, false); // don't create a DeselectEdit because the backup shape could be null
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
    public boolean dispatchMouseClicked(MouseEvent e, ImageDisplay ic) {
        super.dispatchMouseClicked(e, ic);

//        if(typeCombo.getSelectedItem() == SelectionType.POLYGONAL_LASSO) {
//            addPolygonalLassoPoint(ic);
//
//            return false;
//        }

        deselect(ic, true);

        altMeansSubtract = false;

        return false;
    }

    private void addPolygonalLassoPoint(ImageDisplay ic) {
        Composition comp = ic.getComp();
        Optional<Selection> selection = comp.getSelection();
        if (selection.isPresent()) {
            selection.get().addNewPolygonalLassoPoint(userDrag);
        }
    }

    private static void deselect(ImageDisplay ic, boolean sendDeselectEdit) {
        Composition comp = ic.getComp();

        if (comp.hasSelection()) {
            comp.deselect(sendDeselectEdit);
        }
    }

    private boolean newSelectionStarted() {
        return backupShape == null;
    }

}
