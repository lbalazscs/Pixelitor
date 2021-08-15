package pixelitor.utils;

import pixelitor.colors.Colors;

import java.awt.Color;
import java.util.Random;

public class GoldenRatio {
    public static final double GOLDEN_RATIO = 1.61803398874989490253;
    public static final float GOLDEN_RATIO_CONJUGATE = 0.618033988749895f;

    private final Random random;
    private final Color root;
    private final float colorRandomness;
    float[] hsbColors;

    public GoldenRatio(Random random, Color root, float colorRandomness) {
        this.random = random;
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
