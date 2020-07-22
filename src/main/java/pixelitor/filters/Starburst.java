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

import pixelitor.colors.Colors;
import pixelitor.colors.FgBgColors;
import pixelitor.filters.gui.*;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Rnd;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.NO_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Fill with Starburst filter
 */
public class Starburst extends ParametrizedFilter {
    public static final String NAME = "Starburst";

    private static final int BG_BLACK = 1;
    private static final int BG_ORIGINAL = 2;
    private static final int BG_TRANSPARENT = 3;
    private static final int BG_TOOL = 4;

    private final RangeParam numberOfRaysParam = new RangeParam("Number of Rays", 2, 10, 100);
    private final ImagePositionParam center = new ImagePositionParam("Center");

    private final IntChoiceParam background = new IntChoiceParam("Background", new IntChoiceParam.Item[]{
        new IntChoiceParam.Item("Black", BG_BLACK),
        new IntChoiceParam.Item("Original Image", BG_ORIGINAL),
        new IntChoiceParam.Item("Transparent", BG_TRANSPARENT),
        new IntChoiceParam.Item("Tool Background", BG_TOOL),
    }, IGNORE_RANDOMIZE);

    private final ColorParam raysColor = new ColorParam("Ray Color", WHITE, NO_TRANSPARENCY);
    private final BooleanParam randomColors = new BooleanParam("Random Ray Colors", false, IGNORE_RANDOMIZE);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public Starburst() {
        super(ShowOriginal.NO);

        var reseedColorsAction = ReseedSupport.createAction(
                "Reseed", "Changes the random colors");

        setParams(
                numberOfRaysParam,
                background,
                raysColor,
                randomColors.withAction(reseedColorsAction),
                center,
                rotate
        );

        // enable the "Reseed Colors" button only if
        // the "Random Ray Colors" checkbox is checked
        randomColors.setupEnableOtherIfChecked(reseedColorsAction);

        // enable the "Ray Color" color selector only if
        // the "Random Ray Colors" checkbox is *not* checked
        randomColors.setupDisableOtherIfChecked(raysColor);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Random rand = ReseedSupport.reInitialize();

        int bg = background.getValue();
        if (bg == BG_ORIGINAL) {
            dest = ImageUtils.copyImage(src);
        } else {
            dest = ImageUtils.createImageWithSameCM(src);
        }

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        if (bg != BG_ORIGINAL) {
            if (bg == BG_BLACK) {
                Colors.fillWith(Color.BLACK, g, width, height);
            } else if (bg == BG_TOOL) {
                Colors.fillWith(FgBgColors.getBGColor(), g, width, height);
            }
        }

        float cx = width * center.getRelativeX();
        float cy = height * center.getRelativeY();

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(raysColor.getColor());

        int numberOfRays = numberOfRaysParam.getValue();
        boolean useRandomColors = randomColors.isChecked();

        double averageRayAngle = Math.PI / numberOfRays;
        double angle = rotate.getValueInRadians();

        double radius = width + height; // should be enough even if the center is outside the image

        for (int i = 0; i < numberOfRays; i++) {
            var triangle = new GeneralPath();
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
                g.setColor(Rnd.createRandomColor(rand, false));
            }

            g.fill(triangle);
        }

        g.dispose();
        return dest;
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}