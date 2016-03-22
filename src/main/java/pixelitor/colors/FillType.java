/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.FilterAction;

import java.awt.Color;

import static pixelitor.colors.ColorUtils.TRANSPARENT_COLOR;

/**
 * A fill color with a string description
 */
public enum FillType {
    WHITE("White") {
        @Override
        public Color getColor() {
            return Color.WHITE;
        }
    }, BLACK("Black") {
        @Override
        public Color getColor() {
            return Color.BLACK;
        }
    }, TRANSPARENT("Transparent") {
        @Override
        public Color getColor() {
            return TRANSPARENT_COLOR;
        }
    }, FOREGROUND("Foreground Color") {
        @Override
        public Color getColor() {
            return FgBgColors.getFG();
        }
    }, BACKGROUND("Background Color") {
        @Override
        public Color getColor() {
            return FgBgColors.getBG();
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

    public FilterAction createFillFilterAction() {
        return new FilterAction(guiName, () -> new Fill(this))
                .withoutGUI()
                .withFillListName();
    }
}
