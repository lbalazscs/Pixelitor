/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.painter.effects.GlowPathEffect;
import org.jdesktop.swingx.painter.effects.InnerGlowPathEffect;
import org.jdesktop.swingx.painter.effects.NeonBorderEffect;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.filters.painters.AreaEffects;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.awt.font.TextAttribute.*;
import static java.text.AttributedCharacterIterator.Attribute;

/**
 * Static utility methods related to random numbers
 */
public class Rnd {
    private static final Random rand = new Random();

    private Rnd() {
        // do not instantiate
    }

    public static int chooseFrom(int[] items) {
        return items[rand.nextInt(items.length)];
    }

    @SafeVarargs
    public static <T> T chooseFrom(T... items) {
        return items[rand.nextInt(items.length)];
    }

    public static <T> T chooseFrom(List<T> items) {
        return items.get(rand.nextInt(items.size()));
    }

    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    public static Point nextPoint(Rectangle bounds) {
        int x = intInRange(bounds.x, bounds.x + bounds.width);
        int y = intInRange(bounds.y, bounds.y + bounds.height);
        return new Point(x, y);
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

    public static double nextDouble() {
        return rand.nextDouble();
    }

    public static float nextFloat() {
        return rand.nextFloat();
    }

    public static Color createRandomColor() {
        return createRandomColor(false);
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

    public static AreaEffects createRandomEffects() {
        var ae = new AreaEffects();
        float f = rand.nextFloat();
        if (f < 0.25f) {
            ae.setNeonBorder(new NeonBorderEffect());
        } else if (f < 0.5f) {
            ae.setDropShadow(new ShadowPathEffect(1.0f));
        } else if (f < 0.75f) {
            ae.setInnerGlow(new InnerGlowPathEffect(1.0f));
        } else {
            ae.setGlow(new GlowPathEffect(1.0f));
        }
        return ae;
    }

    public static Font createRandomFont() {
        Map<Attribute, Object> attributes = new HashMap<>();

        attributes.put(SIZE, intInRange(10, 100));
        if (nextBoolean()) {
            attributes.put(WEIGHT, WEIGHT_BOLD);
        }
        if (nextBoolean()) {
            attributes.put(POSTURE, POSTURE_OBLIQUE);
        }
        if (nextBoolean()) {
            attributes.put(KERNING, KERNING_ON);
        }
        if (nextBoolean()) {
            attributes.put(TRACKING, TRACKING_LOOSE);
        }
        if (nextBoolean()) {
            attributes.put(LIGATURES, LIGATURES_ON);
        }
        if (nextBoolean()) {
            attributes.put(STRIKETHROUGH, STRIKETHROUGH_ON);
        }
        if (nextBoolean()) {
            attributes.put(UNDERLINE, UNDERLINE_ON);
        }

        return new Font(attributes);
    }

    public static boolean withProbability(double p, Runnable task) {
        assert p >= 0 && p <= 1;
        if (p > rand.nextDouble()) {
            task.run();
            return true;
        }
        return false;
    }

}
