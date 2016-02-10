/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import com.jhlabs.image.Colormap;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Gradient map
 */
public class GradientMap extends FilterWithParametrizedGUI {
    public static final String NAME = "Gradient Map";

    private final GradientParam gradientParam = new GradientParam("Colors", BLACK, WHITE);

    public GradientMap() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(gradientParam));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        Colormap colormap = gradientParam.getValue();

        int[] gradientLookup = new int[256];
        for (int i = 0; i < gradientLookup.length; i++) {
            gradientLookup[i] = colormap.getColor(i / 255.0f);
        }

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        for (int i = 0; i < destData.length; i++) {
            int rgb = srcData[i];

//            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            int average = (r + g + b) / 3;

            // TODO take transparency into account
            destData[i] = gradientLookup[average];

        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}