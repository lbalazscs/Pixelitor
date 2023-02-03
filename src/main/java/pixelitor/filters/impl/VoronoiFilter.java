/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.AppContext;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Metric;
import pixelitor.utils.PoissonDiskSampling;
import pixelitor.utils.ReseedSupport;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Voronoi Diagram filter implementation
 */
public class VoronoiFilter extends PointFilter {
    private double distanceBetweenPoints;
    private Metric metric;
    private boolean useImageColors;

    private int aaRes = 2;
    private int aaRes2 = aaRes * aaRes;

    private PoissonDiskSampling sampling;
    private int[] colors;

    public VoronoiFilter(String filterName) {
        super(filterName);
    }

    public void setDistanceBetweenPoints(double distanceBetweenPoints) {
        this.distanceBetweenPoints = distanceBetweenPoints;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public void setUseImageColors(boolean useImageColors) {
        this.useImageColors = useImageColors;
    }

    public void setAaRes(int aaRes) {
        this.aaRes = aaRes;
        aaRes2 = aaRes * aaRes;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        SplittableRandom rand = ReseedSupport.getLastSeedSRandom();

        sampling = new PoissonDiskSampling(width, height, distanceBetweenPoints, 10, true, rand);
        List<Point2D> points = sampling.getSamples();
        int numPoints = points.size();
        colors = new int[numPoints];

        for (int i = 0; i < numPoints; i++) {
            int color;
            if (useImageColors) {
                Point2D point = points.get(i);
                color = src.getRGB((int) point.getX(), (int) point.getY());
            } else {
                color = 0xFF_00_00_00 | rand.nextInt(0xFF_FF_FF);
            }
            colors[i] = color;
        }

        return super.filter(src, dst);
    }

    public void showPoints(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        if (useImageColors) {
            g.setXORMode(Color.WHITE);
        }
        double radius = (img.getWidth() + img.getHeight()) / 300.0;
        if (radius < 1) {
            radius = 1;
        }

        sampling.showSamples(g, radius);

        g.dispose();
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        int closestIndex = sampling.findClosestPointTo(x, y,
            metric.asIntPrecisionDistance());
        if (closestIndex == -1) {
            // there wasn't a point in the cell or in its neighbours
            if (AppContext.isDevelopment()) {
                throw new IllegalStateException(String.format(
                    "x = %d, y = %d", x, y));
            }
            return 0xFF_FF_FF_FF;
        }

        return colors[closestIndex];
    }

    /**
     * Check whether the pixel is different from its neighbours.
     */
    private static boolean isEdge(int[] allPixels, int pixelIndex, int width) {
        int color = allPixels[pixelIndex];
        int colorLeft = allPixels[pixelIndex - 1];
        int colorRight = allPixels[pixelIndex + 1];
        if (color != colorLeft || color != colorRight) {
            return true;
        }
        int colorUp = allPixels[pixelIndex - width];
        int colorDown = allPixels[pixelIndex + width];
        return color != colorUp || color != colorDown;
    }

    private static boolean tryIsEdge(int width, int[] pixelsCopy, int i) {
        boolean edge;
        try {
            edge = isEdge(pixelsCopy, i, width);
        } catch (ArrayIndexOutOfBoundsException e) {
            edge = false;
        }
        return edge;
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
                // xx and yy are the supersampling coordinates
                int closestIndex = sampling.findClosestPointTo(xx, yy, metric.asDoublePrecisionDistance());
                int color = colors[closestIndex];
                r += (color >>> 16) & 0xFF;
                g += (color >>> 8) & 0xFF;
                b += color & 0xFF;
            }
        }
        r /= aaRes2;
        g /= aaRes2;
        b /= aaRes2;

        return 0xFF_00_00_00 | r << 16 | g << 8 | b;
    }

    public void antiAlias(BufferedImage imgSoFar) {
        assert aaRes != 0;
        int width = imgSoFar.getWidth();
        int[] pixels = ImageUtils.getPixelArray(imgSoFar);

        // since this code runs outside the superclass-powered
        // parallelization, use parallel streams
        int[] aaPixels = IntStream.range(0, pixels.length).parallel()
            .map(i -> {
                // only pixels at the edges are supersampled
                if (tryIsEdge(width, pixels, i)) {
                    return calcSuperSampledColor(i, width);
                } else {
                    return pixels[i];
                }
            }).toArray();
        System.arraycopy(aaPixels, 0, pixels, 0, pixels.length);
    }

    public void debugGrid(BufferedImage dest) {
        Graphics2D g2 = dest.createGraphics();
        g2.setColor(Color.WHITE);
        sampling.showGrid(g2);
        g2.dispose();
    }
}
