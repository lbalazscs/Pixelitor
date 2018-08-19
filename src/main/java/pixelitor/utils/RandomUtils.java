/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class RandomUtils {
    private static final Random rand = new Random();

    private RandomUtils() {
        // static utility methods, do not instantiate
    }

    @SafeVarargs
    public static <T> T chooseFrom(T... items) {
        return items[rand.nextInt(items.length)];
    }

    public static <T> T chooseFrom(List<T> items) {
        return items.get(rand.nextInt(items.size()));
    }

    public static Color createRandomColor(boolean randomAlpha) {
        return createRandomColor(rand, randomAlpha);
    }

    public static Color createRandomColor(Random rnd, boolean randomAlpha) {
        int r = rnd.nextInt(256);
        int g = rnd.nextInt(256);
        int b = rnd.nextInt(256);

        if (randomAlpha) {
            int a = rnd.nextInt(256);
            return new Color(r, g, b, a);
        }

        return new Color(r, g, b);
    }

    public static String createRandomString(int length) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz -".toCharArray();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = chars[rand.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    public static int intInRange(int min, int max) {
        assert max - min + 1 > 0 : "max = " + max + ", min = " + min;
        return min + rand.nextInt(max - min + 1);
    }

    public static long nextLong() {
        return rand.nextLong();
    }

    public static double nextGaussian() {
        return rand.nextGaussian();
    }
}
