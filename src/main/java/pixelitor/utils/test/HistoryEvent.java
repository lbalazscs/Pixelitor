/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.utils.test;

import pixelitor.history.PixelitorEdit;

/**
 * An event that is generated when there is a change to the history
 */
public class HistoryEvent extends PixelitorEvent {
    public HistoryEvent(PixelitorEdit edit) {
        super("    [EDIT] " + edit.getDebugName());
    }

    private HistoryEvent(String description) {
        super(description);
    }

    public static HistoryEvent createUndoEvent() {
        return new HistoryEvent("    [UNDO]");
    }

    public static HistoryEvent createRedoEvent() {
        return new HistoryEvent("    [REDO]");
    }
}
