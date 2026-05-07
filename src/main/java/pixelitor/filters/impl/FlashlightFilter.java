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

package pixelitor.filters.impl;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.colors.Colors;
import pixelitor.colors.FgBgColors;
import pixelitor.utils.BlurredShape;

import java.awt.geom.Point2D;

/**
 * Flashlight implementation.
 */
public class FlashlightFilter extends PointFilter {
    public static final int BG_BLACK = 0;
    public static final int BG_WHITE = 1;
    public static final int BG_TRANSPARENT = 2;
    public static final int BG_TOOL_BG = 3;

    private final int bgARGB;
    private final BlurredShape shape;
    private final boolean invert;

    /**
     * Constructs a new FlashlightFilter.
     *
     * @param filterName the name of the filter
     * @param center     the coordinates of the center of the flashlight
     * @param radiusX    the radius of the flashlight along the x-axis
     * @param radiusY    the radius of the flashlight along the y-axis
     * @param softness   the softness of the shape's edge
     * @param shapeType  the type of the shape to be drawn
     * @param bgType     the type of the background
     * @param invert     whether to invert the flashlight effect
     */
    public FlashlightFilter(String filterName, Point2D center, double radiusX, double radiusY, double softness, int shapeType, int bgType, boolean invert) {
        super(filterName);
        this.invert = invert;

        this.bgARGB = switch (bgType) {
            case BG_BLACK -> 0xFF_00_00_00;
            case BG_WHITE -> 0xFF_FF_FF_FF;
            case BG_TOOL_BG -> FgBgColors.getBgColor().getRGB();
            case BG_TRANSPARENT -> 0;
            default -> throw new IllegalArgumentException("bg = " + bgType);
        };

        double innerRadiusX = radiusX - radiusX * softness;
        double innerRadiusY = radiusY - radiusY * softness;
        double outerRadiusX = radiusX + radiusX * softness;
        double outerRadiusY = radiusY + radiusY * softness;

        this.shape = BlurredShape.create(shapeType, center,
            innerRadiusX, innerRadiusY,
            outerRadiusX, outerRadiusY);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        int srcAlpha = rgb >>> 24;
        double outside = shape.isOutside(x, y);
        if (invert) {
            outside = 1.0 - outside;
        }

        if (outside == 1.0) {
            // outside the blurred shape set the background color
            // while preserving the transparency of the source
            return Colors.capAlpha(bgARGB, srcAlpha);
        } else if (outside == 0.0) {
            return rgb;
        } else {
            if (bgARGB == 0) {
                // Don't mix, because it would darken the image.
                // Take the smaller alpha in order to preserve existing transparency.
                int calcAlpha = (int) (255.0 * (1.0 - outside));
                return Colors.capAlpha(rgb, calcAlpha);
            } else {
                int mixed = ImageMath.mixColors((float) outside, rgb, bgARGB);
                return Colors.capAlpha(mixed, srcAlpha);
            }
        }
    }
}
