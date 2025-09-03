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
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * A tool that creates rectangular or elliptical selections by dragging.
 */
public class MarqueeSelectionTool extends AbstractSelectionTool {
    private final SelectionType selectionType;

    public MarqueeSelectionTool(SelectionType selectionType) {
        super(selectionType.toString() + " Selection", 'M',
            "<b>click and drag</b> creates a selection, " +
                "<b>Space-drag</b> moves it.", Cursors.DEFAULT, false);
        repositionOnSpace = true; // allow moving the start point with space down
        pixelSnapping = true;
        this.selectionType = selectionType;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        initCombinatorAndBuilder(e, selectionType);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (selectionBuilder == null) {
            // can happen if the image changed mid-drag; restart the drag
            dragStarted(e);
        }

        // update alt state if Alt was pressed mid-drag
        altDown = e.isAltDown();

        // determine if Alt means "expand from center"
        // this is true if Alt is down, but wasn't pressed *at the start* for subtraction
        boolean expandFromCenter = !altMeansSubtract && altDown;

        // if Alt is released mid-drag, it no longer means subtract for this drag
        if (!altDown) {
            altMeansSubtract = false;
        }

        drag.setExpandFromCenter(expandFromCenter);
        selectionBuilder.updateDraftSelection(drag);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        // common logic in the base class
        finalizeDragBasedSelection(e);
    }

    @Override
    public void altPressed() {
        // handle pressing Alt *during* a drag: if Alt wasn't already
        // down and wasn't for subtraction, enable expand-from-center
        if (!altDown && !altMeansSubtract && drag != null && drag.isDragging()) {
            drag.setExpandFromCenter(true);
            selectionBuilder.updateDraftSelection(drag);
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        // handle releasing Alt *during* a drag: if Alt wasn't
        // for subtraction, disable expand-from-center
        if (!altMeansSubtract && drag != null && drag.isDragging()) {
            drag.setExpandFromCenter(false);
            selectionBuilder.updateDraftSelection(drag);
        }
        altDown = false;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return selectionType == SelectionType.RECTANGLE
            ? ToolIcons::paintRectangleSelectionIcon
            : ToolIcons::paintEllipseSelectionIcon;
    }
}

