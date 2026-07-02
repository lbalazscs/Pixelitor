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

import pixelitor.tools.crop.CropTool;
import pixelitor.tools.gradient.GradientTool;
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
        public boolean checkInvariants(ShapesTool tool) {
            return !tool.hasStyledShape() && !tool.hasBox();
        }

        @Override
        public boolean checkInvariants(CropTool tool) {
            return !tool.hasCropBox() && !tool.isCropEnabled();
        }

        @Override
        public boolean checkInvariants(GradientTool tool) {
            return !tool.hasHandles();
        }
    },
    /**
     * A transient state after the mouse is pressed but before a drag has officially started.
     * Can be useful for distinguishing between a click and a drag.
     */
    AFTER_FIRST_MOUSE_PRESS {
        @Override
        public boolean checkInvariants(ShapesTool tool) {
            return tool.hasStyledShape() && !tool.hasBox();
        }

        @Override
        public boolean checkInvariants(CropTool tool) {
            return !tool.hasCropBox();
        }

        @Override
        public boolean checkInvariants(GradientTool tool) {
            // handles might be present (if the user clicked a handle or clicked
            // outside an active gradient) or absent (first gradient interaction)
            return true;
        }
    },
    /**
     * The state during the initial drag (no handles yet).
     */
    INITIAL_DRAG {
        @Override
        public boolean checkInvariants(ShapesTool tool) {
            return tool.hasStyledShape() && !tool.hasBox();
        }

        @Override
        public boolean checkInvariants(CropTool tool) {
            return !tool.hasCropBox();
        }

        @Override
        public boolean checkInvariants(GradientTool tool) {
            return !tool.hasHandles();
        }
    },
    /**
     * The state when the handles are shown.
     */
    TRANSFORM {
        @Override
        public boolean checkInvariants(ShapesTool tool) {
            return tool.hasStyledShape() && tool.hasBox();
        }

        @Override
        public boolean checkInvariants(CropTool tool) {
            return tool.hasCropBox() && tool.isCropEnabled();
        }

        @Override
        public boolean checkInvariants(GradientTool tool) {
            return tool.hasHandles();
        }
    };

    // these are used for assertions to verify that the tool's internal state is consistent
    public abstract boolean checkInvariants(ShapesTool tool);

    public abstract boolean checkInvariants(CropTool tool);

    public abstract boolean checkInvariants(GradientTool tool);
}
