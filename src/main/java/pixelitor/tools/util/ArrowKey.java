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

package pixelitor.tools.util;

import java.awt.geom.AffineTransform;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

/**
 * Represents an arrow key (that can be used for nudging),
 * possibly with a shift modifier (indicating a faster nudge).
 */
public enum ArrowKey {
    UP(false, VK_UP, 0, -1),
    SHIFT_UP(true, VK_UP, 0, -10),
    DOWN(false, VK_DOWN, 0, 1),
    SHIFT_DOWN(true, VK_DOWN, 0, 10),
    RIGHT(false, VK_RIGHT, 1, 0),
    SHIFT_RIGHT(true, VK_RIGHT, 10, 0),
    LEFT(false, VK_LEFT, -1, 0),
    SHIFT_LEFT(true, VK_LEFT, -10, 0);

    private final boolean shiftDown;
    private final int keyCode;
    private final int deltaX;
    private final int deltaY;

    ArrowKey(boolean shiftDown, int keyCode, int deltaX, int deltaY) {
        this.shiftDown = shiftDown;
        this.keyCode = keyCode;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public static ArrowKey up(boolean shift) {
        return shift ? SHIFT_UP : UP;
    }

    public static ArrowKey down(boolean shift) {
        return shift ? SHIFT_DOWN : DOWN;
    }

    public static ArrowKey right(boolean shift) {
        return shift ? SHIFT_RIGHT : RIGHT;
    }

    public static ArrowKey left(boolean shift) {
        return shift ? SHIFT_LEFT : LEFT;
    }

    /**
     * Returns the movement amount along the X-axis.
     */
    public int getDeltaX() {
        return deltaX;
    }

    /**
     * Returns the movement amount along the Y-axis.
     */
    public int getDeltaY() {
        return deltaY;
    }

    /**
     * Creates an AffineTransform representing this arrow key's movement.
     */
    public AffineTransform toTransform() {
        return AffineTransform.getTranslateInstance(getDeltaX(), getDeltaY());
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
