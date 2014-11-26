/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools.shapestool;

import java.awt.BasicStroke;

/**
 * An enum wrapper around the join argument of a BasicStroke constructor
 */
enum BasicStrokeJoin {
    ROUND(BasicStroke.JOIN_ROUND) {
        @Override
        public String toString() {
            return "Round";
        }
    }, BEVEL(BasicStroke.JOIN_BEVEL) {
        @Override
        public String toString() {
            return "Bevel";
        }
    }, MITER(BasicStroke.JOIN_MITER) {
        @Override
        public String toString() {
            return "Miter";
        }
    };
    private final int value;

    BasicStrokeJoin(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
