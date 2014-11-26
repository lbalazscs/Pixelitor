/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Draw Grid
 */
public class RenderGrid extends FilterWithParametrizedGUI {
    private final RangeParam spacingParam = new RangeParam("Spacing", 1, 100, 40);
    private final RangeParam widthParam = new RangeParam("Width", 1, 100, 20);
    private final ColorParam colorParam = new ColorParam("Color", Color.BLACK, true, true);
    private final BooleanParam emptyIntersectionsParam = new BooleanParam("Empty Intersections", false);
    private final RangeParam opacityParam = new RangeParam("Opacity (%)", 0, 100, 100);
    private final AngleParam rotateResult = new AngleParam("Rotate Result", 0);


    public RenderGrid() {
        super("Grid", true, false);
        setParamSet(new ParamSet(
                spacingParam,
                widthParam,
                colorParam,
                emptyIntersectionsParam,
                opacityParam,
                rotateResult
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);

        Color color = colorParam.getColor();
        int width = widthParam.getValue();
        int spacing = spacingParam.getValue();

        boolean emptyIntersections = emptyIntersectionsParam.getValue();

        Graphics2D g = dest.createGraphics();

        int destWidth = dest.getWidth();
        int destHeight = dest.getHeight();

        float opacity = opacityParam.getValueAsPercentage();
        boolean rotated = !rotateResult.isSetToDefault();

        int maxX = destWidth;
        int maxY = destHeight;
//        if(rotated) {
//            maxX = (int) (Math.max(destWidth, destHeight) * 1.3);
//            maxY = maxX;
//        }

        // we draw the grid first on an image with transparent background
        BufferedImage tmp = ImageUtils.getGridImageOnTransparentBackground(color, maxX, maxY, width, spacing, width, spacing, emptyIntersections);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, opacity));
        if(rotated) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);


            g.translate(maxX/2, maxY/2);

            g.rotate(rotateResult.getValueInRadians());

            int xDiff = maxX - destWidth;
            int yDiff = maxY - destHeight;

            g.translate(-xDiff / 2, -yDiff / 2);
            g.translate(-maxX/2 / 2, -maxY/2 / 2);
        }
        g.drawImage(tmp, 0, 0, null);

        g.dispose();

        return dest;
    }
}