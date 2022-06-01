/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.particles;

import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public abstract class SmoothPathParticle extends Particle {
    private final List<Point2D> pathPoints;
    private Graphics2D g2 = null;
    protected final Graphics2D[] gc;

    protected SmoothPathParticle(Graphics2D[] gc) {
        this.pathPoints = new ArrayList<>();
        this.gc = gc;
    }

    public void addPoint(Point2D point) {
        pathPoints.add(point);
    }

    @Override
    public void flush() {
        if (isPathReady()) {
            Graphics2D g = getGraphics();
            g.setColor(color);
            g.draw(getPath());
        }
        pathPoints.clear();
    }

    private boolean isPathReady() {
        return pathPoints.size() >= 3;
    }

    private Shape getPath() {
        return Shapes.smoothConnect(pathPoints, 0.5);
    }

    protected Graphics2D getGraphics() {
        if (g2 != null) {
            return g2;
        } else if (gc != null) {
            if (groupIndex != -1) {
                return g2 = gc[groupIndex];
            }
        }
        throw new IllegalStateException("Either Graphics2D or it's array has to be non-null!");
    }
}
