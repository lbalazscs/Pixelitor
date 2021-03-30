/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.Metric.DistanceFunction;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Voronoi Diagram filter implementation
 */
public class VoronoiFilter extends PointFilter {
    private int numPoints = 10;
    private Metric metric;
    private boolean useImageColors;

    private int aaRes = 2;
    private int aaRes2 = aaRes * aaRes;
    private double gridPixelWidth;
    private double gridPixelHeight;
    private GridCell[][] cells;

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
        aaRes2 = aaRes * aaRes;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        Random rand = ReseedSupport.reInitialize();

        // Determining the number of cells is tricky because more
        // cells mean better performance, but if a cell and all
        // of its neighbors are empty, then the algorithm fails.
        // Also the cells should be more or less square-shaped.
        double est = Math.sqrt(numPoints / 10.0);
        double aspectRatio = width / (double) height;
        int numVerGridCells;
        int numHorGridCells;
        if (aspectRatio > 1.0) {
            numHorGridCells = calcNumGridCells(width, est);
            numVerGridCells = calcNumGridCells(height, est / aspectRatio);
        } else {
            numHorGridCells = calcNumGridCells(width, est * aspectRatio);
            numVerGridCells = calcNumGridCells(height, est);
        }

        gridPixelWidth = width / (double) numHorGridCells;
        gridPixelHeight = height / (double) numVerGridCells;

        cells = new GridCell[numHorGridCells][numVerGridCells];
        for (int i = 0; i < numHorGridCells; i++) {
            GridCell[] column = new GridCell[numVerGridCells];
            cells[i] = column;
            for (int j = 0; j < numVerGridCells; j++) {
                column[j] = new GridCell(i, j);
            }
        }

        for (int i = 0; i < numHorGridCells; i++) {
            GridCell[] column = cells[i];
            GridCell[] leftCol = null;
            if (i > 0) {
                leftCol = cells[i - 1];
            }
            GridCell[] rightCol = null;
            if (i < numHorGridCells - 1) {
                rightCol = cells[i + 1];
            }

            for (int j = 0; j < numVerGridCells; j++) {
                GridCell cell = column[j];
                if (leftCol != null) {
                    cell.addNeighbour(leftCol[j]); // west
                    if (j > 0) {
                        cell.addNeighbour(leftCol[j - 1]); // north-west
                    }
                    if (j < numVerGridCells - 1) {
                        cell.addNeighbour(leftCol[j + 1]); // south-west
                    }
                }
                if (j > 0) {
                    cell.addNeighbour(column[j - 1]); // north
                }
                if (j < numVerGridCells - 1) {
                    cell.addNeighbour(column[j + 1]); // south
                }
                if (rightCol != null) {
                    cell.addNeighbour(rightCol[j]); // east
                    if (j > 0) {
                        cell.addNeighbour(rightCol[j - 1]); // north-east
                    }
                    if (j < numVerGridCells - 1) {
                        cell.addNeighbour(rightCol[j + 1]); // south-east
                    }
                }
            }
        }

        for (int i = 0; i < numPoints; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            int color;

            if (useImageColors) {
                color = src.getRGB(x, y);
            } else {
                color = 0xFF_00_00_00 | rand.nextInt(0xFF_FF_FF);
            }
            VorPoint point = new VorPoint(x, y, color);

            int gridIndexX = (int) (x / gridPixelWidth);
            int gridIndexY = (int) (y / gridPixelHeight);
            cells[gridIndexX][gridIndexY].points.add(point);
        }

        return super.filter(src, dst);
    }

    private int calcNumGridCells(int size, double estimated) {
        int numGridCells;
        if (estimated > size / 3.0) { // can happen for very small images
            // the cell size should be at least 3 pixels
            numGridCells = 1 + size / 3;
        } else {
            numGridCells = 1 + (int) estimated;
        }
        return numGridCells;
    }

    public void debugGrid(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        Graphics2D rg = img.createGraphics();
        double yy = 0;
        while (yy < height) {
            yy += gridPixelHeight;
            Line2D.Double line = new Line2D.Double(0, yy, width, yy);
            rg.draw(line);
        }
        double xx = 0;
        while (xx < width) {
            xx += gridPixelWidth;
            Line2D.Double line = new Line2D.Double(xx, 0, xx, height);
            rg.draw(line);
        }
        rg.dispose();
    }

    public void showPoints(BufferedImage img) {
        double radius = (img.getWidth() + img.getHeight()) / 300.0;
        if (radius < 1) {
            radius = 1;
        }
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        if (useImageColors) {
            g.setXORMode(Color.WHITE);
        }
        for (GridCell[] cols : cells) {
            for (GridCell cell : cols) {
                for (VorPoint point : cell.points) {
                    g.fill(Shapes.createCircle(point.x, point.y, radius));
                }
            }
        }
        g.dispose();
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        int gridIndexX = (int) (x / gridPixelWidth);
        int gridIndexY = (int) (y / gridPixelHeight);
        VorPoint closest = cells[gridIndexX][gridIndexY].findClosestPointTo(x, y,
            metric.asIntPrecisionDistance());
        if (closest == null) {
            // there wasn't a point in the cell or in its neighbours
            if (AppContext.isDevelopment()) {
                throw new IllegalStateException(String.format(
                    "gridIndexX = %d, gridIndexY = %d", gridIndexX, gridIndexY));
            }
            return 0xFF_FF_FF_FF;
        }

        return closest.color;
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
        if (color != colorUp || color != colorDown) {
            return true;
        }
        return false;
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
                int gridIndexX = (int) (xx / gridPixelWidth);
                int gridIndexY = (int) (yy / gridPixelHeight);
                VorPoint closest = cells[gridIndexX][gridIndexY].findClosestPointTo(xx, yy,
                    metric.asDoublePrecisionDistance());
                int color = closest.color;
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
        int[] pixels = ImageUtils.getPixelsAsArray(imgSoFar);

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

    private record VorPoint(int x, int y, int color) {
        @Override
        public String toString() {
            return "{x=" + x + ", y=" + y + '}';
        }
    }

    private static class GridCell {
        final List<VorPoint> points = new ArrayList<>();
        final List<GridCell> neighbours = new ArrayList<>();
        final int horPos;
        final int verPos;

        public GridCell(int horPos, int verPos) {
            this.horPos = horPos;
            this.verPos = verPos;
        }

        public void addNeighbour(GridCell n) {
            if (n == this) {
                throw new IllegalStateException();
            }
            neighbours.add(n);
        }

        public VorPoint findClosestPointTo(double x, double y, DistanceFunction distFunc) {
            double minDistSoFar = Double.POSITIVE_INFINITY;
            VorPoint closest = null;

            for (VorPoint point : points) {
                double dist = distFunc.apply(x, point.x, y, point.y);
                if (dist < minDistSoFar) {
                    minDistSoFar = dist;
                    closest = point;
                }
            }

            for (GridCell neighbour : neighbours) {
                for (VorPoint point : neighbour.points) {
                    double dist = distFunc.apply(x, point.x, y, point.y);
                    if (dist < minDistSoFar) {
                        minDistSoFar = dist;
                        closest = point;
                    }
                }
            }

            return closest;
        }

        public String toPosString() {
            return new StringJoiner(", ", "Cell(", ")")
                .add("" + horPos)
                .add("" + verPos)
                .toString();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "Cell[", "]")
                .add("" + horPos)
                .add("" + verPos)
                .add("neighbours=" + neighbours.stream().map(GridCell::toPosString).collect(Collectors.joining(",")))
                .toString();
        }
    }
}
