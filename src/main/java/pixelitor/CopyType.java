/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor;

import pixelitor.utils.Utils;

/**
 * Represents the reason for copying a composition or layer.
 */
public enum CopyType {
    /**
     * A layer is being duplicated.
     */
    DUPLICATE_LAYER(true) {
        @Override
        public String createLayerCopyName(String originalName) {
            return Utils.createCopyName(originalName);
        }
    },
    /**
     * A composition or layer copy is needed for undo.
     */
    UNDO(false) {
        @Override
        public String createLayerCopyName(String originalName) {
            return originalName;
        }
    },
    /**
     * A composition is being duplicated (Image/Duplicate).
     */
    DUPLICATE_COMP(true) {
        @Override
        public String createLayerCopyName(String originalName) {
            return originalName;
        }
    },
    /**
     * A shallow duplicate of a smart layer.
     */
    CLONE_SMART_OBJECT(false) {
        @Override
        public String createLayerCopyName(String originalName) {
            return originalName + " clone";
        }
    };

    private final boolean deepContentCopy;

    CopyType(boolean deepContentCopy) {
        this.deepContentCopy = deepContentCopy;
    }

    public boolean isDeepContentCopy() {
        return deepContentCopy;
    }

    /**
     * Generates a name for the copied layer based on the original name.
     */
    public abstract String createLayerCopyName(String originalName);
}
