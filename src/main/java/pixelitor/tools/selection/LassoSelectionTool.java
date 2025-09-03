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

import pixelitor.selection.SelectionType;
import pixelitor.tools.ToolIcons;
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * A tool that creates freehand selections by dragging.
 */
public class LassoSelectionTool extends AbstractSelectionTool {
    public LassoSelectionTool() {
        super("Freehand Selection", 'L',
            "simply drag around the area that you want to select.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = true;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        initCombinatorAndBuilder(e, SelectionType.LASSO);

        // add the starting point immediately?
        // selectionBuilder.updateDraftSelection(drag, e.getComp(), e);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (selectionBuilder == null) {
            // can happen if the image changed mid-drag; restart the drag
            dragStarted(e);
        }

        altDown = e.isAltDown();
        // if Alt is released mid-drag, it no longer means subtract for this drag
        if (!altDown) {
            altMeansSubtract = false;
        }

        // add the current point to the lasso path
        selectionBuilder.updateDraftSelection(drag);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        // common logic in the base class
        finalizeDragBasedSelection(e);
    }

    @Override
    protected OverlayType getOverlayType() {
        // no measurement overlay for freehand drawing
        return OverlayType.NONE;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintLassoSelectionIcon;
    }
}
