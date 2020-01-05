/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
 * Whether the pressing of the Alt key is simulated
 */
public enum Alt implements EventMaskModifier {
    YES {
        @Override
        public int modify(int in) {
            in |= MouseEvent.ALT_DOWN_MASK;
            return in;
        }
    }, NO {
        @Override
        public int modify(int in) {
            return in;
        }
    }
}
