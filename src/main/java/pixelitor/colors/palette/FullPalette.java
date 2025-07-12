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

package pixelitor.colors.palette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A palette that shows as many colors as possible (as opposed to
 * other palettes that concentrate on specific mixes or variations).
 */
public class FullPalette extends DynamicPalette {
    // static palette-specific fields are remembered between dialog sessions
    private static int lastRowCount = 11;
    private static int lastColumnCount = 7;

    private final String dialogTitle;

    private int hueSteps;

    public FullPalette(String dialogTitle) {
        super(lastRowCount, lastColumnCount);
        this.dialogTitle = dialogTitle;

        config = new HueSatPaletteConfig(0, 0.9f); // default saturation is 90%
    }

    @Override
    public void setGridSize(int rows, int columns) {
        super.setGridSize(rows, columns);

        // one row is for grayscale, the rest for hues
        hueSteps = Math.max(1, rows - 1);
        lastColumnCount = columns;
        lastRowCount = rows;
    }

    @Override
    public List<Color> getColors() {
        var hsConfig = (HueSatPaletteConfig) config;
        float hueOffset = hsConfig.getHueOffset();
        float saturation = hsConfig.getSaturation();

        List<Color> colors = new ArrayList<>(rowCount * columnCount);

        for (int row = 0; row < rowCount; row++) {
            float hue = calcHue(row, hueOffset);
            for (int col = 0; col < columnCount; col++) {
                Color color;
                if (row == 0) { // first row is grayscale
                    float bri = (col + 1) / (float) columnCount;
                    color = Color.getHSBColor(0, 0, bri);
                } else { // subsequent rows are colored
                    float bri = (col + 1) / (float) columnCount;
                    color = Color.getHSBColor(hue, saturation, bri);
                }
                colors.add(color);
            }
        }
        return colors;
    }

    // calculates hue for a given row, skipping the first (grayscale) row
    private float calcHue(int row, float hueOffset) {
        float hue = hueOffset + (row - 1) / (float) hueSteps;
        return hue % 1.0f;
    }

    @Override
    public String getDialogTitle() {
        return dialogTitle;
    }
}