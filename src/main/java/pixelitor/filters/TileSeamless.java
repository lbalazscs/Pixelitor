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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import com.jhlabs.image.OffsetFilter;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.utils.ImageUtils;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The "Tile Seamless" filter.
 */
public class TileSeamless extends ParametrizedFilter {
    public static final String NAME = "Tile Seamless";

    private final ImagePositionParam center =
        new ImagePositionParam("Center");

    private MaskMaker maskMaker = null;
    private OffsetFilter offsetFilter = null;

    public TileSeamless() {
        super(true);

        setParams(center);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (maskMaker == null) {
            maskMaker = new MaskMaker();
        }
        if (offsetFilter == null) {
            offsetFilter = new OffsetFilter(NAME);
        }

        offsetFilter.setRelativeX(center.getRelativeX());
        offsetFilter.setRelativeY(center.getRelativeY());
        offsetFilter.setUseRelative(true);

        BufferedImage offsetImage = offsetFilter.filter(src, dest);

        maskMaker.setCenter(center.getAbsolutePoint(src));
        maskMaker.setSize(src);
        BufferedImage maskImage = maskMaker.filter(src, ImageUtils.createImageWithSameCM(src));

        return ImageUtils.mask(src, offsetImage, maskImage);
//        return switch (showParam.getSelected()) {
//            case Final -> ImageUtils.mask(src, offsetImage, maskImage);
//            case Offset -> offsetImage;
//            case Mask -> maskImage;
//        };
    }

    private static class MaskMaker extends PointFilter {
        double cx;
        double cy;
        double width;
        double height;

        protected MaskMaker() {
            super(NAME);
        }

        public void setCenter(Point2D p) {
            this.cx = p.getX();
            this.cy = p.getY();
        }

        public void setSize(BufferedImage src) {
            width = src.getWidth();
            height = src.getHeight();
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            double distX;
            double distY;

            if (x > cx) {
                distX = (x - cx) / (width - cx);
            } else {
                distX = (cx - x) / cx;
            }
            if (y > cy) {
                distY = (y - cy) / (height - cy);
            } else {
                distY = (cy - y) / cy;
            }

            double bri01 = distX * distY / (distX * distY + (1.0 - distX) * (1.0 - distY));
            int bri = ImageMath.clamp((int) (bri01 * 255), 0, 255);

            return 0xFF_00_00_00 | bri << 16 | bri << 8 | bri;
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}