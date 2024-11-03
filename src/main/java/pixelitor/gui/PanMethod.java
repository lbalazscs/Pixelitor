/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui;

import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.AppPreferences;

public enum PanMethod {
    SPACE_DRAG("Space-drag", "sd") {
        @Override
        public boolean initPan(PMouseEvent e) {
            return GlobalEvents.isSpaceDown();
        }
    }, MIDDLE_MOUSE("Middle-mouse Drag", "mmd") {
        @Override
        public boolean initPan(PMouseEvent e) {
            return e.isMiddle();
        }
    };

    private final String displayName;
    private final String saveCode;

    PanMethod(String displayName, String saveCode) {
        this.displayName = displayName;
        this.saveCode = saveCode;
    }

    public static boolean ignoreSpace() {
        return CURRENT != SPACE_DRAG;
    }

    public abstract boolean initPan(PMouseEvent e);

    public static PanMethod CURRENT = SPACE_DRAG;

    public static void load() {
        String loadedCode = AppPreferences.loadPan();

        for (PanMethod method : values()) {
            if (method.saveCode().equals(loadedCode)) {
                CURRENT = method;
                break;
            }
        }
    }

    public static void changeTo(PanMethod newMethod) {
        CURRENT = newMethod;
    }

    public String saveCode() {
        return saveCode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
