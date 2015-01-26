/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

public class Brightness extends FilterWithParametrizedGUI {
    private final RangeParam power = new RangeParam("Brightness Power (%)", 50, 150, 100);
    private final RangeParam multiply = new RangeParam("Brightness Multiply (%)", 1, 200, 100);
    private final RangeParam add = new RangeParam("Brightness Add", -255, 255, 0);
    private final RangeParam contrast = new RangeParam("Contrast", -255, 255, 0);

    public Brightness() {
        super("Brightness/Contrast", true, false);
        setParamSet(new ParamSet(
                power,
                multiply,
                add,
                contrast
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int addValue = add.getValue();
        int contrastValue = contrast.getValue();

        if ((addValue == 0) && (multiply.getValue() == 100) && (power.getValue() == 100) && contrastValue == 0) {
            return src;
        }

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        float multiplyValue = multiply.getValueAsPercentage();
        float powerValue = power.getValueAsPercentage();

        int[] lookup = new int[256];


        double contrastFactor = (259.0 * (contrastValue + 255)) / (255.0 * (259 - contrastValue));

        for (int i = 0; i < lookup.length; i++) {
            float lookupValue = i; // by default do nothing

            lookupValue = (float) Math.pow(lookupValue, powerValue);
            int lookupValueInt = ((int) (lookupValue * multiplyValue)) + addValue;

            // contrastValue
            lookupValueInt = (int)(contrastFactor * (lookupValueInt - 128) + 128);

            lookup[i] = PixelUtils.clamp(lookupValueInt);
        }

        for (int i = 0; i < destData.length; i++) {
            int rgb = srcData[i];

//            int a = (rgb >>> 24) & 0xFF;
            int a = rgb & 0xFF000000;
            int r = (rgb >>> 16) & 0xFF;
            int g = (rgb >>> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            if (a == 0) {
                destData[i] = 0; // for premultiplied images
            } else {
                r = lookup[r];
                g = lookup[g];
                b = lookup[b];

                // now the contrast
/*                if(contrastValue != 0)  {
                    r  = (int)(contrastFactor * (r - 128) + 128);
                    g  = (int)(contrastFactor * (g - 128) + 128);
                    b  = (int)(contrastFactor * (b - 128) + 128);
                    if(r > 255) {
                        r = 255;
                    }
                    if(g > 255) {
                        g = 255;
                    }
                    if(b > 255) {
                        b = 255;
                    }
                    if(r < 0) {
                        r = 0;
                    }
                    if(g < 0) {
                        g = 0;
                    }
                    if(b < 0) {
                        b = 0;
                    }

                }
    */

                destData[i] = a | (r << 16) | (g << 8) | b;
            }
        }

        return dest;
    }
}