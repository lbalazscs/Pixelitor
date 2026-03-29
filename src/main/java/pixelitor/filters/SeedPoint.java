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

import pixelitor.utils.CustomShapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A seed point for a Voronoi cell.
 */
class SeedPoint {
    final double x;
    final double y;
    final List<SeedPoint> neighbors = new ArrayList<>();

    SeedPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Draws the seed point and arrows to its neighbors (for debugging/visualization).
     */
    void debugRender(Graphics2D g2) {
        for (SeedPoint neighbor : neighbors) {
            CustomShapes.drawDirectionArrow(g2, x, y, neighbor.x, neighbor.y);
        }
        CustomShapes.fillCircle(x, y, 10, Color.RED, g2);
    }
}
