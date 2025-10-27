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

package pixelitor.utils.input;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Random;

import static pixelitor.utils.input.Modifiers.ROBOT_DELAY_MS;

/**
 * Represents which mouse button is being simulated in a {@link MouseEvent}.
 */
public enum MouseButton implements EventMaskModifier {
    LEFT(InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1),
    RIGHT(InputEvent.BUTTON3_DOWN_MASK, MouseEvent.BUTTON3);

    private final int buttonMask;
    private final int awtButton;

    MouseButton(int buttonMask, int awtButton) {
        this.buttonMask = buttonMask;
        this.awtButton = awtButton;
    }

    @Override
    public int modify(int currentMask) {
        return currentMask | buttonMask;
    }

    public boolean isRight() {
        return this == RIGHT;
    }

    public int getAwtButton() {
        return awtButton;
    }

    public MouseButton press(Robot robot) {
        robot.mousePress(buttonMask);
        robot.delay(ROBOT_DELAY_MS);

        return this;
    }

    public void release(Robot robot) {
        robot.mouseRelease(buttonMask);
        robot.delay(ROBOT_DELAY_MS);
    }

    public static MouseButton randomly(Random rand) {
        return rand.nextBoolean() ? LEFT : RIGHT;
    }
}
