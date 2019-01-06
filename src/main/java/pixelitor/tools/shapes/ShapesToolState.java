/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.shapes;

/**
 * The current state of the crop tool
 *
 * TODO possibly it could be merged with CropToolState
 */
public enum ShapesToolState {
    /**
     * The initial state and the state after Esc
     */
    NO_INTERACTION {
        @Override
        public boolean isOK(ShapesTool tool) {
            return tool.getStyledShape() == null && tool.getTransformBox() == null;
        }
    },
    /**
     * The state during the initial drag (no transform box)
     */
    INITIAL_DRAG {
        @Override
        public boolean isOK(ShapesTool tool) {
            return tool.getStyledShape() != null && tool.getTransformBox() == null;
        }
    },
    /**
     * The state when transform box is visible
     */
    TRANSFORM {
        @Override
        public boolean isOK(ShapesTool tool) {
            return tool.getStyledShape() != null && tool.getTransformBox() != null;
        }
    };

    // can be used in assertions
    public abstract boolean isOK(ShapesTool tool);
}
