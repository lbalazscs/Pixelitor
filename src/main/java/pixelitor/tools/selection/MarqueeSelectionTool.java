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

import pixelitor.AppMode;
import pixelitor.gui.GlobalEvents;
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

    // the rectangle and ellipse selection tools share the 'M' hotkey, with cycling
    public MarqueeSelectionTool(SelectionType selectionType) {
        super(selectionType.toString() + " Selection", 'M',
            "<b>click and drag</b> creates a selection, " +
                "<b>Space-drag</b> moves it.", Cursors.DEFAULT, false);

        if (selectionType != SelectionType.RECTANGLE && selectionType != SelectionType.ELLIPSE) {
            throw new IllegalArgumentException("unexpected selectionType: " + selectionType);
        }

        repositionOnSpace = true; // allows moving both the start and the end points with space down
        pixelSnapping = true;
        this.selectionType = selectionType;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        initSession(e, selectionType);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (selectionSession == null) {
            // the session is missing if the composition changed mid-drag; create it again
            dragStarted(e);
        }

        // check if the Alt key was pressed mid-drag
        boolean altDown = e.isAltDown();
        assert altDown == GlobalEvents.isAltDown() || AppMode.isUnitTesting()
            : "altDown = " + altDown + ", GlobalEvents.isAltDown() = " + GlobalEvents.isAltDown();

        // expand from center only if Alt wasn't already down at drag-start
        // (in that case Alt is used for shape combination, not expand-from-center)
        boolean expandFromCenter = !altUsedForCombinator && altDown;

        // if Alt is released mid-drag, it no longer means subtract/intersect for this drag
        if (!altDown) {
            altUsedForCombinator = false;
        }

        drag.setExpandedFromCenter(expandFromCenter);
        selectionSession.updateFromDrag(drag);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        // common logic in the base class
        handleDragFinished(e);
    }

    // altPressed() and altReleased() mirror the expand-from-center handling
    // in ongoingDrag(), but fire immediately on the key event since
    // there may be no mouse movement to trigger it otherwise
    @Override
    public void altPressed(boolean shiftDown) {
        if (!altUsedForCombinator && drag != null && drag.isDragging()) {
            drag.setExpandedFromCenter(true);
            if (selectionSession != null) {
                selectionSession.updateFromDrag(drag);
            }
        }
    }

    @Override
    public void altReleased(boolean shiftDown) {
        boolean wasAltCombinator = altUsedForCombinator;

        super.altReleased(shiftDown); // clears altUsedForCombinator

        if (!wasAltCombinator && drag != null && drag.isDragging()) {
            drag.setExpandedFromCenter(false);
            if (selectionSession != null) {
                selectionSession.updateFromDrag(drag);
            }
        }
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return selectionType == SelectionType.RECTANGLE
            ? ToolIcons::paintRectangleSelectionIcon
            : ToolIcons::paintEllipseSelectionIcon;
    }

    @Override
    public boolean checkInvariants() {
        super.checkInvariants();

        if (drag != null && drag.isDragging() && selectionSession == null) {
            throw new AssertionError("active drag without selectionSession");
        }

        return true;
    }
}
