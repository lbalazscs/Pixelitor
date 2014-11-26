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
package pixelitor.tools;

import java.awt.Color;

/**
 *
 */
public enum GradientColorType {
    FG_TO_BG {
        @Override
        public String toString() {
            return "Foreground to Background";
        }

        @Override
        public Color getStartColor() {
            if (isInverted()) {
                return FgBgColorSelector.getBG();
            }
            return FgBgColorSelector.getFG();
        }

        @Override
        public Color getEndColor() {
            if (isInverted()) {
                return FgBgColorSelector.getFG();
            }
            return FgBgColorSelector.getBG();
        }
    },
    FG_TO_TRANSPARENT {
        private final Color transparentColor = new Color(0, 0, 0, 0);

        @Override
        public String toString() {
            return "Foreground to Transparent";
        }

        @Override
        public Color getStartColor() {
            if (isInverted()) {
                return transparentColor;
            }
            return FgBgColorSelector.getFG();
        }

        @Override
        public Color getEndColor() {
            if (isInverted()) {
                return FgBgColorSelector.getFG();
            }
            return transparentColor;
        }
    },
    BLACK_TO_WHITE {
        @Override
        public String toString() {
            return "Black to White";
        }

        @Override
        public Color getStartColor() {
            if (isInverted()) {
                return Color.WHITE;
            }
            return Color.BLACK;
        }

        @Override
        public Color getEndColor() {
            if (isInverted()) {
                return Color.BLACK;
            }
            return Color.WHITE;
        }
    };

    public abstract Color getStartColor();

    public abstract Color getEndColor();


    public boolean isInverted() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    private boolean invert;
}
