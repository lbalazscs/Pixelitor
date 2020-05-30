/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
    private static int lastRows = 7;
    private static int lastCols = 10;

    private final int rgb;
    private final int otherRGB;
    private final boolean startWithFg;

    public RGBColorMixPalette(boolean startWithFg) {
        super(lastRows, lastCols);
        this.startWithFg = startWithFg;

        Color color, otherColor;
        if (startWithFg) {
            color = getFGColor();
            otherColor = getBGColor();
        } else {
            color = getBGColor();
            otherColor = getFGColor();
        }

        rgb = color.getRGB();
        otherRGB = otherColor.getRGB();

        config = new RGBPaletteConfig();
    }

    @Override
    public void addButtons(PalettePanel panel) {
        for (int y = 0; y < numRows; y++) {
            for (int x = 0; x < numCols; x++) {
                Color c;
                if (numRows == 1) {
                    int mixed = getMixed(x);

                    c = new Color(mixed);
                } else {
                    int mixed = getMixed(x);
                    int rowsMiddle = numRows / 2;
                    if (y < rowsMiddle) {
                        float mixFactor = (rowsMiddle - y) / (rowsMiddle + 1.0f);
                        mixed = mixColors(mixFactor, mixed, 0xFF000000);
                    } else if (y > rowsMiddle) {
                        float mixFactor = (y - rowsMiddle) / (rowsMiddle + 1.0f);
                        mixed = mixColors(mixFactor, mixed, 0xFFFFFFFF);
                    }
                    c = new Color(mixed);
                }
                panel.addButton(x, y, c);
            }
        }
    }

    private int getMixed(int x) {
        float mixFactor = calcMixFactor(x);
        int mixed = mixColors(mixFactor, rgb, otherRGB);

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
        return (x * (numCols + 1) / (float) numCols) / numCols;
    }

    @Override
    public void setSize(int numRows, int numCols) {
        super.setSize(numRows, numCols);
        lastCols = numCols;
        lastRows = numRows;
    }

    @Override
    public String getDialogTitle() {
        return startWithFg ?
            "RGB Mix with Background" :
            "RGB Mix with Foreground";
    }
}
