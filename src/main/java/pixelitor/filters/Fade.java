/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters;

import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.history.FadeableEdit;
import pixelitor.history.History;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

public class Fade extends FilterWithParametrizedGUI {
    private static final int FADE_MIN = 0;
    private static final int FADE_MAX = 100;
    private static final int FADE_INIT = 100;

    private final RangeParam opacityParam = new RangeParam("Opacity (%)", FADE_MIN, FADE_MAX,
            FADE_INIT);
//    private BlendingModeParam blendingModeParam = new BlendingModeParam(BlendingMode.values());


    public Fade() {
        super("Fade", true, false);
        setParamSet(new ParamSet(
                opacityParam
//                blendingModeParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (opacityParam.getValue() == 100) {
            return src;
        }

        FadeableEdit edit = History.getPreviousEditForFade(ImageComponents.getActiveComp());

        if (edit == null) { // the fade menu item is active only if History.canFade()
            throw new IllegalStateException();
        }

        BufferedImage previous = edit.getBackupImage();
        if(previous == null) {
            // soft reference expired
            return src;
        }

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);
        int[] prevData = ImageUtils.getPixelsAsArray(previous);

        int length = srcData.length;

        if (length != prevData.length) {
            Composition activeComp = ImageComponents.getActiveComp();
            Composition previousComp = edit.getComp();
            if (activeComp != previousComp) {
                throw new IllegalArgumentException("activeComp != previousComp");
            }

            int width = src.getWidth();
            int height = src.getHeight();
            int previousWidth = previous.getWidth();
            int previousHeight = previous.getHeight();

            String debugInfo = "Fade.transform width = " + width + ", height = " + height + ", previousWidth = " + previousWidth + ", previousHeight = " + previousHeight;
            debugInfo += (" comp = " + activeComp.getName());
            throw new IllegalArgumentException("the image and the previous are not the same size: " + debugInfo);
        }

        float fadeFactor = opacityParam.getValueAsPercentage();

        for (int i = 0; i < length; i++) {
            int rgb = srcData[i];
            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            int prevRGB = prevData[i];
            int prevA = (prevRGB >>> 24) & 0xFF;
            int prevR = (prevRGB >>> 16) & 0xFF;
            int prevG = (prevRGB >>> 8) & 0xFF;
            int prevB = prevRGB & 0xFF;

            a = (int) (prevA + fadeFactor * (a - prevA));
            r = (int) (prevR + fadeFactor * (r - prevR));
            g = (int) (prevG + fadeFactor * (g - prevG));
            b = (int) (prevB + fadeFactor * (b - prevB));

            destData[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return dest;
    }

    public void setOpacity(int newOpacity) {
        opacityParam.setValue(newOpacity);
    }
}
