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

import pixelitor.colors.Colors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A palette that produces variations around a single reference color.
 */
public class VariationsPalette extends DynamicPalette {
    // static palette-specific fields are remembered between dialog sessions
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
    public List<Color> getColors() {
        List<Color> colors = new ArrayList<>(rowCount * columnCount);
        var hsConfig = (HueSatPaletteConfig) config;
        float hueOffset = hsConfig.getHueOffset();
        float saturation = hsConfig.getSaturation();

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                float bri = (col + 1) / (float) columnCount;
                float hue;

                if (rowCount == 1) {
                    // for a single row, only vary brightness, not hue
                    hue = hueOffset + refHue;
                } else {
                    hue = calcHue(row, hueOffset);
                }
                Color color = Color.getHSBColor(hue, saturation, bri);
                colors.add(color);
            }
        }
        return colors;
    }

    private float calcHue(int y, float hueOffset) {
        float startHue = refHue - MAX_HUE_DEVIATION;
        if (startHue < 0) {
            startHue += 1.0f;
        }
        float hueStep = calcHueStep();
        float hue = hueOffset + startHue + y * hueStep;
        if (hue > 1.0f) {
            hue -= 1.0f;
        }
        return hue;
    }

    private float calcHueStep() {
        if (rowCount <= 1) {
            return 0.0f; // avoid division by zero
        }
        // the total hue range (2 * MAX_HUE_DEVIATION) is
        // divided into (rowCount - 1) parts
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
