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

package pixelitor.tools;

/**
 * Represents a combination of modifier key states for event testing.
 */
public record KeyModifiers(Ctrl ctrl, Alt alt, Shift shift) implements EventMaskModifier {
    // common modifier combinations
    public static final KeyModifiers NONE = new KeyModifiers(Ctrl.RELEASED, Alt.RELEASED, Shift.RELEASED);
    public static final KeyModifiers CTRL = new KeyModifiers(Ctrl.PRESSED, Alt.RELEASED, Shift.RELEASED);
    public static final KeyModifiers ALT = new KeyModifiers(Ctrl.RELEASED, Alt.PRESSED, Shift.RELEASED);
    public static final KeyModifiers SHIFT = new KeyModifiers(Ctrl.RELEASED, Alt.RELEASED, Shift.PRESSED);

    @Override
    public int modify(int currentMask) {
        currentMask = ctrl.modify(currentMask);
        currentMask = alt.modify(currentMask);
        currentMask = shift.modify(currentMask);
        return currentMask;
    }
}
