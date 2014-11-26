/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor;

/**
 * Why is an image changed
 */
public enum ChangeReason {
    OP_WITHOUT_DIALOG(true) {
    }, OP_PREVIEW(false) {
    }, UNDO_REDO(false) {
    }, PERFORMANCE_TEST(false) {
    };

    private final boolean needsUndo;

    public boolean makeUndoBackup() {
        return needsUndo;
    }

    ChangeReason(boolean makeUndoBackup) {
        this.needsUndo = makeUndoBackup;
    }
}

