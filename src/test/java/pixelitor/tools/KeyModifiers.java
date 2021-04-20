/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
 * Represents the state of all key modifiers.
 */
public record KeyModifiers(Ctrl ctrl, Alt alt, Shift shift) implements EventMaskModifier {
    public static final KeyModifiers NONE = new KeyModifiers(Ctrl.NO, Alt.NO, Shift.NO);
    public static final KeyModifiers CTRL = new KeyModifiers(Ctrl.YES, Alt.NO, Shift.NO);
    public static final KeyModifiers ALT = new KeyModifiers(Ctrl.NO, Alt.YES, Shift.NO);
    public static final KeyModifiers SHIFT = new KeyModifiers(Ctrl.NO, Alt.NO, Shift.YES);

    @Override
    public int modify(int in) {
        in = ctrl.modify(in);
        in = alt.modify(in);
        in = shift.modify(in);
        return in;
    }
}
