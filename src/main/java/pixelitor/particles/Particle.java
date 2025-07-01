/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.utils.Vector2D;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * Represents a single particle with position, velocity, and color.
 */
public abstract class Particle {
    public Point2D pos;
    protected Point2D lastPos;

    public Vector2D vel;
    public Color color;
    public int iterationIndex;
    public int groupIndex = -1;

    /**
     * Flushes any buffered drawing operations for this particle.
     */
    public abstract void flush();

    /**
     * Resets the particle to its initial state.
     */
    public abstract void reset();

    /**
     * Checks if the particle is no longer active.
     */
    public abstract boolean isDead();

    /**
     * Updates the particle's state for a single time step.
     */
    public abstract void update();
}
