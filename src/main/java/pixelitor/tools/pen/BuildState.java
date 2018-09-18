/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.pen;

/**
 * The state of a path while building it.
 */
public enum BuildState {
    // The state after a subpath is finished, but another is not started yet.
    // Also the starting state of a subpath
    NO_INTERACTION(false),

    // The two basic operations of building a path:
    DRAGGING_THE_CONTROL_OF_LAST(true), // the out control of last anchor is dragged out
    MOVING_TO_NEXT_ANCHOR(false), // there is a moving point

    // Drag and move events when the Ctrl or Alt key is down:
    DRAG_EDITING_PREVIOUS(true),  // a previous (anchor or control) point is dragged
    MOVE_EDITING_PREVIOUS(false); // moving, but the rubber band is not shown

    private final boolean dragging;

    BuildState(boolean dragging) {
        this.dragging = dragging;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isMoving() {
        return !dragging;
    }
}
