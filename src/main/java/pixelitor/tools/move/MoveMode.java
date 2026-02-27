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

package pixelitor.tools.move;

import pixelitor.history.ContentLayerMoveEdit;

/**
 * The type of moves available in the Move Tool.
 */
public enum MoveMode {
    MOVE_BOTH("Layer and Selection", "Move",
        true, true) {
    }, MOVE_SELECTION_ONLY("Selection Only", "Move Selection",
        true, false) {
    }, MOVE_LAYER_ONLY("Layer Only", ContentLayerMoveEdit.NAME,
        false, true) {
    };

    public static final String PRESET_KEY = "Move Mode";
    private final String displayName;
    private final String editName;
    private final boolean moveSelection;
    private final boolean moveLayer;

    MoveMode(String displayName, String editName, boolean moveSelection, boolean moveLayer) {
        this.displayName = displayName;
        this.editName = editName;
        this.moveSelection = moveSelection;
        this.moveLayer = moveLayer;
    }

    public boolean movesSelection() {
        return moveSelection;
    }

    public boolean movesLayer() {
        return moveLayer;
    }

    public String getEditName() {
        return editName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
