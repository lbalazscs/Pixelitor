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
import pixelitor.Invariants;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.View;
import pixelitor.selection.*;
import pixelitor.tools.DragTool;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;

import javax.swing.*;
import java.awt.Cursor;
import java.util.ResourceBundle;

/**
 * Abstract base class for tools that create selections.
 */
public abstract class AbstractSelectionTool extends DragTool {
    private static final String NEW_SELECTION_TEXT = "New Selection";

    private final EnumComboBoxModel<ShapeCombinator> combinatorModel
        = new EnumComboBoxModel<>(ShapeCombinator.class);

    // the shape combinator selected in the UI
    // before being overridden by Shift/Alt keys
    private ShapeCombinator prevShapeCombinator;

    // true if the Alt key is pressed and used for subtraction mode
    protected boolean altMeansSubtract = false;

    // manages the building of the current selection shape
    protected SelectionBuilder selectionBuilder;

    protected AbstractSelectionTool(String name, char hotKey, String toolMessage, Cursor cursor, boolean shiftConstrains) {
        super(name, hotKey,
            toolMessage + " <b>Shift</b> adds, <b>Alt</b> subtracts, <b>Shift+Alt</b> intersects.",
            cursor, shiftConstrains);
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

    public ShapeCombinator getCombinator() {
        return combinatorModel.getSelectedItem();
    }

    protected void initCombinatorAndBuilder(PMouseEvent e, SelectionType selectionType) {
        // setup combinator based on initial key state
        setupCombinatorWithKeyModifiers(e);

        // create the builder for this drag operation
        selectionBuilder = new SelectionBuilder(
            selectionType, getCombinator(), e.getComp());
    }

    /**
     * Sets the shape combination mode based on Shift/Alt key modifiers.
     */
    protected void setupCombinatorWithKeyModifiers(PMouseEvent e) {
        boolean shiftDown = e.isShiftDown();
        altDown = e.isAltDown();

        // alt always means subtract if pressed before the start of the drag
        altMeansSubtract = altDown;

        if (shiftDown || altDown) {
            prevShapeCombinator = getCombinator();

            if (shiftDown && altDown) {
                setCombinator(ShapeCombinator.INTERSECT);
            } else if (shiftDown) { // only shift
                setCombinator(ShapeCombinator.ADD);
            } else { // only alt
                setCombinator(ShapeCombinator.SUBTRACT);
            }
        }
    }

    @Override
    public void escPressed() {
        Views.onActiveComp(this::cancelSelection);
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        cancelSelection(e.getComp());
    }

    protected void cancelSelection(Composition comp) {
        // if a selection is being built, cancel it first
        cancelSelectionBuilder();

        if (comp.hasSelection() || comp.hasDraftSelection()) {
            comp.deselect(true);
        }
        assert !comp.hasDraftSelection() : "draft selection is = " + comp.getDraftSelection();
        assert !comp.hasSelection() : "selection is = " + comp.getSelection();

        altMeansSubtract = false;

        if (AppMode.isDevelopment()) {
            Invariants.selectionActionsEnabledCheck(comp);
        }
    }

    /**
     * Stops the current selection building process if one is active.
     */
    protected void cancelSelectionBuilder() {
        if (selectionBuilder != null) {
            selectionBuilder.cancelIfNotFinished();
            selectionBuilder = null;
        }
    }

    /**
     * Restores the shape combinator that was active before modifier keys were pressed.
     */
    protected void resetCombinator() {
        if (prevShapeCombinator != null) {
            setCombinator(prevShapeCombinator);
            prevShapeCombinator = null;
        }
    }

    private void setCombinator(ShapeCombinator combinator) {
        combinatorModel.setSelectedItem(combinator);
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = Views.getActive();
        if (view != null) {
            // nudges the existing selection using arrow keys
            Selection selection = view.getComp().getSelection();
            if (selection != null) {
                selection.nudge(key);
                return true; // key event consumed
            }
        }
        return false; // key event not consumed
    }

    /**
     * Finalizes the selection after a drag operation for Marquee or Lasso tools.
     */
    protected void finalizeDragBasedSelection(PMouseEvent e) {
        if (drag.isClick()) { // clicks are handled by mouseClicked
            // reset combinator if modifier keys were pressed but resulted in a click
            resetCombinator();
            return;
        }

        Composition comp = e.getComp();
        Selection draftSelection = comp.getDraftSelection();
        if (draftSelection == null) {
            // can happen, if we called cancelSelectionBuilder()
            // for some exceptional reason
            return;
        }

        // finish the selection process (update shape, combine, cleanup)
        resetCombinator();
        boolean expandFromCenter = !altMeansSubtract && e.isAltDown();
        drag.setExpandFromCenter(expandFromCenter);
        selectionBuilder.updateDraftSelection(drag);
        selectionBuilder.combineShapes();
        cancelSelectionBuilder();
        assert !comp.hasDraftSelection();

        // reset state for the next operation
        altMeansSubtract = false;

        assert Invariants.selectionShapeIsNotEmpty(comp.getSelection()) : "selection is empty";
        assert Invariants.selectionIsInsideCanvas(comp) : "selection is outside";
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
