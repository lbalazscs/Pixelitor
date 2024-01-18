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

import net.jafama.FastMath;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Geometry;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Random;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.lang.Math.PI;

/**
 * "Drunk Vision" filter inspired by "Fragment Blur" in paint.net
 */
public class DrunkVision extends ParametrizedFilter {
    public static final String NAME = "Drunk Vision";

    @Serial
    private static final long serialVersionUID = 6466819957540455396L;

    private final RangeParam drunkenness = new RangeParam("Drunkenness", 0, 20, 100);
    private final RangeParam numEyes = new RangeParam("Number of Eyes", 2, 5, 42);

    public DrunkVision() {
        super(true);

        setParams(
            drunkenness,
            numEyes
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (drunkenness.isZero()) {
            return src;
        }

        int numShiftedImages = numEyes.getValue() - 1;
        var pt = new StatusBarProgressTracker(NAME, numShiftedImages);

        dest = ImageUtils.copyImage(src);

        Graphics2D g = dest.createGraphics();

        Random rand = paramSet.getLastSeedRandom();

        int maxDistance = (int) (drunkenness.getPercentage() * 0.2 * (src.getWidth() + src.getHeight()));

        Point2D[] transformPoints = generateTransforms(numShiftedImages, maxDistance, rand);
        for (int i = 0; i < numShiftedImages; i++) {
            var transform = AffineTransform.getTranslateInstance(
                transformPoints[i].getX(),
                transformPoints[i].getY());
            g.setComposite(AlphaComposite.getInstance(SRC_OVER, 1.0f / (i + 2)));
            g.drawImage(src, transform, null);
            pt.unitDone();
        }
        pt.finished();

        g.dispose();

        return dest;
    }

    // generates the transforms for the images that are shifted
    // from the original
    private static Point2D[] generateTransforms(int numImages, int maxDistance, Random rand) {
        Point2D[] retVal = new Point2D[numImages];
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
                retVal[i] = Geometry.polarToCartesian(maxDistance, firstPointAngle);
            } else if (i == 1) {
                // put it more or less opposing the first point
                double rangeStart = firstPointAngle + 3 * PI / 4;
                double angle = rangeStart + PI * rand.nextDouble() / 2;
                retVal[i] = Geometry.polarToCartesian(maxDistance, angle);
            } else if (i < 4) {
                double rangeStart;
                if (i == 2) {
                    rangeStart = firstPointAngle + PI / 4;
                } else { // i == 3
                    rangeStart = firstPointAngle + 5 * PI / 4;
                }
                double angle = rangeStart + PI * rand.nextDouble() / 2;
                retVal[i] = Geometry.polarToCartesian(maxDistance, angle);
            } else {
                double randomAngle = rand.nextDouble() * PI * 2;

                double minDistance = maxDistance / (double) i;
                double distance = minDistance + (maxDistance - minDistance) * rand.nextDouble();

                double shiftX = distance * FastMath.cos(randomAngle);
                double shiftY = distance * FastMath.sin(randomAngle);
                retVal[i] = new Point2D.Double(shiftX, shiftY);
            }
            i++;
        }

        return retVal;
    }
}