/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

public class GoldenRatio {
    public static final double GOLDEN_RATIO = 1.618033988749895;
    public static final float GOLDEN_RATIO_CONJUGATE = 0.618034f;

    private final Color root;
    private final float colorRandomness;
    private final float[] hsbColors;

    public GoldenRatio(Random random, Color root, float colorRandomness) {
        this.root = root;
        this.colorRandomness = colorRandomness;
        hsbColors = Colors.toHSB(Rnd.createRandomColor(random, false));
    }

    public Color next() {
        Color randomColor = new Color(Colors.HSBAtoARGB(hsbColors, root.getAlpha()), true);
        randomColor = Colors.rgbInterpolate(root, randomColor, colorRandomness);
        hsbColors[0] = (hsbColors[0] + GOLDEN_RATIO_CONJUGATE) % 1;
        return randomColor;
    }

    public Color next(Color root) {
        Color randomColor = new Color(Colors.HSBAtoARGB(hsbColors, root.getAlpha()), true);
        randomColor = Colors.rgbInterpolate(root, randomColor, colorRandomness);
        hsbColors[0] = (hsbColors[0] + GOLDEN_RATIO_CONJUGATE) % 1;
        return randomColor;
    }
}
