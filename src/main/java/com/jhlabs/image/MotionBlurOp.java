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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

/**
 * A filter which produces motion blur the faster, but lower-quality way.
 */
public class MotionBlurOp extends AbstractBufferedImageOp implements MotionBlur {
    private float centerX = 0.5f, centerY = 0.5f;
    private float distance;
    private float angle;
    private float rotation;
    private float zoom;

    /**
     * Construct a MotionBlurOp.
     */
    public MotionBlurOp(String filterName) {
        super(filterName);
    }

    /**
     * Sets the angle of blur.
     *
     * @param angle the angle of blur.
     * @angle
     */
    @Override
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Set the distance of blur.
     *
     * @param distance the distance of blur.
     */
    @Override
    public void setDistance(float distance) {
        this.distance = distance;
    }

    /**
     * Set the blur rotation.
     *
     * @param rotation the angle of rotation.
     */
    @Override
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * Set the blur zoom.
     *
     * @param zoom the zoom factor.
     */
    @Override
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Set the center of the effect in the X direction as a proportion of the image size.
     *
     * @param centerX the center
     */
    @Override
    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    /**
     * Set the center of the effect in the Y direction as a proportion of the image size.
     *
     * @param centerY the center
     */
    @Override
    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    /**
     * Set the center of the effect as a proportion of the image size.
     *
     * @param center the center
     */
    @Override
    public void setCenter(Point2D center) {
        centerX = (float) center.getX();
        centerY = (float) center.getY();
    }

    private static int log2(int n) {
        int m = 1;
        int log2n = 0;

        while (m < n) {
            m *= 2;
            log2n++;
        }
        return log2n;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        BufferedImage tsrc = src;
        float cx = src.getWidth() * centerX;
        float cy = src.getHeight() * centerY;
        float imageRadius = (float) Math.sqrt(cx * cx + cy * cy);
        float translateX = (float) (distance * Math.cos(angle));
        float translateY = (float) (distance * -Math.sin(angle));
        float scale = zoom;
        float rotate = rotation;
        float maxDistance = distance + Math.abs(rotation * imageRadius) + zoom * imageRadius;
        int steps = log2((int) maxDistance);

        translateX /= maxDistance;
        translateY /= maxDistance;
        scale /= maxDistance;
        rotate /= maxDistance;

        if (steps == 0) {
            Graphics2D g = dst.createGraphics();
            g.drawRenderedImage(src, null);
            g.dispose();
            return dst;
        }

        pt = createProgressTracker(steps);

        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        BufferedImage tmp = createCompatibleDestImage(src, null);
        for (int i = 0; i < steps; i++) {
            Graphics2D g = tmp.createGraphics();
            g.drawImage(tsrc, null, null);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g.setComposite(alphaComposite);

            g.translate(cx + translateX, cy + translateY);
            g.scale(1.0001 + scale, 1.0001 + scale);  // The .0001 works round a bug on Windows where drawImage throws an ArrayIndexOutofBoundException
            if (rotation != 0) {
                g.rotate(rotate);
            }
            g.translate(-cx, -cy);

            g.drawImage(dst, null, null);
            g.dispose();
            BufferedImage ti = dst;
            dst = tmp;
            tmp = ti;
            tsrc = dst;

            translateX *= 2;
            translateY *= 2;
            scale *= 2;
            rotate *= 2;

            pt.unitDone();
        }

        finishProgressTracker();

        return dst;
    }

    @Override
    public String toString() {
        return "Blur/Faster Motion Blur...";
    }
}
