/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
 * Represents the reason for which a composition or a layer was copied.
 */
public enum CopyType {
    /**
     * A layer is duplicated
     */
    LAYER_DUPLICATE(true) {
        @Override
        public String createLayerDuplicateName(String orig) {
            return Utils.createCopyName(orig);
        }
    },
    /**
     * A composition or layer copy is needed for undo
     */
    UNDO(false) {
        @Override
        public String createLayerDuplicateName(String orig) {
            return orig;
        }
    },
    /**
     * A composition is duplicated (Image/Duplicate)
     */
    COMP_DUPLICATE(true) {
        @Override
        public String createLayerDuplicateName(String orig) {
            return orig;
        }
    },
    /**
     * A shallow duplicate of a smart layer.
     */
    SMART_OBJECT_CLONE(false) {
        @Override
        public String createLayerDuplicateName(String orig) {
            return orig + " clone";
        }
    };

    private final boolean deepContentCopy;

    CopyType(boolean deepContentCopy) {
        this.deepContentCopy = deepContentCopy;
    }

    public boolean doDeepContentCopy() {
        return deepContentCopy;
    }

    public abstract String createLayerDuplicateName(String orig);
}
