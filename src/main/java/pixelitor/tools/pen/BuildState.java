/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
 * The various states in the process of building a path.
 */
public enum BuildState {
    // The starting empty state when there is no interaction.
    // Also the state after a subpath is finished, but another is not started yet.
    IDLE(false),

    // States during the two primary path-building operations:
    DRAGGING_LAST_CONTROL(true), // Dragging out the out-control of the last anchor.
    MOVING_TO_NEXT_ANCHOR(false), // Moving to position the next anchor point.

    // States while editing with modifier keys (Ctrl or Alt):
    DRAG_EDITING_PREVIOUS(true),  // A previous anchor or control point is being dragged.
    MOVE_EDITING_PREVIOUS(false); // Moving without showing the rubber band.

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
