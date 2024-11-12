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

package pixelitor.tools;

import pixelitor.tools.shapes.ShapesTool;

/**
 * The possible states of a {@link DragTool}.
 */
public enum DragToolState {
    /**
     * The initial state and the state after finishing a tool action
     */
    IDLE {
        @Override
        public boolean isOK(ShapesTool tool) {
            return !tool.hasStyledShape() && !tool.hasBox();
        }
    },
    AFTER_FIRST_MOUSE_PRESS {
        @Override
        public boolean isOK(ShapesTool tool) {
            return tool.hasStyledShape() && !tool.hasBox();
        }
    },
    /**
     * The state during the initial drag (no handles yet).
     */
    INITIAL_DRAG {
        @Override
        public boolean isOK(ShapesTool tool) {
            return tool.hasStyledShape() && !tool.hasBox();
        }
    },
    /**
     * The state when the handles are shown.
     */
    TRANSFORM {
        @Override
        public boolean isOK(ShapesTool tool) {
            return tool.hasStyledShape() && tool.hasBox();
        }
    };

    // can be used in assertions
    public abstract boolean isOK(ShapesTool tool);
}
