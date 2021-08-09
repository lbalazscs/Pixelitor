package pixelitor.particles;

import pixelitor.utils.GoldenRatio;
import pixelitor.utils.Rnd;

import java.awt.*;
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
