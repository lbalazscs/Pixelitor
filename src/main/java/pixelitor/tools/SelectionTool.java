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

package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.*;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Graphics2D;
import java.util.ResourceBundle;

import static pixelitor.gui.utils.SliderSpinner.LabelPosition.WEST;
import static pixelitor.selection.ShapeCombinator.ADD;
import static pixelitor.selection.ShapeCombinator.INTERSECT;
import static pixelitor.selection.ShapeCombinator.SUBTRACT;

/**
 * The selection tool
 */
public class SelectionTool extends DragTool {
    private static final String HELP_TEXT = "<b>click and drag</b> creates a selection, " +
        "<b>Space-drag</b> moves it. " +
        "<b>Shift-drag</b> adds to an existing selection, " +
        "<b>Alt-drag</b> removes from it, <b>Shift-Alt-drag</b> intersects.";
    private static final String POLY_HELP_TEXT = "Polygonal selection: " +
        "<b>click</b> to add points, " +
        "<b>double-click</b> (or <b>right-click</b>) to close the selection." +
        "<b>Shift</b> adds to an existing selection, " +
        "<b>Alt</b> removes from it, <b>Shift+Alt</b> intersects.";
    private static final String FREEHAND_HELP_TEXT = "Freehand selection: " +
        "simply drag around the area that you want to select. " +
        "<b>Shift-drag</b> adds to an existing selection, " +
        "<b>Alt-drag</b> removes from it, <b>Shift-Alt-drag</b> intersects.";
    private static final String MAGIC_WAND_HELP_TEXT = "MagicWand selection: " +
        "<b>click</b> on the area you want to select. " +
        "<b>right-click</b> to cancel the selection." +
        "<b>Shift</b> adds to an existing selection, " +
        "<b>Alt</b> removes from it, <b>Shift-Alt</b> intersects.";
    private static final String NEW_SELECTION_TEXT = "New Selection";

    // whether the Alt key is pressed and used for subtraction mode
    private boolean altMeansSubtract = false;

    // the shape combinator that was selected in the UI
    // before being overridden by pressing the SHIFT or ALT key
    private ShapeCombinator previousShapeCombinator;

    // manages the building of the current selection
    private SelectionBuilder selectionBuilder;

    // whether the selection type is polygonal
    private boolean polygonal = false;

    // whether the selection type is magic wand
    private boolean magicWand = false;

    // whether the width/height should be displayed
    private boolean displayWidthHeight = true;

    private final EnumComboBoxModel<SelectionType> typeModel
        = new EnumComboBoxModel<>(SelectionType.class);
    private final EnumComboBoxModel<ShapeCombinator> combinatorModel
        = new EnumComboBoxModel<>(ShapeCombinator.class);

    private static final RangeParam magicWandToleranceParam = new RangeParam("Tolerance", 0, 20, 255);
    private static final SliderSpinner magicWandToleranceSlider = new SliderSpinner(magicWandToleranceParam, WEST, false);

    SelectionTool() {
        super("Selection", 'M', HELP_TEXT, Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
        pixelSnapping = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initSettingsPanel(ResourceBundle resources) {
        var typeCB = new JComboBox<SelectionType>(typeModel);
        typeCB.addActionListener(e -> selectionTypeChanged());
        settingsPanel.addComboBox(GUIText.TYPE + ":", typeCB, "typeCB");

        settingsPanel.addSeparator();

        var combinatorCB = new JComboBox<ShapeCombinator>(combinatorModel);
        settingsPanel.addComboBox(NEW_SELECTION_TEXT + ":",
            combinatorCB, "combinatorCB");

        settingsPanel.addSeparator();

        settingsPanel.addButton(SelectionActions.getCrop(),
            "cropButton", "Crop using the current selection");

        settingsPanel.addButton(SelectionActions.getConvertToPath(),
            "toPathButton", "Convert the selection to a path");

        settingsPanel.addSeparator();

        magicWandToleranceSlider.setEnabled(false);
        settingsPanel.add(magicWandToleranceSlider);
    }

    private void selectionTypeChanged() {
        stopBuildingSelection();

        SelectionType type = getSelectionType();
        polygonal = type == SelectionType.POLYGONAL_LASSO;
        magicWand = type == SelectionType.SELECTION_MAGIC_WAND;
        displayWidthHeight = type.displayWidthHeight();

        magicWandToleranceSlider.setEnabled(magicWand);

        if (polygonal) {
            Messages.showStatusMessage(POLY_HELP_TEXT);
        } else if (type == SelectionType.LASSO) {
            Messages.showStatusMessage(FREEHAND_HELP_TEXT);
        } else if (magicWand) {
            Messages.showStatusMessage(MAGIC_WAND_HELP_TEXT);
        } else {
            Messages.showStatusMessage("Selection Tool: " + HELP_TEXT);
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        if (polygonal || magicWand) {
            return; // ignore mouse pressed
        }

        setupCombinatorWithKeyModifiers(e);

        selectionBuilder = new SelectionBuilder(
            getSelectionType(), getCombinator(), e.getComp());
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (polygonal || magicWand) {
            return; // ignore dragging
        }
        if (selectionBuilder == null) {
            // the image was changed so start again
            dragStarted(e);
        }

        altDown = e.isAltDown();
        boolean startFromCenter = !altMeansSubtract && altDown;
        if (!altDown) {
            altMeansSubtract = false;
        }

        drag.setExpandFromCenter(startFromCenter);
        selectionBuilder.updateDraftSelection(drag, e.getComp(), e);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        if (drag.isClick() && !polygonal && !magicWand) { // will be handled by mouseClicked
            resetCombinator();
            return;
        }

        Composition comp = e.getComp();
        Selection draftSelection = comp.getDraftSelection();
        if (draftSelection == null && !polygonal) {
            // can happen, if we called stopBuildingSelection()
            // for some exceptional reason
            return;
        }

        if (polygonal) {
            polygonalDragFinished(e);
        } else {
            notPolygonalDragFinished(e);
        }

        altMeansSubtract = false;

        assert ConsistencyChecks.selectionShapeIsNotEmpty(comp) : "selection is empty";
        assert ConsistencyChecks.selectionIsInsideCanvas(comp) : "selection is outside";
    }

    private void polygonalDragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        if (selectionBuilder == null) {
            setupCombinatorWithKeyModifiers(e);
            selectionBuilder = new SelectionBuilder(
                getSelectionType(), getCombinator(), comp);
            selectionBuilder.updateDraftSelection(e, comp);
            resetCombinator();
        } else {
            selectionBuilder.updateDraftSelection(e, comp);
            if (e.isRight()) {
                selectionBuilder.combineShapes(comp);
                stopBuildingSelection();
            }
        }
    }

    private void notPolygonalDragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        resetCombinator();

        boolean startFromCenter = !altMeansSubtract && e.isAltDown();
        drag.setExpandFromCenter(startFromCenter);

        selectionBuilder.updateDraftSelection(drag, comp, e);
        selectionBuilder.combineShapes(comp);
        stopBuildingSelection();

        assert !comp.hasDraftSelection();
    }

    public static int getTolerance() {
        return magicWandToleranceParam.getValue();
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Composition comp = e.getComp();
        if (polygonal) {
            if (selectionBuilder != null && e.getClickCount() > 1) {
                // finish polygonal for double-click
                selectionBuilder.updateDraftSelection(e, comp);
                selectionBuilder.combineShapes(comp);
                stopBuildingSelection();
            } else {
                // ignore otherwise: will be handled in mouse released
            }
            return;
        } else if (magicWand) {
            setupCombinatorWithKeyModifiers(e);
            selectionBuilder = new SelectionBuilder(
                    getSelectionType(), getCombinator(), e.getComp());

            if (e.isRight()) {
                cancelSelection(comp);
            } else if (selectionBuilder != null && e.getClickCount() == 1) {
                SelectionBuilder sb = selectionBuilder;
                SwingWorker<Void,Void> swingWorker = new SwingWorker<>() {
                    @Override
                    public Void doInBackground() {
                        try {
                            sb.updateDraftSelection(e, comp);
                            sb.combineShapes(comp);
                            stopBuildingSelection();
                        } catch (Exception e) {
                            cancelSelection(comp);
                        }
                    return null;
                    }
                };
                swingWorker.execute();
            }
        }

        super.mouseClicked(e);

        if (!magicWand) { cancelSelection(comp); }
    }

    private void cancelSelection(Composition comp) {
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

    @Override
    public void escPressed() {
        // pressing Esc should work the same as clicking outside the selection
        Views.onActiveComp(this::cancelSelection);
    }

    @Override
    public void altPressed() {
        if (!altDown && !altMeansSubtract && drag != null && drag.isDragging()) {
            drag.setExpandFromCenter(true);
            selectionBuilder.updateDraftSelection(drag, Views.getActiveComp(), null);
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (!altMeansSubtract && drag != null && drag.isDragging()) {
            drag.setExpandFromCenter(false);
            selectionBuilder.updateDraftSelection(drag, Views.getActiveComp(), null);
        }
        altDown = false;
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

    @Override
    protected DragDisplayType getDragDisplayType() {
        if (displayWidthHeight) {
            return DragDisplayType.WIDTH_HEIGHT;
        }
        return DragDisplayType.NONE;
    }

    private void setupCombinatorWithKeyModifiers(PMouseEvent e) {
        boolean shiftDown = e.isShiftDown();
        altDown = e.isAltDown();

        altMeansSubtract = altDown;

        if (shiftDown || altDown) {
            previousShapeCombinator = getCombinator();
            if (shiftDown) {
                if (altDown) {
                    setCombinator(INTERSECT);
                } else {
                    setCombinator(ADD);
                }
            } else if (altDown) {
                setCombinator(SUBTRACT);
            }
        }
    }

    private void resetCombinator() {
        if (previousShapeCombinator != null) {
            setCombinator(previousShapeCombinator);
            previousShapeCombinator = null;
        }
    }

    @Override
    public void allViewsClosed() {
        // ignore
    }

    @Override
    public void viewActivated(View oldCV, View newCV) {
        stopBuildingSelection();
    }

    private void stopBuildingSelection() {
        if (selectionBuilder != null) {
            selectionBuilder.cancelIfNotFinished(Views.getActiveComp());
            selectionBuilder = null;
        }
    }

    @Override
    protected void toolDeactivated() {
        super.toolDeactivated();

        // otherwise in polygonal mode unfinished selections
        // remain visible after switching to another tool
        stopBuildingSelection();
    }

    public SelectionType getSelectionType() {
        return typeModel.getSelectedItem();
    }

    public ShapeCombinator getCombinator() {
        return combinatorModel.getSelectedItem();
    }

    private void setSelectionType(SelectionType type) {
        typeModel.setSelectedItem(type);
    }

    private void setCombinator(ShapeCombinator combinator) {
        combinatorModel.setSelectedItem(combinator);
    }

    @Override
    public void saveStateTo(UserPreset preset) {
        preset.put(GUIText.TYPE, getSelectionType().toString());
        preset.put(NEW_SELECTION_TEXT, getCombinator().toString());
    }

    @Override
    public void loadUserPreset(UserPreset preset) {
        setSelectionType(preset.getEnum(GUIText.TYPE, SelectionType.class));
        setCombinator(preset.getEnum(NEW_SELECTION_TEXT, ShapeCombinator.class));
    }

    @Override
    public String getStateInfo() {
        return getSelectionType() + ", " + getCombinator();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addAsString("type", getSelectionType());
        node.addAsString("combinator", getCombinator());

        return node;
    }

    @Override
    public VectorIcon createIcon() {
        return new SelectionToolIcon();
    }

    private static class SelectionToolIcon extends Tool.ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // based on selection_tool.svg

            // north
            g.fillRect(1, 1, 4, 2);
            g.fillRect(9, 1, 4, 2);
            g.fillRect(17, 1, 4, 2);

            // east
            g.fillRect(25, 1, 2, 4);
            g.fillRect(25, 9, 2, 4);
            g.fillRect(25, 17, 2, 4);

            // south
            g.fillRect(7, 25, 4, 2);
            g.fillRect(15, 25, 4, 2);
            g.fillRect(23, 25, 4, 2);

            // west
            g.fillRect(1, 7, 2, 4);
            g.fillRect(1, 15, 2, 4);
            g.fillRect(1, 23, 2, 4);
        }
    }
}
