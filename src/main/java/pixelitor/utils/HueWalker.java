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

package pixelitor.utils;

import pixelitor.colors.Colors;

import java.awt.Color;
import java.util.Random;

/**
 * Generates a sequence of well‑distributed, visually distinct colors
 * by blending an anchor color with a variable random color
 * whose hue advances by the golden ratio conjugate each step.
 */
public class HueWalker {
    private static final float GOLDEN_RATIO_CONJUGATE = 0.618034f;

    private final Color anchor;
    private final double colorRandomness;
    private final float[] hsbColors;

    public HueWalker(Random random, Color anchor, double colorRandomness) {
        this.anchor = anchor;
        this.colorRandomness = colorRandomness;
        hsbColors = Colors.toHSB(Rnd.createRandomColor(random, false));
    }

    /**
     * Generates the next color using the anchor color given in the constructor.
     */
    public Color next() {
        Color randomColor = new Color(Colors.hsbToARGB(hsbColors, anchor.getAlpha()), true);
        randomColor = Colors.interpolateRGB(anchor, randomColor, colorRandomness);
        hsbColors[0] = (hsbColors[0] + GOLDEN_RATIO_CONJUGATE) % 1;
        return randomColor;
    }

    /**
     * Generates the next color using the given anchor color (overriding the constructor parameter).
     */
    public Color next(Color newAnchor) {
        Color randomColor = new Color(Colors.hsbToARGB(hsbColors, newAnchor.getAlpha()), true);
        randomColor = Colors.interpolateRGB(newAnchor, randomColor, colorRandomness);
        hsbColors[0] = (hsbColors[0] + GOLDEN_RATIO_CONJUGATE) % 1;
        return randomColor;
    }
}
