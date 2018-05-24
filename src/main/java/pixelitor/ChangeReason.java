/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
 * The reason for an image change
 */
public enum ChangeReason {
    FILTER_WITHOUT_DIALOG(true, false) {
    }, REPEAT_LAST(true, false) {
    },

    // can't be called "PREVIEW", because it gets
    // confused with the ImageLayer's PREVIEW state
    // because of the static imports...
    PREVIEWING(false, true) {

    }, PERFORMANCE_TEST(false, false) {
    }, NORMAL_TEST(true, true) { 

    }, TWEEN_PREVIEW(false, true) {
    }, BATCH_AUTOMATE(false, false) {
    };

    private final boolean makeUndoBackup;

    // whether this is only a preview during a dialog session
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

