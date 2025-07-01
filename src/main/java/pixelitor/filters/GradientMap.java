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

import com.jhlabs.image.Colormap;
import pixelitor.filters.gui.GradientParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * The "Gradient Map" filter.
 */
public class GradientMap extends ParametrizedFilter {
    public static final String NAME = "Gradient Map";

    @Serial
    private static final long serialVersionUID = 1258924395449510227L;

    private final GradientParam gradient =
        GradientParam.createBlackToWhite("Colors");

    public GradientMap() {
        super(true);

        initParams(gradient);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Colormap colormap = gradient.getColorMap();

        int[] gradientLookup = new int[256];
        for (int i = 0; i < gradientLookup.length; i++) {
            gradientLookup[i] = colormap.getColor(i / 255.0f);
        }

        int[] srcPixels = ImageUtils.getPixels(src);
        int[] destPixels = ImageUtils.getPixels(dest);

        for (int i = 0; i < destPixels.length; i++) {
            int rgb = srcPixels[i];

            int a = (rgb >>> 24) & 0xFF;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = rgb & 0xFF;

            int lum = (r + r + b + g + g + g) / 6;

            int gr = gradientLookup[lum];
            if (a == 0xFF) {
                destPixels[i] = gr;
            } else {
                int mask = a << 24 | 0xFF_FF_FF;
                destPixels[i] = mask & gr;
            }
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}