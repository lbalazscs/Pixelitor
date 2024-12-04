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

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * Represents which mouse button is being simulated in a {@link MouseEvent}.
 */
public enum MouseButton implements EventMaskModifier {
    LEFT(InputEvent.BUTTON1_DOWN_MASK),
    RIGHT(InputEvent.BUTTON3_DOWN_MASK);

    private final int buttonMask;

    MouseButton(int buttonMask) {
        this.buttonMask = buttonMask;
    }

    @Override
    public int modify(int currentMask) {
        return currentMask | buttonMask;
    }

    public boolean isRight() {
        return this == RIGHT;
    }

    public MouseButton press(Robot robot) {
        robot.mousePress(buttonMask);
        robot.delay(50);

        return this;
    }

    public void release(Robot robot) {
        robot.mouseRelease(buttonMask);
        robot.delay(50);
    }

    public static MouseButton randomly(Random rand) {
        return rand.nextBoolean() ? LEFT : RIGHT;
    }
}
