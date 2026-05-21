/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.utils.Metric.DistanceFunction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static com.jhlabs.image.ImageMath.SQRT_2;

/**
 * Poisson disk sampling generates a set of randomly distributed points with
 * the constraint that no two points are closer than a given minimum distance.
 *
 * This class implements "Fast Poisson Disk Sampling in Arbitrary Dimensions" by Robert Bridson.
 * If the "improved" constructor argument is true, then it uses
 * the algorithm improvement by Martin Roberts described at
 * http://extremelearning.com.au/an-improved-version-of-bridsons-algorithm-n-for-poisson-disc-sampling/
 */
public class PoissonDiskSampling {
    // dimensions of the sampling region
    private final int width;
    private final int height;

    private final double rSq; // minimum squared distance between points

    private final List<Point2D> samples; // the generated points

    // acceleration grid parameters
    private final int[][] grid;
    private final int numHorCells;
    private final int numVerCells;
    private final double cellWidth;
    private final double cellHeight;

    public PoissonDiskSampling(int width, int height, double minDist, int k,
                               boolean improved, RandomGenerator rnd) {
        this.width = width;
        this.height = height;
        this.rSq = minDist * minDist;

        // calculate grid cell size to ensure each cell
        // can contain at most one point
        double cellSize = minDist / SQRT_2;
        numHorCells = (int) Math.ceil(width / cellSize);
        numVerCells = (int) Math.ceil(height / cellSize);
        cellWidth = width / (double) numHorCells;
        cellHeight = height / (double) numVerCells;

        // initialize acceleration grid with -1 (empty cells)
        grid = new int[numHorCells][numVerCells];
        for (int x = 0; x < numHorCells; x++) {
            for (int y = 0; y < numVerCells; y++) {
                grid[x][y] = -1;
            }
        }

        // start with the center point
        Point2D initialSample = new Point2D.Double(width / 2.0, height / 2.0);
        addSamplePoint(initialSample, 0);

        samples = new ArrayList<>();
        samples.add(initialSample);

        List<Point2D> activeList = new ArrayList<>();
        activeList.add(initialSample);

        while (!activeList.isEmpty()) {
            int randomIndex = rnd.nextInt(activeList.size());
            Point2D activePoint = activeList.get(randomIndex);
            boolean goodPointFound = false;
            for (int i = 0; i < k; i++) {
                Vector2D candidate;
                double angle = 2 * Math.PI * rnd.nextDouble();
                if (improved) {
                    double dist = minDist + 0.0000001;
                    candidate = Vector2D.createFromPolar(angle, dist);
                } else {
                    candidate = Vector2D.createUnitFromAngle(angle);
                    double dist = minDist + rnd.nextDouble() * minDist;
                    candidate.multiply(dist);
                }
                candidate.add(activePoint);

                boolean reject;
                if (candidate.x < 0 || candidate.x >= width) {
                    reject = true;
                } else if (candidate.y < 0 || candidate.y >= height) {
                    reject = true;
                } else {
                    reject = hasNearbyPoints(candidate);
                }

                if (!reject) {
                    goodPointFound = true;
                    Point2D newPoint = candidate.toPoint();
                    addSamplePoint(newPoint, samples.size());
                    samples.add(newPoint);
                    activeList.add(newPoint);
                    break;
                }
            }
            if (!goodPointFound) {
                // O(1) remove the element at randomIndex
                activeList.set(randomIndex, activeList.getLast());
                activeList.removeLast();
            }
        }
    }

    private void addSamplePoint(Point2D p, int atIndex) {
        int gridX = (int) (p.getX() / cellWidth);
        int gridY = (int) (p.getY() / cellHeight);
        grid[gridX][gridY] = atIndex;
    }

    private boolean hasNearbyPoints(Vector2D candidate) {
        int gridX = (int) (candidate.x / cellWidth);
        int gridY = (int) (candidate.y / cellHeight);

        // check neighboring cells for nearby points (must be a 5x5 grid)
        for (int gx = gridX - 2; gx <= gridX + 2; gx++) {
            if (gx < 0 || gx >= numHorCells) {
                continue;
            }
            for (int gy = gridY - 2; gy <= gridY + 2; gy++) {
                if (gy < 0 || gy >= numVerCells) {
                    continue;
                }
                int index = grid[gx][gy];
                if (index != -1) {
                    Point2D point = samples.get(index);
                    double dx = candidate.x - point.getX();
                    double dy = candidate.y - point.getY();
                    if (dx * dx + dy * dy < rSq) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void renderGrid(Graphics2D g2) {
        for (double y = cellHeight; y < height; y += cellHeight) {
            g2.draw(new Line2D.Double(0, y, width, y));
        }
        for (double x = cellWidth; x < width; x += cellWidth) {
            g2.draw(new Line2D.Double(x, 0, x, height));
        }
    }

    public void renderPoints(Graphics2D g2, double radius) {
        for (Point2D sample : samples) {
            g2.fill(CustomShapes.createCircle(sample, radius));
        }
    }

    public void renderPoints(Graphics2D g2, double radius, Color[] colors) {
        for (int i = 0; i < samples.size(); i++) {
            g2.setColor(colors[i % colors.length]);
            g2.fill(CustomShapes.createCircle(samples.get(i), radius));
        }
    }

    /**
     * Returns the index of the closest sample to the given (x, y) coordinates.
     */
    public int findClosestPointIndex(double x, double y, DistanceFunction distanceFunction) {
        int gridX = (int) (x / cellWidth);
        int gridY = (int) (y / cellHeight);

        double minDistSoFar = Double.POSITIVE_INFINITY;
        int indexOfClosest = -1;

        int searchDist = 0;
        // search additional rings after finding the first point to
        // cover all cells that could possibly contain a closer point
        int extraRings = 1; // seems to be enough

        while (true) {
            for (int gx = gridX - searchDist; gx <= gridX + searchDist; gx++) {
                if (gx < 0 || gx >= numHorCells) {
                    continue;
                }
                for (int gy = gridY - searchDist; gy <= gridY + searchDist; gy++) {
                    if (gy < 0 || gy >= numVerCells) {
                        continue;
                    }

                    // skips the inner area that was already checked in previous iterations
                    if (searchDist > 0 && Math.abs(gx - gridX) < searchDist && Math.abs(gy - gridY) < searchDist) {
                        gy = gridY + searchDist - 1;
                        continue;
                    }

                    int index = grid[gx][gy];
                    if (index != -1) {
                        Point2D point = samples.get(index);
                        double dist = distanceFunction.apply(x, y, point.getX(), point.getY());
                        if (dist < minDistSoFar) {
                            minDistSoFar = dist;
                            indexOfClosest = index;
                        }
                    }
                }
            }

            if (indexOfClosest != -1) {
                // we've searched the necessary buffer area to guarantee the absolute closest point
                if (extraRings <= 0) {
                    return indexOfClosest;
                }
                extraRings--;
            }
            searchDist++;

            // fallback safety break if the grid is abnormally sparse
            if (searchDist > Math.max(numHorCells, numVerCells)) {
                return indexOfClosest;
            }
        }
    }

    public List<Point2D> getSamples() {
        return samples;
    }
}
