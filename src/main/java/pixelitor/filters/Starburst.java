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

package pixelitor.filters;

import pixelitor.colors.ColorUtils;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ReseedSupport;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.NO_OPACITY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Starburst
 */
public class Starburst extends FilterWithParametrizedGUI {
    private final RangeParam numberOfRaysParam = new RangeParam("Number of Rays", 2, 10, 100);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final ColorParam bgColor = new ColorParam("Background Color:", WHITE, NO_OPACITY);
    private final ColorParam fgColor = new ColorParam("Rays Color:", BLACK, NO_OPACITY);
    private final BooleanParam randomColorsParam = new BooleanParam("Use Random Colors for Rays", false, IGNORE_RANDOMIZE);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public Starburst() {
        super(ShowOriginal.NO);
        setParamSet(new ParamSet(
                numberOfRaysParam,
                bgColor,
                fgColor,
                randomColorsParam,
                center,
                rotate
        ).withAction(ReseedSupport.createAction("Reseed Colors", "Recalculates the random colors")));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(bgColor.getColor());
        g.fillRect(0, 0, width, height);

        float cx = width * center.getRelativeX();
        float cy = height * center.getRelativeY();

        g.setColor(fgColor.getColor());

        int numberOfRays = numberOfRaysParam.getValue();
        boolean useRandomColors = randomColorsParam.isChecked();

        double averageRayAngle = Math.PI / numberOfRays;
        double startAngle = rotate.getValueInRadians();
        double angle = startAngle;

        double radius = width + height; // should be enough even if the center is outside the image

        for (int i = 0; i < numberOfRays; i++) {
            GeneralPath triangle = new GeneralPath();
            triangle.moveTo(cx, cy);

            double p1x = cx + radius * Math.cos(angle);
            double p1y = cy + radius * Math.sin(angle);

            triangle.lineTo(p1x, p1y);

            angle += averageRayAngle;

            double p2x = cx + radius * Math.cos(angle);
            double p2y = cy + radius * Math.sin(angle);

            angle += averageRayAngle; // increment again to leave out

            triangle.lineTo(p2x, p2y);
            triangle.closePath();

            if (useRandomColors) {
                g.setColor(ColorUtils.getRandomColor(rand, false));
            }

            g.fill(triangle);
        }

        g.dispose();
        return dest;
    }
}