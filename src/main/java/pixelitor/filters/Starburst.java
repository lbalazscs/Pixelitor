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

import pixelitor.filters.gui.ActionParam;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionParam;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Starburst
 */
public class Starburst extends FilterWithParametrizedGUI {
    private final RangeParam numberOfRaysParam = new RangeParam("Number of Rays", 2, 100, 10);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final ColorParam bgColor = new ColorParam("Background Color:", Color.WHITE, false, false);
    private final ColorParam fgColor = new ColorParam("Rays Color:", Color.BLACK, false, false);
    private final BooleanParam randomColorsParam = new BooleanParam("Use Random Colors for Rays", false, true);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionParam reseedAction = new ReseedNoiseActionParam(
            "Reseed Colors", "Recalculates the random colors",
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    reseed();
                }
            });

    private final Random rand;
    private static long seed = System.nanoTime();

    public Starburst() {
        super("Starburst ", true, false);
        setParamSet(new ParamSet(
                numberOfRaysParam,
                bgColor,
                fgColor,
                randomColorsParam,
                center,
                rotate,
                reseedAction
        ));
        rand = new Random(seed);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        rand.setSeed(seed);
        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(bgColor.getColor());
        g.fillRect(0, 0, width, height);

        float cx = width * center.getRelativeX();
        float cy = height * center.getRelativeY();

        g.setColor(fgColor.getColor());

        int numberOfRays = numberOfRaysParam.getValue();
        boolean useRandomColors = randomColorsParam.getValue();

        double averageRayAngle = Math.PI / numberOfRays;
        double startAngle = rotate.getValueInRadians();
        double angle = startAngle;

        double radius = Math.sqrt(width * width + height * height);

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
                g.setColor(ImageUtils.getRandomColor(rand, false));
            }

            g.fill(triangle);
        }

        g.dispose();
        return dest;
    }

    private static void reseed() {
        seed = System.nanoTime();
    }
}