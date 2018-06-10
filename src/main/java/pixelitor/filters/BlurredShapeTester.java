/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BlurredShape;

import java.awt.image.BufferedImage;

/**
 * Development filter for testing blurred shapes
 */
public class BlurredShapeTester extends ParametrizedFilter {
    public static final String NAME = "Blurred Shape Tester";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam radius = new GroupedRangeParam("Radius", 1, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Softness", 0, 20, 99);
    private final IntChoiceParam shape = BlurredShape.getChoices();

    private Impl filter;

    public BlurredShapeTester() {
        super(ShowOriginal.YES);

        setParamSet(new ParamSet(
                center,
                radius.withAdjustedRange(1.0),
                softness,
                shape
        ));
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

        dest = filter.filter(src, dest);

        return dest;
    }

    private static class Impl extends PointFilter {
        private double cx;
        private double cy;
        private double innerRadiusX;
        private double innerRadiusY;
        private double outerRadiusX;
        private double outerRadiusY;

        public static final int BG_BLACK = 0;
        public static final int BG_WHITE = 1;
        public static final int BG_TRANSPARENT = 2;

        private static final int RGB_BLACK = 0xFF000000;
        private static final int RGB_WHITE = 0xFFFFFFFF;

        private BlurredShape shape;

        public Impl() {
            super(NAME);
        }

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {
            return super.filter(src, dst);
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            double outside = shape.isOutside(x, y);
            if (outside == 1.0) {
                return RGB_BLACK;
            } else if (outside == 0.0) {
                return RGB_WHITE;
            } else {
                return ImageMath.mixColors((float) outside, RGB_WHITE, RGB_BLACK);
            }
        }

        public void setCenter(double cx, double cy) {
            this.cx = cx;
            this.cy = cy;
        }

        public void setRadius(double radiusX, double radiusY, double softness) {
            this.innerRadiusX = radiusX - radiusX * softness;
            this.innerRadiusY = radiusY - radiusY * softness;

            this.outerRadiusX = radiusX + radiusX * softness;
            this.outerRadiusY = radiusY + radiusY * softness;
        }

        // must be called after the shape arguments!
        public void setShape(int type) {
            shape = BlurredShape.create(type, cx, cy,
                    innerRadiusX, innerRadiusY,
                    outerRadiusX, outerRadiusY);
        }
    }
}
