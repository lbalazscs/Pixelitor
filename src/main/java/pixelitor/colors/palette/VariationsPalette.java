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

import pixelitor.colors.Colors;

import java.awt.Color;

/**
 * A palette that produces variations around a single reference color.
 * Columns vary the brightness, rows vary the hue.
 */
public class VariationsPalette extends DynamicPalette {
    private static final float MAX_HUE_DEVIATION = 0.1f;

    private final HueSatPaletteConfig hueSatConfig;
    private final float refHue;

    public VariationsPalette(Color refColor, String dialogTitle) {
        assert refColor != null;
        float[] hsb = Colors.toHsb(refColor);
        var hueSatConfig = new HueSatPaletteConfig(0, hsb[1]);
        // default grid: 7 hue variations × 10 brightness columns
        super(dialogTitle, hueSatConfig, 7, 10);
        this.refHue = hsb[0];
        this.hueSatConfig = hueSatConfig;
    }

    @Override
    protected Color createColor(int row, int col) {
        float hue = refHue + hueSatConfig.getHueOffset() + MAX_HUE_DEVIATION * calcRowOffset(row);
        float bri = calcColumnBrightness(col);
        return Color.getHSBColor(hue, hueSatConfig.getSaturation(), bri);
    }
}
