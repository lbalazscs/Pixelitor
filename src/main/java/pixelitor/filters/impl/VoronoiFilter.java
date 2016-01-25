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

package pixelitor.filters.impl;

import com.jhlabs.image.PointFilter;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Metric;
import pixelitor.utils.ReseedSupport;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class VoronoiFilter extends PointFilter {
    private int numPoints = 10;
    private int[] xCoords;
    private int[] yCoords;
    private int[] colors;
    private Metric metric;
    private boolean useImageColors;

    private int aaRes = 2;
    private int aaRes2 = aaRes * aaRes;

    public VoronoiFilter(String filterName) {
        super(filterName);
    }

    public void setNumPoints(int numPoints) {
        this.numPoints = numPoints;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public void setUseImageColors(boolean useImageColors) {
        this.useImageColors = useImageColors;
    }

    public void setAaRes(int aaRes) {
        this.aaRes = aaRes;
        this.aaRes2 = aaRes * aaRes;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        xCoords = new int[numPoints];
        yCoords = new int[numPoints];
        colors = new int[numPoints];

        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        for (int i = 0; i < numPoints; i++) {
            xCoords[i] = rand.nextInt(src.getWidth());
            yCoords[i] = rand.nextInt(src.getHeight());

            if (useImageColors) {
                colors[i] = src.getRGB(xCoords[i], yCoords[i]);
            } else {
                colors[i] = 0xFF_00_00_00 | rand.nextInt(0xFF_FF_FF);
            }
        }

        BufferedImage result = super.filter(src, dst);

        return result;
    }

    public void showPoints(BufferedImage img) {
        double radius = (img.getWidth() + img.getHeight()) / 300.0;
        if(radius < 1) {
            radius = 1;
        }
        double diameter = 2 * radius;
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        if (useImageColors) {
            g.setXORMode(Color.WHITE);
        }
        for (int i = 0; i < numPoints; i++) {
            g.fill(new Ellipse2D.Double(xCoords[i] - radius, yCoords[i] - radius, diameter, diameter));
        }
        g.dispose();
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        int closestPointIndex = 0;
        double fromHereToClosestSoFar = metric.distanceInt(xCoords[closestPointIndex], x, yCoords[closestPointIndex], y);
        for (int i = 0; i < numPoints; i++) {
            double fromHereToPointI = metric.distanceInt(xCoords[i], x, yCoords[i], y);
            if (fromHereToPointI < fromHereToClosestSoFar) {
                closestPointIndex = i;
                fromHereToClosestSoFar = fromHereToPointI;
            }
        }

        return colors[closestPointIndex];
    }

    /**
     * Finds the nearest point with double precision. Used for AA
     */
    private int nearestSiteDouble(double x, double y) {
        int closestPointIndex = 0;
        double fromHereToClosestSoFar = metric.distanceDouble(xCoords[closestPointIndex], x, yCoords[closestPointIndex], y);
        for (int i = 0; i < numPoints; i++) {
            double fromHereToPointI = metric.distanceDouble(xCoords[i], x, yCoords[i], y);
            if (fromHereToPointI < fromHereToClosestSoFar) {
                closestPointIndex = i;
                fromHereToClosestSoFar = fromHereToPointI;
            }
        }
        return closestPointIndex;
    }

    /**
     * Check whether the pixel is different from its neighbours.
     * It is enough to check in the horizontal direction
     */
    private static boolean isEdge(int[] allPixels, int pixelIndex, int width) {
        int color = allPixels[pixelIndex];
        int colorLeft = allPixels[pixelIndex - 1];
        int colorRight = allPixels[pixelIndex + 1];
        if ((color != colorLeft) || (color != colorRight)) {
            return true;
        }
        int colorUp = allPixels[pixelIndex - width];
        int colorDown = allPixels[pixelIndex + width];
        if ((color != colorUp) || (color != colorDown)) {
            return true;
        }
        return false;
    }

    private int calcSuperSampledColor(int pixelIndex, int width) {
        int x = pixelIndex % width;
        int y = pixelIndex / width;

        int r = 0;
        int g = 0;
        int b = 0;

        for (int i = 0; i < aaRes; i++) {
            double yy = y + 1.0 / aaRes * i - 0.5;
            for (int j = 0; j < aaRes; j++) {
                double xx = x + 1.0 / aaRes * j - 0.5;
                int closestPointIndex = nearestSiteDouble(xx, yy);
                int color = colors[closestPointIndex];
                r += (color >>> 16) & 0xFF;
                g += (color >>> 8) & 0xFF;
                b += color & 0xFF;
            }
        }
        r /= aaRes2;
        g /= aaRes2;
        b /= aaRes2;

        return (0xFF_00_00_00 | (r << 16) | (g << 8) | b);
    }

    public void antiAlias(BufferedImage imgSoFar) {
        assert aaRes != 0;
        int width = imgSoFar.getWidth();
        int[] pixels = ImageUtils.getPixelsAsArray(imgSoFar);

        // make a copy so that the original is inspected for edges
        // while the array is changed
        int[] pixelsCopy = new int[pixels.length];
        System.arraycopy(pixels, 0, pixelsCopy, 0, pixels.length);

        for (int i = 0; i < pixels.length; i++) {
            // only pixels at the edges are supersampled
            boolean edge;
            try {
                edge = isEdge(pixelsCopy, i, width);
            } catch (ArrayIndexOutOfBoundsException e) {
                edge = false;
            }

            if(edge) {
                pixels[i] = calcSuperSampledColor(i, width);
            }
        }
    }
}
