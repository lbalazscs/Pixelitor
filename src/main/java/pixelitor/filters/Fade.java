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

package pixelitor.filters;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.history.History;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * The Fade filter
 */
public class Fade extends ParametrizedFilter {
    private static final int FADE_MIN = 0;
    private static final int FADE_MAX = 100;
    private static final int FADE_INIT = 100;

    private final RangeParam opacityParam = new RangeParam("Opacity (%)", FADE_MIN, FADE_INIT, FADE_MAX
    );
//    private BlendingModeParam blendingModeParam = new BlendingModeParam(BlendingMode.values());

    public Fade() {
        super(ShowOriginal.YES);

        setParams(opacityParam);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        // the fade menu item must be active only if History.canFade()
        assert History.canFade();

        BufferedImage previous = OpenImages.getActiveCompOpt()
                .flatMap(Composition::getActiveDrawableOpt)
                .flatMap(History::getPreviousEditForFade)
                .orElseThrow(() -> new IllegalStateException("no FadeableEdit"))
                .getBackupImage();

        if (previous == null) {
            // soft reference expired
            return src;
        }

        dest = fade(previous, src, dest, opacityParam);

        return dest;
    }

    public void setOpacity(int newOpacity) {
        opacityParam.setValue(newOpacity);
    }

    public static BufferedImage fade(BufferedImage before, BufferedImage after,
                                     BufferedImage dest, RangeParam opacity) {
        if (opacity.getValue() == 100) {
            return after;
        }

        float fadeFactor = opacity.getPercentageValF();
        // A simple AlphaComposite would not handle semitransparent pixels correctly
        int[] srcData = ImageUtils.getPixelsAsArray(after);
        int[] destData = ImageUtils.getPixelsAsArray(dest);
        int[] prevData = ImageUtils.getPixelsAsArray(before);

        int length = srcData.length;
        for (int i = 0; i < length; i++) {
            int rgb = srcData[i];
            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            int prevRGB = prevData[i];
            int prevA = (prevRGB >>> 24) & 0xFF;
            int prevR = (prevRGB >>> 16) & 0xFF;
            int prevG = (prevRGB >>> 8) & 0xFF;
            int prevB = prevRGB & 0xFF;

            a = (int) (prevA + fadeFactor * (a - prevA));
            r = (int) (prevR + fadeFactor * (r - prevR));
            g = (int) (prevG + fadeFactor * (g - prevG));
            b = (int) (prevB + fadeFactor * (b - prevB));

            destData[i] = a << 24 | r << 16 | g << 8 | b;
        }
        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
