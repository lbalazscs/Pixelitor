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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.utils.BlurredShape;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Flashlight filter
 */
public class Flashlight extends ParametrizedFilter {
    public static final String NAME = "Flashlight";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 1, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Softness", 0, 20, 100);
    private final IntChoiceParam shape = BlurredShape.getChoices();
    private final IntChoiceParam bg = new IntChoiceParam("Background", new Item[]{
        new Item("Black", Impl.BG_BLACK),
        new Item("White", Impl.BG_WHITE),
        new Item("Transparent", Impl.BG_TRANSPARENT),
    }, IGNORE_RANDOMIZE);
    private final BooleanParam invert = new BooleanParam("Invert", false);

    private Impl filter;

    public Flashlight() {
        super(ShowOriginal.YES);

        setParams(
            center,
            radius.withAdjustedRange(1.0),
            softness,
            shape,
            invert,
            bg
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl();
        }

        filter.setCenter(
                src.getWidth() * center.getRelativeX(),
                src.getHeight() * center.getRelativeY()
        );

        double radiusX = radius.getValueAsDouble(0);
        double radiusY = radius.getValueAsDouble(1);
        double softnessFactor = softness.getValueAsDouble() / 100.0;
        filter.setRadius(radiusX, radiusY, softnessFactor);

        filter.setShape(shape.getValue());
        filter.setBG(bg.getValue());
        filter.setInvert(invert.isChecked());

        dest = filter.filter(src, dest);

        return dest;
    }

    /**
     * Flashlight implementation
     */
    private static class Impl extends PointFilter {
        private Point2D center;
        private double innerRadiusX;
        private double innerRadiusY;
        private double outerRadiusX;
        private double outerRadiusY;

        public static final int BG_BLACK = 0;
        public static final int BG_WHITE = 1;
        public static final int BG_TRANSPARENT = 2;

        private int bgPixel;
        private BlurredShape shape;
        private boolean invert;

        public Impl() {
            super(NAME);
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            double outside = shape.isOutside(x, y);
            if (invert) {
                outside = 1.0 - outside;
            }
            if (outside == 1.0) {
                return bgPixel;
            } else if (outside == 0.0) {
                return rgb;
            } else {
                return ImageMath.mixColors((float) outside, rgb, bgPixel);
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

        public void setBG(int bg) {
            if (bg == BG_BLACK) {
                bgPixel = 0xFF000000;
            } else if (bg == BG_WHITE) {
                bgPixel = 0xFFFFFFFF;
            } else if (bg == BG_TRANSPARENT) {
                bgPixel = 0;
            } else {
                throw new IllegalArgumentException("bg = " + bg);
            }
        }

        // must be called after the shape arguments!
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
