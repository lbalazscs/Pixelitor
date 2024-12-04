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

package pixelitor.utils.input;

import pixelitor.gui.View;
import pixelitor.tools.Tools;
import pixelitor.tools.util.PMouseEvent;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Random;

import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_MOVED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;
import static java.awt.event.MouseEvent.MOUSE_RELEASED;

/**
 * Represents a combination of key and mouse modifiers for event testing.
 */
public record Modifiers(Ctrl ctrl, Alt alt, Shift shift, MouseButton button) implements EventMaskModifier {
    // common modifier combinations
    public static final Modifiers NONE = new Modifiers(Ctrl.RELEASED, Alt.RELEASED, Shift.RELEASED, MouseButton.LEFT);
    public static final Modifiers CTRL = new Modifiers(Ctrl.PRESSED, Alt.RELEASED, Shift.RELEASED, MouseButton.LEFT);
    public static final Modifiers ALT = new Modifiers(Ctrl.RELEASED, Alt.PRESSED, Shift.RELEASED, MouseButton.LEFT);
    public static final Modifiers SHIFT = new Modifiers(Ctrl.RELEASED, Alt.RELEASED, Shift.PRESSED, MouseButton.LEFT);

    @Override
    public int modify(int currentMask) {
        currentMask = ctrl.modify(currentMask);
        currentMask = alt.modify(currentMask);
        currentMask = shift.modify(currentMask);
        currentMask = button.modify(currentMask);
        return currentMask;
    }

    private MouseEvent createEvent(int x, int y, int id, Component source) {
        //noinspection MagicConstant
        return new MouseEvent(source, id, System.currentTimeMillis(),
            modify(0), x, y, 1, button == MouseButton.RIGHT);
    }

    public PMouseEvent createPEvent(int x, int y, int id, View view) {
        MouseEvent e = createEvent(x, y, id, view);
        return new PMouseEvent(e, view);
    }

    public void dispatchPressedEvent(int x, int y, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_PRESSED, view);
        Tools.EventDispatcher.mousePressed(e, view);
    }

    public void dispatchDraggedEvent(int x, int y, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_DRAGGED, view);
        Tools.EventDispatcher.mouseDragged(e, view);
    }

    public void dispatchReleasedEvent(int x, int y, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_RELEASED, view);
        Tools.EventDispatcher.mouseReleased(e, view);
    }

    public void dispatchMoveEvent(int x, int y, View view) {
        MouseEvent e = createEvent(x, y, MOUSE_MOVED, view);
        Tools.EventDispatcher.mouseMoved(e, view);
    }

    public static Modifiers randomly(Random rand) {
        Ctrl ctrl = Ctrl.randomly(rand);
        Alt alt = Alt.randomly(rand);
        Shift shift = Shift.randomly(rand);
        MouseButton button = MouseButton.randomly(rand);

        return new Modifiers(ctrl, alt, shift, button);
    }
}
