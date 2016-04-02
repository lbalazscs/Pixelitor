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

package pixelitor.colors.palette;

import pixelitor.colors.ColorUtils;

import java.awt.Color;

public class VariationsPalette extends Palette {
    private static int lastRows = 7;
    private static int lastCols = 10;

    private final float refHue;
    private static final float MAX_HUE_DEVIATION = 0.1f;
    private final String dialogTitle;

    public VariationsPalette(Color refColor, String dialogTitle) {
        super(lastRows, lastCols);
        this.dialogTitle = dialogTitle;
        assert refColor != null;
        float[] hsb = ColorUtils.colorToHSB(refColor);
        refHue = hsb[0];
        config = new HueSatPaletteConfig(0, hsb[1]);
    }

    @Override
    public void addButtons(VariationsPanel panel) {
        HueSatPaletteConfig hsp = (HueSatPaletteConfig) config;
        float hueShift = hsp.getHueShift();
        float saturation = hsp.getSaturation();

        for (int y = 0; y < numRows; y++) {
            float hue = calcHue(y, hueShift);

            for (int x = 0; x < numCols; x++) {
                Color c;
                float bri = (x + 1) / (float) numCols;
                if (numRows == 1) {
                    c = Color.getHSBColor(hueShift + refHue, saturation, bri);
                } else {
                    c = Color.getHSBColor(hue, saturation, bri);
                }
                panel.addButton(x, y, c);
            }
        }
    }

    private float calcHue(int y, float hueShift) {
        float startHue = refHue - MAX_HUE_DEVIATION;
        if (startHue < 0) {
            startHue += 1.0f;
        }
        float hueStep = calcHueStep();
        float hue = hueShift + startHue + y * hueStep;
        if (hue > 1.0f) {
            hue = hue - 1.0f;
        }
        return hue;
    }

    private float calcHueStep() {
        // the total hue range (2 * MAX_HUE_DEVIATION) is
        // divided into numRows - 1 equal parts
        return 2 * MAX_HUE_DEVIATION / (numRows - 1);
    }

    @Override
    public void setSize(int numRows, int numCols) {
        super.setSize(numRows, numCols);
        lastCols = numCols;
        lastRows = numRows;
    }

    @Override
    public String getDialogTitle() {
        return dialogTitle;
    }
}
