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

import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.SelectionType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.BasicStroke;
import java.awt.Graphics2D;

/**
 * A tool that creates rectangular or elliptical selections by dragging.
 */
public class MarqueeSelectionTool extends AbstractSelectionTool {
    private final SelectionType selectionType;

    public MarqueeSelectionTool(SelectionType selectionType) {
        super(selectionType.toString() + " Selection", 'M',
            "<b>click and drag</b> creates a selection, " +
                "<b>Space-drag</b> moves it.", Cursors.DEFAULT, false);
        spaceDragStartPoint = true; // allow moving the start point with space down
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
        marqueeLassoDragFinished(e);
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
    public boolean hasSharedHotkey() {
        return true;
    }

    @Override
    public VectorIcon createIcon() {
        return selectionType == SelectionType.RECTANGLE
            ? new RectangleSelectionToolIcon()
            : new EllipseSelectionToolIcon();
    }

    private static class EllipseSelectionToolIcon extends ToolIcon {
        @Override
        public void paintIcon(Graphics2D g) {
            // draws a dashed ellipse
            g.setStroke(new BasicStroke(2,
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{4.1888f, 4.1888f}, 0));
            g.drawOval(2, 2, 24, 24);
        }
    }

    private static class RectangleSelectionToolIcon extends ToolIcon {
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

