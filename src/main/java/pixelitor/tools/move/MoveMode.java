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

package pixelitor.tools.move;

public enum MoveMode {
    MOVE_BOTH("Layer and Selection", true, true) {
    }, MOVE_SELECTION_ONLY("Selection Only", true, false) {
    }, MOVE_LAYER_ONLY("Layer Only", false, true) {
    };

    private final String guiName;
    private final boolean moveSelection;
    private final boolean moveLayer;

    MoveMode(String guiName, boolean moveSelection, boolean moveLayer) {
        this.guiName = guiName;
        this.moveSelection = moveSelection;
        this.moveLayer = moveLayer;
    }

    public boolean movesTheSelection() {
        return moveSelection;
    }

    public boolean movesTheLayer() {
        return moveLayer;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
