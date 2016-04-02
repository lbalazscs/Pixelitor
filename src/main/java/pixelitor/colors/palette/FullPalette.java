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

import java.awt.Color;

public class FullPalette extends Palette {
    private static int lastRows = 11;
    private static int lastCols = 7;

    private int numHueLevels;

    public FullPalette() {
        super(lastRows, lastCols);

        config = new HueSatPaletteConfig(0, 0.9f); // default saturation is 90%
    }

    @Override
    public void setSize(int numRows, int numCols) {
        super.setSize(numRows, numCols);
        numHueLevels = numRows - 1;
        lastCols = numCols;
        lastRows = numRows;
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
                if (y == 0) { // first, gray row
                    float bri = (x + 1) / (float) numCols;
                    c = Color.getHSBColor(0, 0, bri);
                } else { // color rows
                    float bri = (x + 1) / (float) numCols;
                    c = Color.getHSBColor(hue, saturation, bri);
                }
                panel.addButton(x, y, c);
            }
        }
    }

    private float calcHue(int y, float hueShift) {
        float hue = hueShift + (y - 1) / (float) numHueLevels;
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
