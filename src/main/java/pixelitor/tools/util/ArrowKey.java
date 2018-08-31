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

/**
 * An enum-like collection of four static classes
 * that calculate the amount of keyboard nudging
 * in the move, selection and crop tools.
 */
public abstract class ArrowKey {
    public static class UP extends ArrowKey {
        public UP(boolean shiftDown) {
            super(shiftDown);
        }

        @Override
        protected int getDirX() {
            return 0;
        }

        @Override
        protected int getDirY() {
            return -1;
        }
    }

    public static class DOWN extends ArrowKey {
        public DOWN(boolean shiftDown) {
            super(shiftDown);
        }

        @Override
        protected int getDirX() {
            return 0;
        }

        @Override
        protected int getDirY() {
            return 1;
        }
    }

    public static class RIGHT extends ArrowKey {
        public RIGHT(boolean shiftDown) {
            super(shiftDown);
        }

        @Override
        protected int getDirX() {
            return 1;
        }

        @Override
        protected int getDirY() {
            return 0;
        }
    }

    public static class LEFT extends ArrowKey {
        public LEFT(boolean shiftDown) {
            super(shiftDown);
        }

        @Override
        protected int getDirX() {
            return -1;
        }

        @Override
        protected int getDirY() {
            return 0;
        }
    }

    /**
     * When Shift is pressed, everything is nudged faster.
     */
    private static final int SHIFT_MULTIPLIER = 10;

    private final boolean shiftDown;

    private ArrowKey(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public int getMoveX() {
        if (shiftDown) {
            return SHIFT_MULTIPLIER * getDirX();
        } else {
            return getDirX();
        }
    }

    public int getMoveY() {
        if (shiftDown) {
            return SHIFT_MULTIPLIER * getDirY();
        } else {
            return getDirY();
        }
    }

    public AffineTransform getTransform() {
        return AffineTransform.getTranslateInstance(getMoveX(), getMoveY());
    }

    @Override
    public String toString() {
        String simpleName = getClass().getSimpleName();
        return shiftDown ? "SHIFT" + simpleName : simpleName;
    }

    protected abstract int getDirX();

    protected abstract int getDirY();
}
