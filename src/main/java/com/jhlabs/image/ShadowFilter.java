/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter which draws a drop shadow based on the alpha channel of the image.
 */
public class ShadowFilter extends AbstractBufferedImageOp {
    private final float radius;
    private final float angle;
    private final float distance;
    private final float opacity;
    private final boolean shadowOnly;
    private final int shadowColor;

    /**
     * Constructs a ShadowFilter.
     *
     * @param filterName  the name of the filter
     * @param angle       the angle of the shadow.
     * @param distance    the distance of the shadow.
     * @param opacity     the opacity of the shadow.
     * @param radius      the radius of the kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     * @param shadowColor the color of the shadow.
     * @param shadowOnly  true to only draw the shadow without the original image.
     */
    public ShadowFilter(String filterName,
                        float angle,
                        float distance,
                        float opacity,
                        float radius,
                        int shadowColor,
                        boolean shadowOnly) {
        super(filterName);

        this.angle = angle;
        this.distance = distance;
        this.opacity = opacity;
        this.radius = radius;
        this.shadowColor = shadowColor;
        this.shadowOnly = shadowOnly;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        float xOffset = distance * (float) Math.cos(angle);
        float yOffset = -distance * (float) Math.sin(angle);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        float shadowR = ((shadowColor >> 16) & 0xFF) / 255.0f;
        float shadowG = ((shadowColor >> 8) & 0xFF) / 255.0f;
        float shadowB = (shadowColor & 0xFF) / 255.0f;

        // make a black mask from the image's alpha channel
        float[][] extractAlpha = {
            {0, 0, 0, shadowR},
            {0, 0, 0, shadowG},
            {0, 0, 0, shadowB},
            {0, 0, 0, opacity}
        };
        BufferedImage shadow = new BufferedImage(width, height, TYPE_INT_ARGB);
        new BandCombineOp(extractAlpha, null).filter(src.getRaster(), shadow.getRaster());
        shadow = new BoxBlurFilter(filterName, radius, radius, 3).filter(shadow, null);

        Graphics2D g = dst.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g.drawRenderedImage(shadow,
            AffineTransform.getTranslateInstance(xOffset, yOffset));
        if (!shadowOnly) {
            g.setComposite(AlphaComposite.SrcOver);
            g.drawRenderedImage(src, null);
        }
        g.dispose();

        return dst;
    }
}
