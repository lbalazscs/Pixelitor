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

package pixelitor.tools;

/**
 * The possible states of a {@link DragTool}.
 */
public enum DragToolState {
    /**
     * The initial state and the state after finishing a tool action
     */
    IDLE,
    /**
     * A transient state after the mouse is pressed but before a drag has officially started.
     * Can be useful for distinguishing between a click and a drag.
     */
    AFTER_FIRST_MOUSE_PRESS,
    /**
     * The state during the initial drag (no handles yet).
     */
    INITIAL_DRAG,
    /**
     * The state when the handles are shown.
     */
    TRANSFORM
}
