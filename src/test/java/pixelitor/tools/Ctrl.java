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

package pixelitor.tools;

import java.awt.event.MouseEvent;

/**
 * Whether we simulate the pressing of the Ctrl key
 * during testing
 */
public enum Ctrl implements EventMaskModifier {
    YES {
        @Override
        public int modify(int in) {
            in |= MouseEvent.CTRL_DOWN_MASK;
            in |= MouseEvent.CTRL_MASK;

            return in;
        }
    }, NO {
        @Override
        public int modify(int in) {
            return in;
        }
    }
}
