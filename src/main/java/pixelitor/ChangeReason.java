/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

/**
 * Why is an image changed...
 */
public enum ChangeReason {
    OP_WITHOUT_DIALOG(true, false) {
    }, OP_PREVIEW(false, true) {
    }, PERFORMANCE_TEST(false, false) {
    }, TWEEN_PREVIEW(false, true) {
    }, BATCH_AUTOMATE(false, false) {
    };

    private final boolean makeUndoBackup;
    private final boolean preview;

    public boolean needsUndo() {
        return makeUndoBackup;
    }

    public boolean isPreview() {
        return preview;
    }

    ChangeReason(boolean makeUndoBackup, boolean preview) {
        this.makeUndoBackup = makeUndoBackup;
        this.preview = preview;
    }
}

