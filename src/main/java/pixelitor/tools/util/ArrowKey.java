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
    UP(false, VK_UP) {
        @Override
        public int getMoveX() {
            return 0;
        }

        @Override
        public int getMoveY() {
            return -1;
        }
    }, SHIFT_UP(true, VK_UP) {
        @Override
        public int getMoveX() {
            return 0;
        }

        @Override
        public int getMoveY() {
            return -SHIFT_MULTIPLIER;
        }
    }, DOWN(false, VK_DOWN) {
        @Override
        public int getMoveX() {
            return 0;
        }

        @Override
        public int getMoveY() {
            return 1;
        }
    }, SHIFT_DOWN(true, VK_DOWN) {
        @Override
        public int getMoveX() {
            return 0;
        }

        @Override
        public int getMoveY() {
            return SHIFT_MULTIPLIER;
        }
    }, RIGHT(false, VK_RIGHT) {
        @Override
        public int getMoveX() {
            return 1;
        }

        @Override
        public int getMoveY() {
            return 0;
        }
    }, SHIFT_RIGHT(true, VK_RIGHT) {
        @Override
        public int getMoveX() {
            return SHIFT_MULTIPLIER;
        }

        @Override
        public int getMoveY() {
            return 0;
        }
    }, LEFT(false, VK_LEFT) {
        @Override
        public int getMoveX() {
            return -1;
        }

        @Override
        public int getMoveY() {
            return 0;
        }
    }, SHIFT_LEFT(true, VK_LEFT) {
        @Override
        public int getMoveX() {
            return -SHIFT_MULTIPLIER;
        }

        @Override
        public int getMoveY() {
            return 0;
        }
    };

    /**
     * When Shift is pressed, everything is nudged faster.
     */
    private static final int SHIFT_MULTIPLIER = 10;

    private final boolean shiftDown;
    private final int keyCode;

    ArrowKey(boolean shiftDown, int keyCode) {
        this.shiftDown = shiftDown;
        this.keyCode = keyCode;
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
     * Returns the amount of nudging in the x direction
     */
    public abstract int getMoveX();

    public abstract int getMoveY();

    public AffineTransform getTransform() {
        return AffineTransform.getTranslateInstance(getMoveX(), getMoveY());
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
