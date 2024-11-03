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

import pixelitor.colors.Colors;

import java.awt.Color;

/**
 * A palette that produces variations around a single color
 */
public class VariationsPalette extends Palette {
    // static palette-specific variables so that they
    // are remembered between dialog sessions
    private static int lastRowCount = 7;
    private static int lastColumnCount = 10;

    private final float refHue;
    private static final float MAX_HUE_DEVIATION = 0.1f;
    private final String dialogTitle;

    public VariationsPalette(Color refColor, String dialogTitle) {
        super(lastRowCount, lastColumnCount);
        this.dialogTitle = dialogTitle;
        assert refColor != null;
        float[] hsb = Colors.toHSB(refColor);
        refHue = hsb[0];
        config = new HueSatPaletteConfig(0, hsb[1]);
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
                float bri = (col + 1) / (float) columnCount;
                if (rowCount == 1) {
                    color = Color.getHSBColor(hueOffset + refHue, saturation, bri);
                } else {
                    color = Color.getHSBColor(hue, saturation, bri);
                }
                panel.addButton(col, row, color);
            }
        }
    }

    private float calcHue(int y, float hueOffset) {
        float startHue = refHue - MAX_HUE_DEVIATION;
        if (startHue < 0) {
            startHue += 1.0f;
        }
        float hueStep = calcHueStep();
        float hue = hueOffset + startHue + y * hueStep;
        if (hue > 1.0f) {
            hue = hue - 1.0f;
        }
        return hue;
    }

    private float calcHueStep() {
        // the total hue range (2 * MAX_HUE_DEVIATION) is
        // divided into numRows - 1 equal parts
        return 2 * MAX_HUE_DEVIATION / (rowCount - 1);
    }

    @Override
    public void setGridSize(int rows, int columns) {
        super.setGridSize(rows, columns);
        lastColumnCount = columns;
        lastRowCount = rows;
    }

    @Override
    public String getDialogTitle() {
        return dialogTitle;
    }
}
