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

import pixelitor.Composition;
import pixelitor.Invariants;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionType;
import pixelitor.tools.DragTool;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * A tool that creates polygonal selections by clicking points.
 * Indirectly extends {@link DragTool} for architectural reuse, but
 * most of the drag hooks are unused or repurposed for click handling.
 */
public class PolygonalSelectionTool extends AbstractSelectionTool {
    // the freehand and polygonal selection tools share the 'L' hotkey, with cycling
    public PolygonalSelectionTool() {
        super("Polygonal Selection", 'L',
            "<b>click</b> to add points, " +
                "<b>double-click</b> or <b>right-click</b> to close the selection.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = true;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        // ignored, polygon points are added on mouse release/click
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        // ignored, dragging doesn't modify the polygon being built
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        Composition comp = e.getComp();
        if (selectionBuilder == null) {
            // first click: start building the polygon
            updateCombinatorFromModifiers(e);
            selectionBuilder = new SelectionBuilder(
                SelectionType.POLYGONAL_LASSO, getCombinator(), comp);
            selectionBuilder.updateDraftSelection(e);
            resetCombinator();
        } else {
            // subsequent click: check for double-click or right-click
            if (e.getClickCount() > 1) {
                // double-click finishes the polygon (the first half
                // of the double-click already recorded the final point)
                selectionBuilder.combineShapes();
                cancelSelectionBuilder();
            } else {
                // single click: add another point
                selectionBuilder.updateDraftSelection(e);
                if (e.isRight()) {
                    // right-click finishes the polygon
                    selectionBuilder.combineShapes();
                    cancelSelectionBuilder();
                }
            }
        }

        assert Invariants.selectionShapeIsNotEmpty(comp.getSelection()) : "selection is empty";
        assert Invariants.selectionIsInsideCanvas(comp) : "selection is outside";
    }

    @Override
    protected OverlayType getOverlayType() {
        // no drag overlay as we build point by point
        return OverlayType.NONE;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintPolygonalSelectionIcon;
    }
}
