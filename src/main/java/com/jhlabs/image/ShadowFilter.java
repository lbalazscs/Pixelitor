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
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * A filter which draws a drop shadow based on the alpha channel of the image.
 */
public class ShadowFilter extends AbstractBufferedImageOp {
    private float radius = 5;
    private float angle = (float) Math.PI * 6 / 4;
    private float distance = 5;
    private float opacity = 0.5f;
    private boolean addMargins = false;
    private boolean shadowOnly = false;
    private int shadowColor = 0xff000000;

    /**
     * Construct a ShadowFilter.
     */
    public ShadowFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the angle of the shadow.
     *
     * @param angle the angle of the shadow.
     * @angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Set the distance of the shadow.
     *
     * @param distance the distance.
     */
    public void setDistance(float distance) {
        this.distance = distance;
    }

    /**
     * Set the radius of the kernel, and hence the amount of blur. The bigger the radius, the longer this filter will take.
     *
     * @param radius the radius of the blur in pixels.
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Set the opacity of the shadow.
     *
     * @param opacity the opacity.
     */
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    /**
     * Set the color of the shadow.
     *
     * @param shadowColor the color.
     */
    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }

    /**
     * Set whether to increase the size of the output image to accomodate the shadow.
     *
     * @param addMargins true to add margins.
     */
    public void setAddMargins(boolean addMargins) {
        this.addMargins = addMargins;
    }

    /**
     * Set whether to only draw the shadow without the original image.
     *
     * @param shadowOnly true to only draw the shadow.
     */
    public void setShadowOnly(boolean shadowOnly) {
        this.shadowOnly = shadowOnly;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        Rectangle r = new Rectangle(0, 0, src.getWidth(), src.getHeight());
        if (addMargins) {
            float xOffset = distance * (float) Math.cos(angle);
            float yOffset = -distance * (float) Math.sin(angle);
            r.width += (int) (Math.abs(xOffset) + 2 * radius);
            r.height += (int) (Math.abs(yOffset) + 2 * radius);
        }
        return r;
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double();
        }

        if (addMargins) {
            float xOffset = distance * (float) Math.cos(angle);
            float yOffset = -distance * (float) Math.sin(angle);
            float topShadow = Math.max(0, radius - yOffset);
            float leftShadow = Math.max(0, radius - xOffset);
            dstPt.setLocation(srcPt.getX() + leftShadow, srcPt.getY() + topShadow);
        } else {
            dstPt.setLocation(srcPt.getX(), srcPt.getY());
        }

        return dstPt;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        float xOffset = distance * (float) Math.cos(angle);
        float yOffset = -distance * (float) Math.sin(angle);

        if (dst == null) {
            if (addMargins) {
                ColorModel cm = src.getColorModel();
                dst = new BufferedImage(cm, cm
                        .createCompatibleWritableRaster(src.getWidth() + (int) (Math.abs(xOffset) + radius), src
                                .getHeight() + (int) (Math.abs(yOffset) + radius)), cm.isAlphaPremultiplied(), null);
            } else {
                dst = createCompatibleDestImage(src, null);
            }
        }

        float shadowR = ((shadowColor >> 16) & 0xff) / 255.0f;
        float shadowG = ((shadowColor >> 8) & 0xff) / 255.0f;
        float shadowB = (shadowColor & 0xff) / 255.0f;

        // Make a black mask from the image's alpha channel
        float[][] extractAlpha = {
                {0, 0, 0, shadowR},
                {0, 0, 0, shadowG},
                {0, 0, 0, shadowB},
                {0, 0, 0, opacity}
        };
        BufferedImage shadow = new BufferedImage(width, height, TYPE_INT_ARGB);
        new BandCombineOp(extractAlpha, null).filter(src.getRaster(), shadow.getRaster());
//        shadow = new GaussianFilter(radius, filterName).filter(shadow, null);
        shadow = new BoxBlurFilter(radius, radius, 3, filterName).filter(shadow, null);

        Graphics2D g = dst.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        if (addMargins) {
//            float radius2 = radius / 2;
            float topShadow = Math.max(0, radius - yOffset);
            float leftShadow = Math.max(0, radius - xOffset);
            g.translate(leftShadow, topShadow);
        }
        g.drawRenderedImage(shadow, AffineTransform.getTranslateInstance(
                xOffset, yOffset));
        if (!shadowOnly) {
            g.setComposite(AlphaComposite.SrcOver);
            g.drawRenderedImage(src, null);
        }
        g.dispose();

        return dst;
    }

    @Override
    public String toString() {
        return "Stylize/Drop Shadow...";
    }
}
