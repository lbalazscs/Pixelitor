/*
 * Copyright 2015 Laszlo Balazs-Csiki
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
    private boolean showPoints;
    private boolean useImageColors;

    public void setNumPoints(int numPoints) {
        this.numPoints = numPoints;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
    }

    public void setUseImageColors(boolean useImageColors) {
        this.useImageColors = useImageColors;
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
        if (showPoints) {
            double radius = (src.getWidth() + src.getHeight()) / 200.0;
            double diameter = 2 * radius;
            Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            for (int i = 0; i < numPoints; i++) {
                g.fill(new Ellipse2D.Double(xCoords[i] - radius, yCoords[i] - radius, diameter, diameter));
            }
            g.dispose();
        }

        return result;
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        int closestPointIndex = 0;
        double fromHereToClosestSoFar = metric.distance(xCoords[closestPointIndex], x, yCoords[closestPointIndex], y);
        for (int i = 0; i < numPoints; i++) {
            double fromHereToPointI = metric.distance(xCoords[i], x, yCoords[i], y);
            if (fromHereToPointI < fromHereToClosestSoFar) {
                closestPointIndex = i;
//                fromHereToClosestSoFar = metric.distance(xCoords[closestPointIndex], x, yCoords[closestPointIndex], y);
                fromHereToClosestSoFar = fromHereToPointI;
            }
        }

        return colors[closestPointIndex];
    }

}
