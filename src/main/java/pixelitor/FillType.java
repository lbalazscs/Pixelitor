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
package pixelitor;

import pixelitor.tools.FgBgColorSelector;
import pixelitor.utils.Utils;

import java.awt.Color;

/**
 * A fill color with a string description
 */
public enum FillType {
    WHITE {
        @Override
        public String toString() {
            return "White";
        }

        @Override
        public Color getColor() {
            return Color.WHITE;
        }
    }, BLACK {
        @Override
        public String toString() {
            return "Black";
        }

        @Override
        public Color getColor() {
            return Color.BLACK;
        }
    }, TRANSPARENT {
        @Override
        public String toString() {
            return "Transparent";
        }

        @Override
        public Color getColor() {
            return Utils.TRANSPARENT_COLOR;
        }
    }, FOREGROUND {
        @Override
        public String toString() {
            return "Foreground Color";
        }

        @Override
        public Color getColor() {
            return FgBgColorSelector.getFG();
        }
    }, BACKGROUND {
        @Override
        public String toString() {
            return "Background Color";
        }

        @Override
        public Color getColor() {
            return FgBgColorSelector.getBG();
        }
    };

    public abstract Color getColor();
}
