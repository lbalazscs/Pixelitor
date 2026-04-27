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

package pixelitor.particles;

import java.util.Random;

/**
 * An interface for modifying a particle's properties.
 */
public interface Modifier<P extends Particle> {
    /**
     * Applies a modification to the given particle.
     */
    void modify(P particle);

    /**
     * A modifier that sets a particle's position to a random location within a rectangle.
     */
    record RandomizePosition<P extends Particle>(int x, int y, int width, int height,
                                                 Random random) implements Modifier<P> {
        @Override
        public void modify(P particle) {
            particle.pos.setLocation(x + width * random.nextDouble(), y + height * random.nextDouble());
        }
    }
}
