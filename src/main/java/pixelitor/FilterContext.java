/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
 * The context in which a filter runs.
 */
public enum FilterContext {
    FILTER_WITHOUT_DIALOG(true, false) {
    }, REPEAT_LAST(true, false) {
    },

    // can't be named "PREVIEW", because it would conflict
    // with the ImageLayer's PREVIEW state after static imports
    PREVIEWING(false, true) {

    }, TWEEN_PREVIEW(false, true) {
    }, BATCH_AUTOMATE(false, false) {
    };

    private final boolean makeUndoBackup;

    // whether this is only a preview during a dialog session
    private final boolean preview;

    FilterContext(boolean makeUndoBackup, boolean preview) {
        this.makeUndoBackup = makeUndoBackup;
        this.preview = preview;
    }

    public boolean needsUndo() {
        return makeUndoBackup;
    }

    public boolean isPreview() {
        return preview;
    }
}
