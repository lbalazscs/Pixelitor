/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import java.util.Random;

/**
 * Generating many random numbers can be expensive,
 * and some filters work just fine with a small number of reused
 * random floats.
 *
 * This is a random number generator that produces fast cached values
 * for nextFloat and regular (slower but with a big period) integers.
 * In a multithreaded environment it is suitable to be used inside a ThreadLocal
 */
public class CachedFloatRandom {
    private static final int CACHE_SIZE = 100;
    private static final float[] cache = new float[CACHE_SIZE];

    private final Random instanceRandom = new Random();

    static {
        rebuildCache(new Random());
    }

    public static void rebuildCache(Random staticRandom) {
        for (int i = 0; i < CACHE_SIZE; i++) {
            cache[i] = staticRandom.nextFloat();
        }
    }

    private int index = 0;

    public float nextFloat() {
        index++;
        if (index >= CACHE_SIZE) {
            index = 0;
        }
        return cache[index];
    }

    // this does not have a short period
    public int nextInt() {
        return instanceRandom.nextInt();
    }

    public void setSeed(int seed) {
        assert seed > 0;
        index = seed % CACHE_SIZE;
        instanceRandom.setSeed(seed);
    }
}
