/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.GlobalEvents;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.UserDrag;

import java.awt.Cursor;
import java.awt.Graphics2D;

/**
 * A tool where where only the drag start and end positions
 * matter, and not the intermediate mouse point positions.
 */
public abstract class DragTool extends Tool {
    protected UserDrag userDrag;

    private boolean endPointInitialized = false;
    protected boolean spaceDragStartPoint = false;

    // subclasses will automatically support constrained
    // movement when Shift is pressed if this is set to true
    private final boolean constrainIfShiftDown;

    protected DragTool(String name, char activationKeyChar, String iconFileName,
                       String toolMessage, Cursor cursor,
                       boolean constrainIfShiftDown, ClipStrategy clipStrategy) {

        super(name, activationKeyChar, iconFileName, toolMessage, cursor, clipStrategy);

        this.constrainIfShiftDown = constrainIfShiftDown;
    }

    @Override
    public void mousePressed(PMouseEvent e) {
        userDrag = new UserDrag();
        userDrag.setStart(e);

        dragStarted(e);

        endPointInitialized = false;
    }

    @Override
    public void mouseDragged(PMouseEvent e) {
        if (userDrag.isCanceled()) {
            return;
        }
        if (spaceDragStartPoint) {
            userDrag.saveEndValues();
        }
        if (constrainIfShiftDown) {
            userDrag.setConstrained(e.isShiftDown());
        }

        userDrag.setEnd(e);

        if (spaceDragStartPoint) {
            if (endPointInitialized && GlobalEvents.isSpaceDown()) {
                userDrag.adjustStartForSpaceDownDrag();
            }

            endPointInitialized = true;
        }

        ongoingDrag(e);
    }

    @Override
    public void mouseReleased(PMouseEvent e) {
        if (userDrag.isCanceled()) {
            return;
        }
        userDrag.setEnd(e);
        userDrag.mouseReleased();
        dragFinished(e);
        endPointInitialized = false;
    }

    public abstract void dragStarted(PMouseEvent e);

    public abstract void ongoingDrag(PMouseEvent e);

    public abstract void dragFinished(PMouseEvent e);

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp) {
        if (userDrag == null || !userDrag.isDragging()) {
            return;
        }

        getDragDisplayType().draw(g2, userDrag);
    }

    public DragDisplayType getDragDisplayType() {
        return DragDisplayType.WIDTH_HEIGHT;
    }
}
