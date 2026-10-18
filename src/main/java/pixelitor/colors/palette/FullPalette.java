/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
 * A palette that shows as many colors as possible (as opposed to
 * other palettes that concentrate on specific mixes or variations).
 */
public class FullPalette extends DynamicPalette {
    public static final float DEFAULT_SATURATION = 0.9f; // default saturation is 90%

    private final HueSatPaletteConfig hueSatConfig;

    public FullPalette(String dialogTitle) {
        var hueSatConfig = new HueSatPaletteConfig(0, DEFAULT_SATURATION);
        // default grid: 11 rows (1 grayscale row + 10 hue steps) × 7 brightness columns
        super(dialogTitle, hueSatConfig, 11, 7);
        this.hueSatConfig = hueSatConfig;
    }

    @Override
    protected Color createColor(int row, int col) {
        float bri = calcColumnBrightness(col);
        if (row == 0) {
            return Color.getHSBColor(0, 0, bri); // grayscale row
        }
        int hueSteps = numRows - 1; // row >= 1 here, so numRows >= 2
        float hue = hueSatConfig.getHueOffset() + (row - 1) / (float) hueSteps;
        return Color.getHSBColor(hue, hueSatConfig.getSaturation(), bri);
    }
}
