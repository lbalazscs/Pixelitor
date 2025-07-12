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

import static com.jhlabs.image.ImageMath.mixColors;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * A palette that mixes foreground and background colors using RGB interpolation.
 */
public class RGBColorMixPalette extends DynamicPalette {
    // static palette-specific fields are remembered between dialog sessions
    private static int lastRowCount = 7;
    private static int lastColumnCount = 10;

    private final int rgbA;
    private final int rgbB;
    private final boolean startWithFg;

    // opaque color constants for mixing
    private static final int OPAQUE_RED = 0xFF_FF_00_00;
    private static final int OPAQUE_GREEN = 0xFF_00_FF_00;
    private static final int OPAQUE_BLUE = 0xFF_00_00_FF;
    private static final int OPAQUE_CYAN = 0xFF_00_FF_FF;
    private static final int OPAQUE_MAGENTA = 0xFF_FF_00_FF;
    private static final int OPAQUE_YELLOW = 0xFF_FF_FF_00;
    private static final int OPAQUE_BLACK = 0xFF_00_00_00;
    private static final int OPAQUE_WHITE = 0xFF_FF_FF_FF;

    public RGBColorMixPalette(boolean startWithFg) {
        super(lastRowCount, lastColumnCount);
        this.startWithFg = startWithFg;

        Color colorA = startWithFg ? getFGColor() : getBGColor();
        Color colorB = startWithFg ? getBGColor() : getFGColor();

        rgbA = colorA.getRGB();
        rgbB = colorB.getRGB();

        config = new RGBPaletteConfig();
    }

    @Override
    public List<Color> getColors() {
        List<Color> colors = new ArrayList<>(rowCount * columnCount);
        int rowsMiddle = rowCount / 2;

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                int mixed = getMixed(col);

                if (row < rowsMiddle) {
                    float mixFactor = (rowsMiddle - row) / (rowsMiddle + 1.0f);
                    mixed = mixColors(mixFactor, mixed, OPAQUE_BLACK);
                } else if (row > rowsMiddle) {
                    float mixFactor = (row - rowsMiddle) / (rowsMiddle + 1.0f);
                    mixed = mixColors(mixFactor, mixed, OPAQUE_WHITE);
                }
                colors.add(new Color(mixed));
            }
        }
        return colors;
    }

    private int getMixed(int x) {
        float mixFactor = calcMixFactor(x);
        int mixed = mixColors(mixFactor, rgbA, rgbB);

        var rgbConfig = (RGBPaletteConfig) config;
        float cyanRed = rgbConfig.getCyanRed();
        if (cyanRed > 0.5f) {
            mixed = mixColors(cyanRed - 0.5f, mixed, OPAQUE_RED);
        } else {
            mixed = mixColors(0.5f - cyanRed, mixed, OPAQUE_CYAN);
        }

        float magentaGreen = rgbConfig.getMagentaGreen();
        if (magentaGreen > 0.5f) {
            mixed = mixColors(magentaGreen - 0.5f, mixed, OPAQUE_GREEN);
        } else {
            mixed = mixColors(0.5f - magentaGreen, mixed, OPAQUE_MAGENTA);
        }

        float yellowBlue = rgbConfig.getYellowBlue();
        if (yellowBlue > 0.5f) {
            mixed = mixColors(yellowBlue - 0.5f, mixed, OPAQUE_BLUE);
        } else {
            mixed = mixColors(0.5f - yellowBlue, mixed, OPAQUE_YELLOW);
        }

        return mixed;
    }

    private float calcMixFactor(int x) {
        return (x * (columnCount + 1) / (float) columnCount) / columnCount;
    }

    @Override
    public void setGridSize(int rows, int columns) {
        super.setGridSize(rows, columns);
        lastColumnCount = columns;
        lastRowCount = rows;
    }

    @Override
    public String getDialogTitle() {
        return startWithFg ? "RGB Mix with Background" : "RGB Mix with Foreground";
    }
}
