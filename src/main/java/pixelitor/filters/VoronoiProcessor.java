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

package pixelitor.filters;

import pixelitor.utils.ProgressTracker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Computes and renders Voronoi cells from a set of seed points.
 */
public class VoronoiProcessor {
    private static final boolean SHOW_SEED_POINTS = false;

    private VoronoiProcessor() {
    }

    private record Point(double x, double y) {
    }

    /**
     * Clips a polygon against the perpendicular bisector of two points A and B.
     * Uses the Sutherland-Hodgman algorithm to retain only the half of the polygon closest to point A.
     */
    private static List<Point> clipByPerpendicularBisector(List<Point> poly, SeedPoint a, SeedPoint b) {
        int numPoints = poly.size();
        List<Point> result = new ArrayList<>(numPoints + 1);

        // the perpendicular bisector is defined by the midpoint between A
        // and B, and a normal direction (dx, dy) — pointing from A toward B
        double midX = (a.x + b.x) / 2.0;
        double midY = (a.y + b.y) / 2.0;
        double dx = b.x - a.x;
        double dy = b.y - a.y;

        Point current = poly.getLast();

        // on which side of the bisector is this point
        boolean isCurrentInside = (current.x() - midX) * dx + (current.y() - midY) * dy < 0;

        for (Point next : poly) {
            boolean isNextInside = (next.x() - midX) * dx + (next.y() - midY) * dy < 0;

            if (isCurrentInside) {
                result.add(current);
            }

            if (isCurrentInside ^ isNextInside) {
                // one inside, one outside => an intersection must be added
                double t = intersect(current, next, midX, midY, dx, dy);
                double x = current.x() + t * (next.x() - current.x());
                double y = current.y() + t * (next.y() - current.y());
                result.add(new Point(x, y));
            }

            current = next;
            isCurrentInside = isNextInside;
        }
        return result;
    }

    /**
     * Computes the parameter t ∈ [0, 1] where the segment p1-p2 intersects the bisector line.
     */
    private static double intersect(Point p1, Point p2, double mx, double my, double dx, double dy) {
        double vx = p2.x() - p1.x();
        double vy = p2.y() - p1.y();

        double num = (mx - p1.x()) * dx + (my - p1.y()) * dy;
        double den = vx * dx + vy * dy;

        if (Math.abs(den) < 1.0e-9) {
            // the edge runs parallel to the bisector => no intersection
            // exists, so return `t = 0` as a safe fallback
            return 0;
        }
        return num / den;
    }

    /**
     * Constructs the Voronoi cell polygon for the given seed point by progressively
     * clipping a large bounding rectangle using the seed’s neighbors.
     */
    private static List<Point> computeCell(SeedPoint seed, int width, int height, int edgeWidth) {
        // starts with a bounding box large enough to cover the entire image
        int x = -edgeWidth;
        int y = -edgeWidth;
        int maxX = width + edgeWidth;
        int maxY = height + edgeWidth;

        List<Point> poly = List.of(
            new Point(x, y),
            new Point(maxX, y),
            new Point(maxX, maxY),
            new Point(x, maxY)
        );

        // only clip against the assigned local neighbors
        for (SeedPoint other : seed.neighbors) {
            // removes the part that belongs to the neighbor’s region
            poly = clipByPerpendicularBisector(poly, seed, other);
            if (poly.isEmpty()) {
                break; // everything is gone, further clipping can't bring it back
            }
            // if the list is not empty, then it has at least 3 points
        }
        return poly;
    }

    static void render(List<SeedPoint> seeds, Graphics2D g2,
                       BufferedImage src, ProgressTracker pt,
                       int edgeWidth, Color edgeColor) {
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        if (edgeWidth > 0) {
            g2.setStroke(new BasicStroke(edgeWidth));
        }

        int width = src.getWidth();
        int height = src.getHeight();
        for (SeedPoint seed : seeds) {
            List<Point> cell = computeCell(seed, width, height, edgeWidth);
            if (cell.isEmpty()) {
                continue;
            }

            renderCell(cell, seed, g2, src, edgeWidth, edgeColor, width, height);

            pt.unitDone();
        }

        if (SHOW_SEED_POINTS) {
            for (SeedPoint seed : seeds) {
                seed.debugRender(g2);
            }
        }
    }

    private static void renderCell(List<Point> cell, SeedPoint seed, Graphics2D g2, BufferedImage src, int edgeWidth, Color edgeColor, int width, int height) {
        Path2D path = new Path2D.Double();
        Point firstPoint = cell.getFirst();
        path.moveTo(firstPoint.x(), firstPoint.y());

        for (int j = 1; j < cell.size(); j++) {
            Point p = cell.get(j);
            path.lineTo(p.x(), p.y());
        }
        path.closePath();

        int sampleX = Math.clamp((int) seed.x, 0, width - 1);
        int sampleY = Math.clamp((int) seed.y, 0, height - 1);
        g2.setColor(new Color(src.getRGB(sampleX, sampleY)));

        g2.fill(path);

        if (edgeWidth > 0) {
            g2.setColor(edgeColor);
            g2.draw(path);
        }
    }
}
