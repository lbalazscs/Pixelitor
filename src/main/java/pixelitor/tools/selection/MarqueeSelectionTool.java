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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.Views;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.Selection;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionType;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.BasicStroke;
import java.awt.Graphics2D;

/**
 * A selection tool that creates rectangle or ellipse selections.
 */
public class MarqueeSelectionTool extends AbstractSelectionTool {
    private final SelectionType selectionType;

    public MarqueeSelectionTool(SelectionType selectionType) {
        super(selectionType.toString() + " Selection", 'M', "<b>click and drag</b> creates a selection, " +
            "<b>Space-drag</b> moves it. " +
            "<b>Shift-drag</b> adds to an existing selection, " +
            "<b>Alt-drag</b> removes from it, <b>Shift-Alt-drag</b> intersects.", Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
        pixelSnapping = true;
        this.selectionType = selectionType;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        setupCombinatorWithKeyModifiers(e);

        selectionBuilder = new SelectionBuilder(
            selectionType, getCombinator(), e.getComp());
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
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
        stopBuildingSelection();

        assert !comp.hasDraftSelection();
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Composition comp = e.getComp();
        super.mouseClicked(e);

        cancelSelection(comp);
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
    protected OverlayType getDragDisplayType() {
        return OverlayType.WIDTH_HEIGHT;
    }

    @Override
    protected void toolDeactivated() {
        super.toolDeactivated();

        // otherwise in polygonal mode unfinished selections
        // remain visible after switching to another tool
        stopBuildingSelection();
    }

    @Override
    public boolean hasSharedHotkey() {
        return true;
    }

    @Override
    public VectorIcon createIcon() {
        if (selectionType == SelectionType.RECTANGLE) {
            return new RectangleSelectionToolIcon();
        } else {
            return new EllipseSelectionToolIcon();
        }
    }

    public static class EllipseSelectionToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4.1888f, 4.1888f}, 0));
            g.drawOval(2, 2, 24, 24);
        }
    }

    public static class RectangleSelectionToolIcon extends ToolIcon {
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

