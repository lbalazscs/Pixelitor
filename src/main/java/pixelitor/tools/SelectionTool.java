/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.GUIMode;
import pixelitor.Views;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.*;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;

import static pixelitor.selection.ShapeCombinator.*;

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
    private static final String NEW_SELECTION_TEXT = "New Selection";

    private boolean altMeansSubtract = false;
    private ShapeCombinator originalShapeCombinator;

    private SelectionBuilder selectionBuilder;
    private boolean polygonal = false;
    private boolean displayWidthHeight = true;

    private final EnumComboBoxModel<SelectionType> typeModel
        = new EnumComboBoxModel<>(SelectionType.class);
    private final EnumComboBoxModel<ShapeCombinator> combinatorModel
        = new EnumComboBoxModel<>(ShapeCombinator.class);

    SelectionTool() {
        super("Selection", 'M', HELP_TEXT, Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initSettingsPanel() {
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
    }

    private void selectionTypeChanged() {
        stopBuildingSelection();

        SelectionType type = getSelectionType();
        polygonal = type == SelectionType.POLYGONAL_LASSO;
        displayWidthHeight = type.displayWidthHeight();

        if (polygonal) {
            Messages.showInStatusBar(POLY_HELP_TEXT);
        } else if (type == SelectionType.LASSO) {
            Messages.showInStatusBar(FREEHAND_HELP_TEXT);
        } else {
            Messages.showInStatusBar("Selection Tool: " + HELP_TEXT);
        }
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        if (polygonal) {
            return; // ignore mouse pressed
        }

        setupCombinatorWithKeyModifiers(e);

        selectionBuilder = new SelectionBuilder(
            getSelectionType(), getCombinator(), e.getComp());
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (polygonal) {
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

        drag.setStartFromCenter(startFromCenter);
        selectionBuilder.updateInProgressSelection(drag, e.getComp());
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        if (drag.isClick() && !polygonal) { // will be handled by mouseClicked
            restoreCombinator();
            return;
        }

        var comp = e.getComp();
        Selection builtSelection = comp.getInProgressSelection();
        if (builtSelection == null && !polygonal) {
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
        var comp = e.getComp();
        if (selectionBuilder == null) {
            setupCombinatorWithKeyModifiers(e);
            selectionBuilder = new SelectionBuilder(
                getSelectionType(), getCombinator(), comp);
            selectionBuilder.updateInProgressSelection(e, comp);
            restoreCombinator();
        } else {
            selectionBuilder.updateInProgressSelection(e, comp);
            if (e.isRight()) {
                selectionBuilder.combineShapes(comp);
                stopBuildingSelection();
            }
        }
    }

    private void notPolygonalDragFinished(PMouseEvent e) {
        var comp = e.getComp();
        restoreCombinator();

        boolean startFromCenter = !altMeansSubtract && e.isAltDown();
        drag.setStartFromCenter(startFromCenter);

        selectionBuilder.updateInProgressSelection(drag, comp);
        selectionBuilder.combineShapes(comp);
        stopBuildingSelection();

        assert !comp.hasInProgressSelection();
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        var comp = e.getComp();
        if (polygonal) {
            if (selectionBuilder != null && e.getClickCount() > 1) {
                // finish polygonal for double-click
                selectionBuilder.updateInProgressSelection(e, comp);
                selectionBuilder.combineShapes(comp);
                stopBuildingSelection();
            } else {
                // ignore otherwise: will be handled in mouse released
            }
            return;
        }

        super.mouseClicked(e);

        cancelSelection(comp);
    }

    private void cancelSelection(Composition comp) {
        if (comp.hasSelection() || comp.hasInProgressSelection()) {
            comp.deselect(true);
        }
        assert !comp.hasInProgressSelection() : "built selection is = " + comp.getInProgressSelection();
        assert !comp.hasSelection() : "selection is = " + comp.getSelection();

        altMeansSubtract = false;

        if (GUIMode.isDevelopment()) {
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
            drag.setStartFromCenter(true);
            selectionBuilder.updateInProgressSelection(drag, Views.getActiveComp());
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (!altMeansSubtract && drag != null && drag.isDragging()) {
            drag.setStartFromCenter(false);
            selectionBuilder.updateInProgressSelection(drag, Views.getActiveComp());
        }
        altDown = false;
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = Views.getActive();
        if (view != null) {
            var comp = view.getComp();
            var selection = comp.getSelection();
            if (selection != null) {
                selection.nudge(key.asTransform());
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
            originalShapeCombinator = getCombinator();
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

    private void restoreCombinator() {
        if (originalShapeCombinator != null) {
            setCombinator(originalShapeCombinator);
            originalShapeCombinator = null;
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
    protected void toolEnded() {
        super.toolEnded();

        // otherwise in polygonal mode half-built selections
        // remain visible after switching to another tool
        stopBuildingSelection();
    }

    @VisibleForTesting
    public SelectionType getSelectionType() {
        return typeModel.getSelectedItem();
    }

    @VisibleForTesting
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
        var node = super.createDebugNode(key);

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
