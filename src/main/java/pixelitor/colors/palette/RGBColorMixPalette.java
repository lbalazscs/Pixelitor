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

import com.jhlabs.image.ImageMath;
import pixelitor.colors.FgBgColors;

import java.awt.Color;

public class RGBColorMixPalette extends Palette {
    private static int lastRows = 7;
    private static int lastCols = 10;

    private static final float MAX_BRI_DEVIATION = 0.5f;
    private final int rgb;
    private final int otherRGB;
    private final boolean fg;

    public RGBColorMixPalette(boolean fg) {
        super(lastRows, lastCols);
        this.fg = fg;

        Color color, otherColor;
        if (fg) {
            color = FgBgColors.getFG();
            otherColor = FgBgColors.getBG();
        } else {
            color = FgBgColors.getBG();
            otherColor = FgBgColors.getFG();
        }

        rgb = color.getRGB();
        otherRGB = otherColor.getRGB();

        config = new RGBPaletteConfig();
    }

    @Override
    public void addButtons(VariationsPanel panel) {
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
                        mixed = ImageMath.mixColors(mixFactor, mixed, 0xFF000000);
                    } else if (y > rowsMiddle) {
                        float mixFactor = (y - rowsMiddle) / (rowsMiddle + 1.0f);
                        mixed = ImageMath.mixColors(mixFactor, mixed, 0xFFFFFFFF);
                    }
                    c = new Color(mixed);
                }
                panel.addButton(x, y, c);
            }
        }
    }

    private int getMixed(int x) {
        float mixFactor = calcMixFactor(x);
        int mixed = ImageMath.mixColors(mixFactor, this.rgb, otherRGB);

        RGBPaletteConfig c = (RGBPaletteConfig) config;
        float cyanRed = c.getCyanRed();

        if (cyanRed > 0.5f) {
            mixed = ImageMath.mixColors(cyanRed - 0.5f, mixed, 0xFF_FF_00_00);
        } else {
            mixed = ImageMath.mixColors(0.5f - cyanRed, mixed, 0xFF_00_FF_FF);
        }

        float magentaGreen = c.getMagentaGreen();
        if (magentaGreen > 0.5f) {
            mixed = ImageMath.mixColors(magentaGreen - 0.5f, mixed, 0xFF_00_FF_00);
        } else {
            mixed = ImageMath.mixColors(0.5f - magentaGreen, mixed, 0xFF_FF_00_FF);
        }

        float yellowBlue = c.getYellowBlue();
        if (yellowBlue > 0.5f) {
            mixed = ImageMath.mixColors(yellowBlue - 0.5f, mixed, 0xFF_00_00_FF);
        } else {
            mixed = ImageMath.mixColors(0.5f - yellowBlue, mixed, 0xFF_FF_FF_00);
        }

        return mixed;
    }

    private float calcMixFactor(int x) {
        return (x * (numCols + 1) / (float) numCols) / (float) numCols;
    }

    @Override
    public void setSize(int numRows, int numCols) {
        super.setSize(numRows, numCols);
        lastCols = numCols;
        lastRows = numRows;
    }

    @Override
    public String getDialogTitle() {
        return fg ? "RGB Mix with Background" : "RGB Mix with Foreground";
    }
}
