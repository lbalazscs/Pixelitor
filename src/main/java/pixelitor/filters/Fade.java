/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import com.jhlabs.image.ImageMath;
import pixelitor.Views;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;
import pixelitor.history.FadeableEdit;
import pixelitor.history.History;
import pixelitor.layers.Drawable;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static pixelitor.utils.Texts.i18n;

/**
 * The Fade filter
 */
public class Fade extends ParametrizedFilter {
    public static final String NAME = i18n("fade");

    private final RangeParam opacityParam =
        new RangeParam(GUIText.OPACITY, 0, 100, 100);
//    private BlendingModeParam blendingModeParam = new BlendingModeParam(BlendingMode.values());

    public Fade() {
        super(true);

        opacityParam.setPresetKey("Opacity (%)");

        setParams(opacityParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        // the fade menu item must be active only if History.canFade()
        assert History.canFade();

        Drawable dr = Views.getActiveDrawable();
        FadeableEdit edit = History.getPreviousEditForFade(dr);
        BufferedImage previous = edit.getBackupImage();

        if (previous == null) {
            // soft reference expired
            return src;
        }

        if (ImageUtils.hasPackedIntArray(src)) {
            dest = fadeRGB(previous, src, dest, opacityParam);
        } else {
            dest = fadeGray(previous, src, dest, opacityParam);
        }

        return dest;
    }

    public void setOpacity(int newOpacity) {
        opacityParam.setValue(newOpacity);
    }

    private static BufferedImage fadeRGB(BufferedImage before, BufferedImage after,
                                         BufferedImage dest, RangeParam opacity) {
        if (opacity.getValue() == 100) {
            return after;
        }

        double fadeFactor = opacity.getPercentage();
        // A simple AlphaComposite would not handle semitransparent pixels correctly
        int[] srcPixels = ImageUtils.getPixels(after);
        int[] destPixels = ImageUtils.getPixels(dest);
        int[] prevPixels = ImageUtils.getPixels(before);

        int length = srcPixels.length;
        for (int i = 0; i < length; i++) {
            int rgb = srcPixels[i];
            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            int prevRGB = prevPixels[i];
            int prevA = (prevRGB >>> 24) & 0xFF;
            int prevR = (prevRGB >>> 16) & 0xFF;
            int prevG = (prevRGB >>> 8) & 0xFF;
            int prevB = prevRGB & 0xFF;

            a = (int) (prevA + fadeFactor * (a - prevA));
            r = (int) (prevR + fadeFactor * (r - prevR));
            g = (int) (prevG + fadeFactor * (g - prevG));
            b = (int) (prevB + fadeFactor * (b - prevB));

            destPixels[i] = a << 24 | r << 16 | g << 8 | b;
        }
        return dest;
    }

    private static BufferedImage fadeGray(BufferedImage before, BufferedImage after,
                                          BufferedImage dest, RangeParam opacity) {
        if (opacity.getValue() == 100) {
            return after;
        }

        double fadeFactor = opacity.getPercentage();
        // A simple AlphaComposite would not handle semitransparent pixels correctly

        byte[] srcPixels = ImageUtils.getGrayPixels(after);
        byte[] destPixels = ImageUtils.getGrayPixels(dest);
        byte[] prevPixels = ImageUtils.getGrayPixels(before);

        int length = srcPixels.length;
        for (int i = 0; i < length; i++) {
            int srcVal = Byte.toUnsignedInt(srcPixels[i]);
            int prevVal = Byte.toUnsignedInt(prevPixels[i]);

            int val = ImageMath.lerp(fadeFactor, prevVal, srcVal);

            destPixels[i] = (byte) val;
        }
        return dest;
    }
}
