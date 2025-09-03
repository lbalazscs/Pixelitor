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

import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.gui.View;
import pixelitor.selection.SelectionBuilder;
import pixelitor.selection.SelectionType;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * A tool that creates polygonal selections by clicking points.
 */
public class PolygonalSelectionTool extends AbstractSelectionTool {
    public PolygonalSelectionTool() {
        super("Polygonal Selection", 'L',
            "<b>click</b> to add points, " +
                "<b>double-click</b> or <b>right-click</b> to close the selection.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = true;
    }

    @Override
    protected void toolDeactivated(View view) {
        super.toolDeactivated(view);

        // ensure unfinished selections are cancelled
        // and don't remain visible after switching tools
        cancelSelectionBuilder();
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
            setupCombinatorWithKeyModifiers(e);
            selectionBuilder = new SelectionBuilder(
                SelectionType.POLYGONAL_LASSO, getCombinator(), comp);
            selectionBuilder.updateDraftSelection(e);
            resetCombinator();
        } else {
            // subsequent click: add another point
            selectionBuilder.updateDraftSelection(e);
            if (e.isRight()) {
                // right-click finishes the polygon
                selectionBuilder.combineShapes();
                cancelSelectionBuilder();
            }
        }

        assert ConsistencyChecks.selectionShapeIsNotEmpty(comp) : "selection is empty";
        assert ConsistencyChecks.selectionIsInsideCanvas(comp) : "selection is outside";
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        Composition comp = e.getComp();
        // handle double-click to finish the polygon
        if (selectionBuilder != null && e.getClickCount() > 1) {
            // update with the final point (same as the double-clicked point)
            selectionBuilder.updateDraftSelection(e);
            selectionBuilder.combineShapes();
            cancelSelectionBuilder();
        }
        // single clicks are handled in dragFinished
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

