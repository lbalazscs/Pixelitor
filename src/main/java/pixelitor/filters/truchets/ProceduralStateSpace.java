package pixelitor.filters.truchets;

import com.jhlabs.math.Noise;
import pixelitor.utils.Utils;

import java.util.Arrays;
import java.util.Random;

public enum ProceduralStateSpace {
    RANDOM((w, h, x, y, limit, random, state) -> random.nextInt(limit)),
    WAVE_FUNCTION((w, h, x, y, limit, random, state) -> ((int[][]) state)[x][y]) {
        private void collapse(int x, int y, int w, int h, int v, float likeliness, int[][] space, int limit, Random random) {
            if (x < 0 || y < 0 || x >= w || y >= h || space[x][y] != -1) {
                return;
            }
            if (v == 0) {
                space[x][y] = 1;
            } else if (v == limit - 1) {
                space[x][y] = limit - 2;
            } else {
                space[x][y] = random.nextFloat() > likeliness ? v : (random.nextBoolean() ? v - 1 : v + 1);
            }
            collapse(x - 1, y, w, h, space[x][y], likeliness, space, limit, random);
            collapse(x + 1, y, w, h, space[x][y], likeliness, space, limit, random);
            collapse(x, y - 1, w, h, space[x][y], likeliness, space, limit, random);
            collapse(x, y + 1, w, h, space[x][y], likeliness, space, limit, random);
        }

        @Override
        public Object createState(int w, int h, int limit, Random random) {
            int[][] space = new int[w][h];
            float likeliness = random.nextFloat();
            for (int[] ints : space) {
                Arrays.fill(ints, -1);
            }
            collapse(random.nextInt(w), random.nextInt(h), w, h, random.nextInt(limit), likeliness, space, limit, random);
            return space;
        }
    },
    NOISE((w, h, x, y, limit, random, state) -> (int) (limit * (Noise.noise3(x * 2.5f / w, y * 2.5f / h, ((float) state)) / 2 + 1))) {
        @Override
        public Object createState(int w, int h, int limit, Random random) {
            return random.nextInt() * 1f;
        }
    },
    ;

    final SpaceAction spaceAction;

    ProceduralStateSpace(SpaceAction spaceAction) {
        this.spaceAction = spaceAction;
    }

    public Object createState(int w, int h, int limit, Random random) {
        return null;
    }

    public int getState(int w, int h, int x, int y, int limit, Random random, Object state) {
        return spaceAction.getState(w, h, x, y, limit, random, state);
    }

    public interface SpaceAction {
        int getState(int w, int h, int x, int y, int limit, Random random, Object state);
    }

    @Override
    public String toString() {
        return Utils.screamingSnakeCaseToSentenceCase(super.toString());
    }
}
