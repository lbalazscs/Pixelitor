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
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

/**
 * The selection tool
 */
public class SelectionTool extends Tool implements ImageSwitchListener {
    public static final String HELP_TEXT = "Click and drag to select an area. Hold SPACE down to move the entire selection. Shift-drag adds to an existing selection, Alt-drag removes from it, Shift+Alt intersects.";
    public static final String POLY_HELP_TEXT = "Polygonal selection: Click to add points, double-click (or right-click) to finish the selection.";

    private JComboBox<SelectionType> typeCombo;
    private JComboBox<SelectionInteraction> interactionCombo;

    private boolean altMeansSubtract = false;
    private SelectionInteraction originalSelectionInteraction;

    private SelectionBuilder selectionBuilder;
    private boolean polygonal = false;

    SelectionTool() {
        super('m', "Selection", "selection_tool_icon.png",
                HELP_TEXT,
                Cursor.getDefaultCursor(), false, true, false, ClipStrategy.INTERNAL_FRAME);
        spaceDragBehavior = true;
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void initSettingsPanel() {
        typeCombo = new JComboBox<>(SelectionType.values());
        typeCombo.addActionListener(e -> {
            stopBuildingSelection();
            polygonal = typeCombo.getSelectedItem() == SelectionType.POLYGONAL_LASSO;
            if (polygonal) {
                Messages.showStatusMessage(POLY_HELP_TEXT);
            } else {
                Messages.showStatusMessage("Selection Tool: " + HELP_TEXT);
            }
        });
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
        if (polygonal) {
            return; // ignore mouse pressed
        }

        setupInteractionWithKeyModifiers(e);

        SelectionType selectionType = (SelectionType) typeCombo.getSelectedItem();
        SelectionInteraction selectionInteraction = (SelectionInteraction) interactionCombo.getSelectedItem();
        Composition comp = ic.getComp();
        selectionBuilder = new SelectionBuilder(selectionType, selectionInteraction, comp);
    }

    @Override
    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        if (polygonal) {
            return; // ignore mouseDragged
        }

        boolean altDown = e.isAltDown();
        boolean startFromCenter = (!altMeansSubtract) && altDown;
        if (!altDown) {
            altMeansSubtract = false;
        }

        userDrag.setStartFromCenter(startFromCenter);

        selectionBuilder.updateSelection(userDrag);
    }

    @Override
    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        // TODO
        if (userDrag.isClick() && !polygonal) { // will be handled by mouseClicked
            return;
        }

        Composition comp = ic.getComp();
        Selection selection = comp.getBuiltSelection();
        if (selection == null && !polygonal) {
            System.err.println("SelectionTool::mouseReleased: no built selection");
            return;
        }

        if (polygonal) {
            PMouseEvent pe = new PMouseEvent(e, ic);

            if (selectionBuilder == null) {
                setupInteractionWithKeyModifiers(e);
                SelectionType selectionType = (SelectionType) typeCombo.getSelectedItem();
                SelectionInteraction selectionInteraction = (SelectionInteraction) interactionCombo.getSelectedItem();
                selectionBuilder = new SelectionBuilder(selectionType, selectionInteraction, comp);
                selectionBuilder.updateSelection(pe);
                restoreInteraction();
            } else {
                selectionBuilder.updateSelection(pe);
                if (SwingUtilities.isRightMouseButton(e)) {
                    selectionBuilder.combineShapes();
                    stopBuildingSelection();
                }
            }
        } else {
            restoreInteraction();

            boolean startFromCenter = (!altMeansSubtract) && e.isAltDown();
            userDrag.setStartFromCenter(startFromCenter);

            selectionBuilder.updateSelection(userDrag);
            selectionBuilder.combineShapes();
            stopBuildingSelection();
        }

        altMeansSubtract = false;
    }

    @Override
    public boolean dispatchMouseClicked(MouseEvent e, ImageComponent ic) {
        if (polygonal) {
            if (selectionBuilder != null && e.getClickCount() > 1) {
                // finish polygonal for double-click
                PMouseEvent pe = new PMouseEvent(e, ic);
                selectionBuilder.updateSelection(pe);
                selectionBuilder.combineShapes();
                stopBuildingSelection();
                return false;
            } else {
                // ignore otherwise: will be handled in mouse released
                return false;
            }
        }

        super.dispatchMouseClicked(e, ic);

        deselect(ic, AddToHistory.YES);

        altMeansSubtract = false;

        return false;
    }

    private static void deselect(ImageComponent ic, AddToHistory addToHistory) {
        Composition comp = ic.getComp();

        if (comp.hasSelection()) {
            comp.deselect(addToHistory);
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        ImageComponent ic = ImageComponents.getActiveIC();
        if (ic != null) {
            Composition comp = ic.getComp();
            Selection selection = comp.getSelection();
            if (selection != null) {
                selection.nudge(key.getTransform());
                return true;
            }
        }
        return false;
    }

    private void setupInteractionWithKeyModifiers(MouseEvent e) {
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
    }

    private void restoreInteraction() {
        if (originalSelectionInteraction != null) {
            interactionCombo.setSelectedItem(originalSelectionInteraction);
            originalSelectionInteraction = null;
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        stopBuildingSelection();
    }

    @Override
    public void noOpenImageAnymore() {
        // ignore
    }

    @Override
    public void newImageOpened(Composition comp) {
        stopBuildingSelection();
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        stopBuildingSelection();
    }

    private void stopBuildingSelection() {
        if (selectionBuilder != null) {
            selectionBuilder.cancelIfNotFinished();
            selectionBuilder = null;
        }
    }

    @Override
    public String getStateInfo() {
        Object type = typeCombo.getSelectedItem();
        Object interaction = interactionCombo.getSelectedItem();

        return "type = " + type + ", interaction = " + interaction;
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addStringChild("Type", typeCombo.getSelectedItem().toString());
        node.addStringChild("Interaction", interactionCombo.getSelectedItem().toString());

        return node;
    }
}
