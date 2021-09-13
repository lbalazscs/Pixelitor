/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
package pixelitor.tools.gradient;

import java.awt.Color;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * The color option in the gradient tool
 */
public enum GradientColorType {
    FG_TO_BG("Foreground to Background", false) {
        @Override
        protected Color getFirstColor() {
            return getFGColor();
        }

        @Override
        protected Color getSecondColor() {
            return getBGColor();
        }
    }, FG_TO_TRANSPARENT("Foreground to Transparent", true) {
        @Override
        protected Color getFirstColor() {
            return getFGColor();
        }

        @Override
        protected Color getSecondColor() {
            int fg = getFGColor().getRGB();
            int fgTransparent = fg & 0x00_FF_FF_FF;
            return new Color(fgTransparent, true);
        }
    }, BLACK_TO_WHITE("Black to White", false) {
        @Override
        protected Color getFirstColor() {
            return BLACK;
        }

        @Override
        protected Color getSecondColor() {
            return WHITE;
        }
    };

    private final String guiName;
    private final boolean hasTransparency;

    GradientColorType(String guiName, boolean hasTransparency) {
        this.guiName = guiName;
        this.hasTransparency = hasTransparency;
    }

    protected abstract Color getFirstColor();

    protected abstract Color getSecondColor();

    public Color getStartColor(boolean revert) {
        if (revert) {
            return getSecondColor();
        }
        return getFirstColor();
    }

    public Color getEndColor(boolean revert) {
        if (revert) {
            return getFirstColor();
        }
        return getSecondColor();
    }

    public boolean hasTransparency() {
        return hasTransparency;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
