/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.colors.palette;

import java.awt.Color;

/**
 * The full color palette tries to show as many colors
 * as possible (as opposed to other palettes that concentrate
 * on specific mixes or variations).
 */
public class FullPalette extends Palette {
    // static palette-specific variables so that they
    // are remembered between dialog sessions
    private static int lastRowCount = 11;
    private static int lastColumnCount = 7;

    private int hueSteps;

    public FullPalette() {
        super(lastRowCount, lastColumnCount);

        config = new HueSatPaletteConfig(0, 0.9f); // default saturation is 90%
    }

    @Override
    public void setDimensions(int rows, int columns) {
        super.setDimensions(rows, columns);
        hueSteps = rows - 1;
        lastColumnCount = columns;
        lastRowCount = rows;
    }

    @Override
    public void addButtons(PalettePanel panel) {
        var hsConfig = (HueSatPaletteConfig) config;
        float hueOffset = hsConfig.getHueOffset();
        float saturation = hsConfig.getSaturation();

        for (int row = 0; row < rowCount; row++) {
            float hue = calcHue(row, hueOffset);
            for (int col = 0; col < columnCount; col++) {
                Color color;
                if (row == 0) { // first, grayscale row
                    float bri = (col + 1) / (float) columnCount;
                    color = Color.getHSBColor(0, 0, bri);
                } else { // color rows
                    float bri = (col + 1) / (float) columnCount;
                    color = Color.getHSBColor(hue, saturation, bri);
                }
                panel.addButton(col, row, color);
            }
        }
    }

    private float calcHue(int row, float hueOffset) {
        float hue = hueOffset + (row - 1) / (float) hueSteps;
        if (hue > 1.0f) {
            hue = hue - 1.0f;
        }
        return hue;
    }

    @Override
    public String getDialogTitle() {
        return "Color Palette";
    }
}
