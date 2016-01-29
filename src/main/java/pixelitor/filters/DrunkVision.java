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

import net.jafama.FastMath;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BasicProgressTracker;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.ReseedSupport;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.lang.Math.PI;

/**
 * "Drunk Vision" filter inspired by "Fragment Blur" in paint.net
 */
public class DrunkVision extends FilterWithParametrizedGUI {
    public static final String NAME = "Drunk Vision";

    private final RangeParam drunkenness = new RangeParam("Drunkenness", 0, 20, 100);
    private final RangeParam numEyes = new RangeParam("Number of Eyes", 2, 5, 42);

    public DrunkVision() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                drunkenness,
                numEyes
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (drunkenness.getValue() == 0) {
            return src;
        }

        int numShiftedImages = numEyes.getValue() - 1;
        ProgressTracker pt = new BasicProgressTracker(NAME, numShiftedImages);

        dest = ImageUtils.copyImage(src);

        Graphics2D g = dest.createGraphics();

        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        int maxDistance = (int) (drunkenness.getValueAsPercentage() * 0.2 * (src.getWidth() + src.getHeight()));

        Point[] transformPoints = generateTransforms(numShiftedImages, maxDistance, rand);
        for (int i = 0; i < numShiftedImages; i++) {
            AffineTransform transform = AffineTransform.getTranslateInstance(transformPoints[i].x, transformPoints[i].y);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f / (i + 2)));
            g.drawImage(src, transform, null);
            pt.unitDone();
        }
        pt.finish();

        g.dispose();

        return dest;
    }

    // generates the transforms for the images that are shifted
    // from the original
    private static Point[] generateTransforms(int numImages, int maxDistance, Random rand) {
        Point[] retVal = new Point[numImages];
        int i = 0;
        double firstPointAngle = 0;
        while (i < numImages) {
            if (i == 0) {
                // put it somewhere horizontal at distance maxDist
                double r = rand.nextDouble();
                if (r < 0.5) { // to the right
                    firstPointAngle = -0.25 + r;
                } else { // to the left
                    firstPointAngle = PI - 0.75 + r;
                }
                retVal[i] = pointFormPolar(firstPointAngle, maxDistance);
            } else if (i == 1) {
                // put it more or less opposing the first point
                double rangeStart = firstPointAngle + 3 * PI / 4;
                double angle = rangeStart + PI * rand.nextDouble() / 2;
                retVal[i] = pointFormPolar(angle, maxDistance);
            } else if (i < 4) {
                double rangeStart;
                if (i == 2) {
                    rangeStart = firstPointAngle + PI / 4;
                } else { // i == 3
                    rangeStart = firstPointAngle + 5 * PI / 4;
                }
                double angle = rangeStart + PI * rand.nextDouble() / 2;
                retVal[i] = pointFormPolar(angle, maxDistance);
            } else {
                double randomAngle = rand.nextDouble() * PI * 2;

                double minDistance = maxDistance / (double) i;
                double distance = minDistance + (maxDistance - minDistance) * rand.nextDouble();

                int shiftX = (int) (distance * FastMath.cos(randomAngle));
                int shiftY = (int) (distance * FastMath.sin(randomAngle));
                retVal[i] = new Point(shiftX, shiftY);
            }
            i++;
        }

        return retVal;
    }

    private static Point pointFormPolar(double angle, double dist) {
        int x = (int) (dist * FastMath.cos(angle));
        int y = (int) (dist * FastMath.sin(angle));
        return new Point(x, y);
    }
}