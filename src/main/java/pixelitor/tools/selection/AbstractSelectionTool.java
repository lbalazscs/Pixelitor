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

package pixelitor.tools.selection;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionActions;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.ShapeCombinator;
import pixelitor.tools.DragTool;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;

import javax.swing.*;
import java.awt.Cursor;
import java.util.ResourceBundle;

public abstract class AbstractSelectionTool extends DragTool {
    private static final String NEW_SELECTION_TEXT = "New Selection";

    private final EnumComboBoxModel<ShapeCombinator> combinatorModel
        = new EnumComboBoxModel<>(ShapeCombinator.class);

    // the shape combinator that was selected in the UI
    // before being overridden by pressing the SHIFT or ALT key
    private ShapeCombinator previousShapeCombinator;

    // whether the Alt key is pressed and used for subtraction mode
    protected boolean altMeansSubtract = false;

    // manages the building of the current selection
    protected SelectionBuilder selectionBuilder;

    protected AbstractSelectionTool(String name, char hotKey, String toolMessage, Cursor cursor, boolean shiftConstrains) {
        super(name, hotKey, toolMessage, cursor, shiftConstrains);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initSettingsPanel(ResourceBundle resources) {
        var combinatorCB = new JComboBox<ShapeCombinator>(combinatorModel);
        settingsPanel.addComboBox(NEW_SELECTION_TEXT + ":",
            combinatorCB, "combinatorCB");

        settingsPanel.addSeparator();

        settingsPanel.addButton(SelectionActions.getCrop(),
            "cropButton", "Crop using the current selection");

        settingsPanel.addButton(SelectionActions.getConvertToPath(),
            "toPathButton", "Convert the selection to a path");
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        // otherwise in polygonal mode unfinished selections
        // remain visible after switching to another tool
        stopBuildingSelection(view.getComp());
    }

    public ShapeCombinator getCombinator() {
        return combinatorModel.getSelectedItem();
    }

    protected void setupCombinatorWithKeyModifiers(PMouseEvent e) {
        boolean shiftDown = e.isShiftDown();
        altDown = e.isAltDown();

        altMeansSubtract = altDown;

        if (shiftDown || altDown) {
            previousShapeCombinator = getCombinator();
            if (shiftDown) {
                if (altDown) {
                    setCombinator(ShapeCombinator.INTERSECT);
                } else {
                    setCombinator(ShapeCombinator.ADD);
                }
            } else if (altDown) {
                setCombinator(ShapeCombinator.SUBTRACT);
            }
        }
    }

    @Override
    public void escPressed() {
        // pressing Esc should work the same as clicking outside the selection
        Views.onActiveComp(this::cancelSelection);
    }

    protected void cancelSelection(Composition comp) {
        if (comp.hasSelection() || comp.hasDraftSelection()) {
            comp.deselect(true);
        }
        assert !comp.hasDraftSelection() : "draft selection is = " + comp.getDraftSelection();
        assert !comp.hasSelection() : "selection is = " + comp.getSelection();

        altMeansSubtract = false;

        if (AppMode.isDevelopment()) {
            ConsistencyChecks.selectionActionsEnabledCheck(comp);
        }
    }

    protected void stopBuildingSelection(Composition comp) {
        if (selectionBuilder != null) {
            selectionBuilder.cancelIfNotFinished(comp);
            selectionBuilder = null;
        }
    }

    protected void resetCombinator() {
        if (previousShapeCombinator != null) {
            setCombinator(previousShapeCombinator);
            previousShapeCombinator = null;
        }
    }

    protected void setCombinator(ShapeCombinator combinator) {
        combinatorModel.setSelectedItem(combinator);
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = Views.getActive();
        if (view != null) {
            Selection selection = view.getComp().getSelection();
            if (selection != null) {
                selection.nudge(key.toTransform());
                return true;
            }
        }
        return false;
    }

    protected void marqueeLassoDragFinished(PMouseEvent e) {
        if (drag.isClick()) { // will be handled by mouseClicked
            resetCombinator();
            return;
        }

        Composition comp = e.getComp();
        Selection draftSelection = comp.getDraftSelection();
        if (draftSelection == null) {
            // can happen, if we called stopBuildingSelection()
            // for some exceptional reason
            return;
        }

        notPolygonalDragFinished(e);

        altMeansSubtract = false;

        assert ConsistencyChecks.selectionShapeIsNotEmpty(comp) : "selection is empty";
        assert ConsistencyChecks.selectionIsInsideCanvas(comp) : "selection is outside";
    }

    private void notPolygonalDragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        resetCombinator();

        boolean startFromCenter = !altMeansSubtract && e.isAltDown();
        drag.setExpandFromCenter(startFromCenter);

        selectionBuilder.updateDraftSelection(drag, comp, e);
        selectionBuilder.combineShapes(comp);
        stopBuildingSelection(comp);

        assert !comp.hasDraftSelection();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.put(NEW_SELECTION_TEXT, getCombinator().toString());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        setCombinator(preset.getEnum(NEW_SELECTION_TEXT, ShapeCombinator.class));
    }
}
