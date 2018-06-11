/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionInteraction;
import pixelitor.selection.SelectionType;
import pixelitor.utils.ActiveImageChangeListener;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * The selection tool
 */
public class SelectionTool extends Tool implements ActiveImageChangeListener {
    private static final String HELP_TEXT = "<b>click and drag</b> to select an area. Hold <b>SPACE</b> down to move the entire selection. <b>Shift-drag</b> adds to an existing selection, <b>Alt-drag</b> removes from it, <b>Shift+Alt drag</b> intersects.";
    private static final String POLY_HELP_TEXT = "<html>Polygonal selection: <b>click</b> to add points, <b>double-click</b> (or <b>right-click</b>) to finish the selection.";

    private JComboBox<SelectionType> typeCombo;
    private JComboBox<SelectionInteraction> interactionCombo;

    private boolean altMeansSubtract = false;
    private SelectionInteraction originalSelectionInteraction;

    private SelectionBuilder selectionBuilder;
    private boolean polygonal = false;

    SelectionTool() {
        super('m', "Selection", "selection_tool_icon.png",
                HELP_TEXT,
                Cursors.DEFAULT, false, true, false, ClipStrategy.INTERNAL_FRAME);
        spaceDragBehavior = true;
        ImageComponents.addActiveImageChangeListener(this);
    }

    @Override
    public void initSettingsPanel() {
        typeCombo = new JComboBox<>(SelectionType.values());
        typeCombo.addActionListener(e -> {
            stopBuildingSelection();
            polygonal = typeCombo.getSelectedItem() == SelectionType.POLYGONAL_LASSO;
            if (polygonal) {
                Messages.showInStatusBar(POLY_HELP_TEXT);
            } else {
                Messages.showInStatusBar("<html>Selection Tool: " + HELP_TEXT);
            }
        });
        settingsPanel.addWithLabel("Type:", typeCombo, "selectionTypeCombo");

        settingsPanel.addSeparator();

        interactionCombo = new JComboBox<>(SelectionInteraction.values());
        settingsPanel.addWithLabel("New Selection:", interactionCombo, "selectionInteractionCombo");

        settingsPanel.addSeparator();

        settingsPanel.addButton(SelectionActions.getTraceWithBrush());
        settingsPanel.addButton(SelectionActions.getTraceWithEraser());

// TODO why doesn't this work?
//        settingsPanel.addButton(SelectionActions.getTraceWithSmudge());
        
        settingsPanel.addButton(SelectionActions.getCrop());
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

        selectionBuilder.updateSelection(userDrag.toImDrag());
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

            selectionBuilder.updateSelection(userDrag.toImDrag());
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

        deselect(ic, true);

        altMeansSubtract = false;

        return false;
    }

    private static void deselect(ImageComponent ic, boolean addToHistory) {
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

        node.addString("Type", typeCombo.getSelectedItem().toString());
        node.addString("Interaction", interactionCombo.getSelectedItem().toString());

        return node;
    }
}
