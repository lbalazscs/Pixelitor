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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.colors.Colors;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.gui.GUIText;
import pixelitor.utils.BlurredShape;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * The "Flashlight" filter.
 */
public class Flashlight extends ParametrizedFilter {
    public static final String NAME = "Flashlight";

    @Serial
    private static final long serialVersionUID = 8815249851114990821L;

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam(GUIText.RADIUS, 1, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Edge Softness", 0, 20, 100);
    private final IntChoiceParam shape = BlurredShape.getChoices();
    private final IntChoiceParam bg = new IntChoiceParam("Background", new Item[]{
        new Item("Black", FlashLightFilter.BG_BLACK),
        new Item("White", FlashLightFilter.BG_WHITE),
        new Item("Background Color", FlashLightFilter.BG_TOOL_BG),
        new Item("Transparent", FlashLightFilter.BG_TRANSPARENT),
    }, IGNORE_RANDOMIZE);
    private final BooleanParam invert = new BooleanParam("Invert");
    private final RangeParam opacity =
        new RangeParam(GUIText.OPACITY, 0, 100, 100);

    private FlashLightFilter filter;

    public Flashlight() {
        super(true);
        opacity.setPresetKey("Opacity");
        radius.setPresetKey("Radius");

        initParams(
            center,
            radius.withAdjustedRange(1.0),
            softness,
            shape,
            invert,
            bg,
            opacity
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new FlashLightFilter();
        }

        filter.setCenter(
            src.getWidth() * center.getRelativeX(),
            src.getHeight() * center.getRelativeY()
        );
        filter.setRadius(
            radius.getValueAsDouble(0),
            radius.getValueAsDouble(1),
            softness.getPercentage());
        filter.setShape(shape.getValue());
        filter.setBackgroundType(bg.getValue());
        filter.setInvert(invert.isChecked());

        BufferedImage filtered = filter.filter(src, dest);

        if (opacity.getValue() == 100) {
            return filtered;
        } else {
            float alpha = (float) opacity.getPercentage();
            Graphics2D g = filtered.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - alpha));
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return filtered;
        }
    }

    /**
     * Flashlight implementation.
     */
    private static class FlashLightFilter extends PointFilter {
        public static final int BG_BLACK = 0;
        public static final int BG_WHITE = 1;
        public static final int BG_TRANSPARENT = 2;
        public static final int BG_TOOL_BG = 3;

        private Point2D center;
        private double innerRadiusX;
        private double innerRadiusY;
        private double outerRadiusX;
        private double outerRadiusY;
        private int bgRGBA;
        private BlurredShape shape;
        private boolean invert;

        public FlashLightFilter() {
            super(NAME);
        }

        @Override
        public int processPixel(int x, int y, int rgb) {
            int srcAlpha = (rgb >>> 24) & 0xFF;
            double outside = shape.isOutside(x, y);
            if (invert) {
                outside = 1.0 - outside;
            }

            if (outside == 1.0) {
                // outside the blurred shape set the background color
                // while preserving the transparency of the source
                return Colors.setMinAlpha(bgRGBA, srcAlpha);
            } else if (outside == 0.0) {
                return rgb;
            } else {
                if (bgRGBA == 0) {
                    // Don't mix, because it would darken the image.
                    // Take the smaller alpha in order to preserve existing transparency.
                    int calcAlpha = (int) (255.0 * (1.0 - outside));
                    return Colors.setMinAlpha(rgb, calcAlpha);
                } else {
                    int mixed = ImageMath.mixColors((float) outside, rgb, bgRGBA);
                    return Colors.setMinAlpha(mixed, srcAlpha);
                }
            }
        }

        public void setCenter(double cx, double cy) {
            center = new Point2D.Double(cx, cy);
        }

        public void setRadius(double radiusX, double radiusY, double softness) {
            innerRadiusX = radiusX - radiusX * softness;
            innerRadiusY = radiusY - radiusY * softness;

            outerRadiusX = radiusX + radiusX * softness;
            outerRadiusY = radiusY + radiusY * softness;
        }

        public void setBackgroundType(int bg) {
            bgRGBA = switch (bg) {
                case BG_BLACK -> 0xFF_00_00_00;
                case BG_WHITE -> 0xFF_FF_FF_FF;
                case BG_TOOL_BG -> FgBgColors.getBGColor().getRGB();
                case BG_TRANSPARENT -> 0;
                default -> throw new IllegalArgumentException("bg = " + bg);
            };
        }

        // must be called after the radius/center settings
        public void setShape(int type) {
            shape = BlurredShape.create(type, center,
                innerRadiusX, innerRadiusY,
                outerRadiusX, outerRadiusY);
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }
    }
}
