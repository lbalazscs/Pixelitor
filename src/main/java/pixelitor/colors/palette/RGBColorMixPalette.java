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

import static com.jhlabs.image.ImageMath.mixColors;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * A {@link Palette} that mixes the foreground color with the background color
 * using the RGB color space to interpolate between them.
 */
public class RGBColorMixPalette extends Palette {
    // static palette-specific variables so that they
    // are remembered between dialog sessions
    private static int lastRowCount = 7;
    private static int lastColumnCount = 10;

    private final int rgbA;
    private final int rgbB;
    private final boolean startWithFg;

    public RGBColorMixPalette(boolean startWithFg) {
        super(lastRowCount, lastColumnCount);
        this.startWithFg = startWithFg;

        Color colorA, colorB;
        if (startWithFg) {
            colorA = getFGColor();
            colorB = getBGColor();
        } else {
            colorA = getBGColor();
            colorB = getFGColor();
        }

        rgbA = colorA.getRGB();
        rgbB = colorB.getRGB();

        config = new RGBPaletteConfig();
    }

    @Override
    public void addButtons(PalettePanel panel) {
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                Color color;
                if (rowCount == 1) {
                    int mixed = getMixed(col);

                    color = new Color(mixed);
                } else {
                    int mixed = getMixed(col);
                    int rowsMiddle = rowCount / 2;
                    if (row < rowsMiddle) {
                        float mixFactor = (rowsMiddle - row) / (rowsMiddle + 1.0f);
                        mixed = mixColors(mixFactor, mixed, 0xFF_00_00_00);
                    } else if (row > rowsMiddle) {
                        float mixFactor = (row - rowsMiddle) / (rowsMiddle + 1.0f);
                        mixed = mixColors(mixFactor, mixed, 0xFF_FF_FF_FF);
                    }
                    color = new Color(mixed);
                }
                panel.addButton(col, row, color);
            }
        }
    }

    private int getMixed(int x) {
        float mixFactor = calcMixFactor(x);
        int mixed = mixColors(mixFactor, rgbA, rgbB);

        var rgbConfig = (RGBPaletteConfig) config;
        float cyanRed = rgbConfig.getCyanRed();

        if (cyanRed > 0.5f) {
            mixed = mixColors(cyanRed - 0.5f, mixed, 0xFF_FF_00_00);
        } else {
            mixed = mixColors(0.5f - cyanRed, mixed, 0xFF_00_FF_FF);
        }

        float magentaGreen = rgbConfig.getMagentaGreen();
        if (magentaGreen > 0.5f) {
            mixed = mixColors(magentaGreen - 0.5f, mixed, 0xFF_00_FF_00);
        } else {
            mixed = mixColors(0.5f - magentaGreen, mixed, 0xFF_FF_00_FF);
        }

        float yellowBlue = rgbConfig.getYellowBlue();
        if (yellowBlue > 0.5f) {
            mixed = mixColors(yellowBlue - 0.5f, mixed, 0xFF_00_00_FF);
        } else {
            mixed = mixColors(0.5f - yellowBlue, mixed, 0xFF_FF_FF_00);
        }

        return mixed;
    }

    private float calcMixFactor(int x) {
        return (x * (columnCount + 1) / (float) columnCount) / columnCount;
    }

    @Override
    public void setDimensions(int rows, int columns) {
        super.setDimensions(rows, columns);
        lastColumnCount = columns;
        lastRowCount = rows;
    }

    @Override
    public String getDialogTitle() {
        return startWithFg ?
            "RGB Mix with Background" :
            "RGB Mix with Foreground";
    }
}
