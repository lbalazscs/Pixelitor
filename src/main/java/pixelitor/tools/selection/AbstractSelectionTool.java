/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GlobalEvents;
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
    private static final String PRESET_KEY_COMBINATOR = "New Selection";

    private final EnumComboBoxModel<ShapeCombinator> combinatorModel
        = new EnumComboBoxModel<>(ShapeCombinator.class);

    // the shape combinator selected in the UI
    // before being overridden by Shift/Alt keys
    private ShapeCombinator baseShapeCombinator;

    // is the currently-held-down Alt key the same one that was down when this
    // drag started (and therefore already consumed for the SUBTRACT/INTERSECT combinator),
    // as opposed to a fresh Alt press that should trigger expand-from-center?
    protected boolean altUsedForCombinator = false;

    // manages the building of the current selection shape
    protected SelectionBuilder selectionBuilder;

    protected AbstractSelectionTool(String name, char hotkey, String statusBarMessage, Cursor cursor, boolean shiftConstrains) {
        super(name, hotkey,
            statusBarMessage + " <b>Shift</b> adds, <b>Alt</b> subtracts, <b>Shift+Alt</b> intersects.",
            cursor, shiftConstrains);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initSettingsPanel(ResourceBundle resources) {
        var combinatorCB = new JComboBox<ShapeCombinator>(combinatorModel);
        settingsPanel.addComboBox("New Selection:",
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
        updateCombinatorFromModifiers(e);

        // create the builder for this drag operation
        selectionBuilder = new SelectionBuilder(
            selectionType, getCombinator(), e.getComp());
    }

    /**
     * Sets the shape combination mode based on Shift/Alt key modifiers.
     */
    protected void updateCombinatorFromModifiers(PMouseEvent e) {
        boolean shiftDown = e.isShiftDown();
        boolean altDown = e.isAltDown();
        assert altDown == GlobalEvents.isAltDown() || AppMode.isUnitTesting()
            : "altDown = " + altDown + ", GlobalEvents.isAltDown() = " + GlobalEvents.isAltDown();

        // alt pressed before the drag means a combination mode (SUBTRACT or INTERSECT)
        altUsedForCombinator = altDown;

        if (shiftDown || altDown) {
            baseShapeCombinator = getCombinator();

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
        super.escPressed();

        Composition comp = Views.getActiveComp();
        if (comp != null) {
            cancelSelection(comp);
        }
    }

    protected void cancelSelection(Composition comp) {
        // if a selection is being built, cancel it first
        cancelSelectionBuilder();

        if (comp.hasSelection() || comp.hasDraftSelection()) {
            comp.deselect(true);
        }
        assert !comp.hasDraftSelection() : "draft selection: " + comp.getDraftSelection();
        assert !comp.hasSelection() : "selection: " + comp.getSelection();

        altUsedForCombinator = false;

        if (AppMode.isDevelopment()) {
            Invariants.selectionActionsEnabledCheck(comp);
        }
    }

    /**
     * Stops the current selection building process if one is active,
     * and restores the combinator overridden by modifier keys.
     */
    protected void cancelSelectionBuilder() {
        if (selectionBuilder != null) {
            selectionBuilder.cancelIfNotFinalized();
            selectionBuilder = null;
        }

        resetCombinator();
    }

    /**
     * Restores the shape combinator that was active before modifier keys were pressed.
     */
    protected void resetCombinator() {
        if (baseShapeCombinator != null) {
            setCombinator(baseShapeCombinator);
            baseShapeCombinator = null;
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
            if (selection != null && !selection.isHidden()) {
                selection.nudge(key);
                return true; // key event consumed
            }
        }
        return false; // key event not consumed
    }

    @Override
    public void altReleased() {
        altUsedForCombinator = false;
    }

    /**
     * Finalizes the selection after a drag operation for Marquee or Lasso tools.
     */
    protected void finalizeDragBasedSelection(PMouseEvent e) {
        if (drag.isClick()) {
            if (e.isRight() || getCombinator() == ShapeCombinator.REPLACE) {
                cancelSelection(e.getComp()); // normal click-to-deselect
            } else {
                // we assume that a 0-pixel click is an accidentally aborted drag
                cancelSelectionBuilder(); // abort the empty drag, keep existing selection
                altUsedForCombinator = false; // reset the drag modifier state
            }
            return;
        }

        Composition comp = e.getComp();
        Selection draftSelection = comp.getDraftSelection();
        if (draftSelection == null) {
            cancelSelectionBuilder();
            return;
        }

        // finish the selection process (update shape, combine, cleanup)
        resetCombinator();
        boolean expandFromCenter = !altUsedForCombinator && e.isAltDown();
        drag.setExpandFromCenter(expandFromCenter);
        selectionBuilder.updateDraftSelection(drag);
        selectionBuilder.combineShapes();
        cancelSelectionBuilder();
        assert !comp.hasDraftSelection();

        // reset state for the next operation
        altUsedForCombinator = false;

        assert Invariants.selectionShapeIsNotEmpty(comp.getSelection()) : "selection is empty";
        assert Invariants.selectionIsInsideCanvas(comp) : "selection is outside";
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        // ensure unfinished selections are canceled after switching tools
        // (necessary for polygonal selections, safety net for the other tools)
        cancelSelectionBuilder();
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.put(PRESET_KEY_COMBINATOR, getCombinator().name());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        setCombinator(preset.getEnum(PRESET_KEY_COMBINATOR, ShapeCombinator.class));
    }

    @Override
    public boolean checkInvariants() {
        return true; // TODO
    }
}
