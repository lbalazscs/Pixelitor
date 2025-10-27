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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Random;

import static java.awt.event.KeyEvent.VK_ALT;
import static pixelitor.utils.input.Modifiers.ROBOT_DELAY_MS;

/**
 * Represents the pressed or released state of the Alt key.
 */
public enum Alt implements EventMaskModifier {
    PRESSED {
        @Override
        public int modify(int currentMask) {
            return currentMask | KeyEvent.ALT_DOWN_MASK;
        }
    }, RELEASED {
        @Override
        public int modify(int currentMask) {
            return currentMask;
        }
    };

    public boolean isDown() {
        return this == PRESSED;
    }

    public static Alt from(MouseEvent e) {
        return e.isAltDown() ? PRESSED : RELEASED;
    }

    public static Alt randomly(Random rand) {
        return rand.nextBoolean() ? PRESSED : RELEASED;
    }

    public Alt press(Robot robot) {
        if (this == PRESSED) {
            robot.keyPress(VK_ALT);
            robot.delay(ROBOT_DELAY_MS);
        }
        return this;
    }

    public void release(Robot robot) {
        if (this == PRESSED) {
            robot.keyRelease(VK_ALT);
            robot.delay(ROBOT_DELAY_MS);
        }
    }
}
