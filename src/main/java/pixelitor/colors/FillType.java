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
package pixelitor.colors;

import pixelitor.filters.Fill;
import pixelitor.filters.util.FilterAction;
import pixelitor.gui.GUIText;

import java.awt.Color;

import static pixelitor.colors.Colors.TRANSPARENT_COLOR;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.utils.Texts.i18n;

/**
 * A fill color with a string description
 */
public enum FillType {
    WHITE(i18n("white")) {
        @Override
        public Color getColor() {
            return Color.WHITE;
        }
    }, BLACK(i18n("black")) {
        @Override
        public Color getColor() {
            return Color.BLACK;
        }
    }, TRANSPARENT("Transparent") {
        @Override
        public Color getColor() {
            return TRANSPARENT_COLOR;
        }
    }, FOREGROUND(GUIText.FG_COLOR) {
        @Override
        public Color getColor() {
            return getFGColor();
        }
    }, BACKGROUND(GUIText.BG_COLOR) {
        @Override
        public Color getColor() {
            return getBGColor();
        }
    };

    private final String guiName;

    FillType(String guiName) {
        this.guiName = guiName;
    }

    public abstract Color getColor();

    @Override
    public String toString() {
        return guiName;
    }

    public FilterAction asFillFilterAction() {
        return new FilterAction(guiName, () -> new Fill(this)).noGUI();
    }
}
