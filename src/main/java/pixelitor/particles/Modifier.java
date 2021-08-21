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

package pixelitor.particles;

import pixelitor.utils.GoldenRatio;
import pixelitor.utils.Rnd;

import java.awt.Color;
import java.util.Random;

public interface Modifier<P extends Particle> {
    void modify(P particle);

    record RandomizePosition<P extends Particle>(int x, int y, int width, int height,
                                                 Random random) implements Modifier<P> {
        public RandomizePosition(int width, int height, Random random) {
            this(0, 0, width, height, random);
        }

        @Override
        public void modify(P particle) {
            particle.pos.setLocation(x + width * random.nextDouble(), y + height * random.nextDouble());
        }
    }

    record RandomizeColor<P extends Particle>(Random random, boolean randomizeAlpha) implements Modifier<P> {
        public RandomizeColor(Random random) {
            this(random, true);
        }

        @Override
        public void modify(P particle) {
            particle.color = Rnd.createRandomColor(random, randomizeAlpha);
        }
    }

    record RandomGoldenRatioColor<P extends Particle>(GoldenRatio goldenRatio) implements Modifier<P> {
        public RandomGoldenRatioColor(Random random, Color root, float colorRandomness) {
            this(new GoldenRatio(random, root, colorRandomness));
        }

        public RandomGoldenRatioColor(Random random) {
            this(new GoldenRatio(random, Color.WHITE, 1));
        }

        @Override
        public void modify(P particle) {
            particle.color = goldenRatio.next();
        }
    }
}
