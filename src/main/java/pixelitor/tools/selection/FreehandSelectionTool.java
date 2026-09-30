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
import pixelitor.tools.util.OverlayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.Graphics2D;
import java.util.function.Consumer;

/**
 * A tool that creates freehand selections by dragging.
 */
public class FreehandSelectionTool extends AbstractSelectionTool {
    public FreehandSelectionTool() {
        // the freehand and polygonal selection tools share the 'L' hotkey, with cycling
        super("Freehand Selection", 'L',
            "drag around the area that you want to select.",
            Cursors.DEFAULT, false);
        repositionOnSpace = false;
        pixelSnapping = true;
    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        initSession(e, SelectionType.FREEHAND);
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {
        if (selectionSession == null) {
            // the session is missing if the composition changed mid-drag; create it again
            dragStarted(e);
        }

        boolean altDown = e.isAltDown();
        assert altDown == GlobalEvents.isAltDown() || AppMode.isUnitTesting()
            : "altDown = " + altDown + ", GlobalEvents.isAltDown() = " + GlobalEvents.isAltDown();

        // if Alt is released mid-drag, it no longer means subtract/intersect for this drag
        if (!altDown) {
            altUsedForCombinator = false;
        }

        // extend the draft path with the current point
        selectionSession.updateFromDrag(drag);
    }

    @Override
    protected void dragFinished(PMouseEvent e) {
        // common logic in the base class
        handleDragFinished(e);
    }

    @Override
    protected OverlayType getOverlayType() {
        // no measurement overlay for freehand drawing
        return OverlayType.NONE;
    }

    @Override
    public Consumer<Graphics2D> createIconPainter() {
        return ToolIcons::paintFreehandSelectionIcon;
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
