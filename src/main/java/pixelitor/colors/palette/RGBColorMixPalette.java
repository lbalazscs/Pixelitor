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

import static com.jhlabs.image.ImageMath.mixColors;
import static pixelitor.colors.FgBgColors.getBgColor;
import static pixelitor.colors.FgBgColors.getFgColor;
import static pixelitor.colors.palette.RGBPaletteConfig.NEUTRAL;

/**
 * A palette that mixes foreground and background colors using RGB interpolation.
 * The rows tint toward black above the middle row and toward white below it.
 * The config sliders apply an additional color shift.
 */
public class RGBColorMixPalette extends DynamicPalette {
    // opaque packed ARGB constants for mixing
    private static final int OPAQUE_RED = 0xFF_FF_00_00;
    private static final int OPAQUE_GREEN = 0xFF_00_FF_00;
    private static final int OPAQUE_BLUE = 0xFF_00_00_FF;
    private static final int OPAQUE_CYAN = 0xFF_00_FF_FF;
    private static final int OPAQUE_MAGENTA = 0xFF_FF_00_FF;
    private static final int OPAQUE_YELLOW = 0xFF_FF_FF_00;
    private static final int OPAQUE_BLACK = 0xFF_00_00_00;
    private static final int OPAQUE_WHITE = 0xFF_FF_FF_FF;

    private final RGBPaletteConfig rgbConfig;

    // packed ARGB ints at the start and end of the interpolation
    private final int startRgb;
    private final int endRgb;

    public RGBColorMixPalette(boolean startWithFg) {
        Color colorA = startWithFg ? getFgColor() : getBgColor();
        Color colorB = startWithFg ? getBgColor() : getFgColor();

        String title = startWithFg
            ? "RGB Mix: Foreground to Background"
            : "RGB Mix: Background to Foreground";

        var rgbConfig = new RGBPaletteConfig();

        // default grid: 7 brightness rows (tinted toward black/white) × 10 color mix columns
        super(title, rgbConfig, 7, 10);

        this.rgbConfig = rgbConfig;
        this.startRgb = colorA.getRGB();
        this.endRgb = colorB.getRGB();
    }

    @Override
    protected Color createColor(int row, int col) {
        int colColor = calcColumnColor(col);
        int mixed = tintRow(colColor, row);
        return new Color(mixed);
    }

    /**
     * Rows above the middle blend toward black, rows below toward white,
     * and the middle row stays as is.
     */
    private int tintRow(int rgb, int row) {
        int middleRow = numRows / 2;
        if (row == middleRow) {
            return rgb; // the middle row keeps the pure column color
        }

        boolean aboveMiddle = row < middleRow;
        int target = aboveMiddle ? OPAQUE_BLACK : OPAQUE_WHITE;
        int rowsInDirection = aboveMiddle ? middleRow : numRows - middleRow - 1;
        float t = Math.abs(row - middleRow) / (rowsInDirection + 1.0f);

        return mixColors(t, rgb, target);
    }

    private int calcColumnColor(int col) {
        int mixed = mixColors(calcColumnMixFactor(col), startRgb, endRgb);
        mixed = applyShift(mixed, rgbConfig.getCyanRed(), OPAQUE_RED, OPAQUE_CYAN);
        mixed = applyShift(mixed, rgbConfig.getMagentaGreen(), OPAQUE_GREEN, OPAQUE_MAGENTA);
        return applyShift(mixed, rgbConfig.getYellowBlue(), OPAQUE_BLUE, OPAQUE_YELLOW);
    }

    private static int applyShift(int rgb, float sliderValue, int highTarget, int lowTarget) {
        float delta = sliderValue - NEUTRAL;
        return delta > 0
            ? mixColors(delta, rgb, highTarget)
            : mixColors(-delta, rgb, lowTarget);
    }
}
