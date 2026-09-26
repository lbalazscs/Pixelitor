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

import static java.awt.RenderingHints.*;

/**
 * A filter which produces a video feedback effect by repeated transformations.
 */
public class FeedbackFilter extends AbstractBufferedImageOp {
    private final double centerX;
    private final double centerY;
    private final double distance;
    private final double angle;
    private final double rotation;
    private final double zoom;
    private final float startAlpha;
    private final float endAlpha;
    private final int iterations;

    /**
     * Constructs a FeedbackFilter.
     *
     * @param filterName the display name of the filter
     * @param iterations the number of iterations
     * @param center     the center of the effect as a proportion of the image size
     * @param distance   the distance to move on each iteration
     * @param angle      the angle to move on each iteration, in radians
     * @param rotation   the amount to rotate on each iteration, in radians
     * @param zoom       the exponent applied on each iteration; the per-iteration
     *                   scale factor is exp(zoom), so 0 means no change in size
     * @param startAlpha the alpha value at the first iteration (in the range [0, 1])
     * @param endAlpha   the alpha value at the last iteration (in the range [0, 1])
     */
    public FeedbackFilter(String filterName, int iterations, Point2D center,
                          double distance, double angle, double rotation,
                          double zoom, float startAlpha, float endAlpha) {
        super(filterName);

        this.centerX = center.getX();
        this.centerY = center.getY();
        this.distance = distance;
        this.angle = angle;
        this.rotation = rotation;
        this.zoom = zoom;
        this.startAlpha = startAlpha;
        this.endAlpha = endAlpha;
        this.iterations = iterations;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        Graphics2D g = dst.createGraphics();
        g.drawImage(src, null, null);

        if (iterations == 0) {
            g.dispose();
            return dst;
        }

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        double cx = src.getWidth() * centerX;
        double cy = src.getHeight() * centerY;
        double stepX = distance * Math.cos(angle);
        double stepY = distance * -Math.sin(angle);
        double scale = Math.exp(zoom);

        pt = createProgressTracker(iterations);

        int lastIndex = Math.max(iterations - 1, 1);
        for (int i = 0; i < iterations; i++) {
            // deliberately not resetting g's transform between iterations:
            // each pass builds on the accumulated transform from all
            // previous passes, which is what produces the "feedback" look
            float t = (float) i / lastIndex;
            float alpha = ImageMath.lerp(t, startAlpha, endAlpha);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g.translate(cx + stepX, cy + stepY);
            g.scale(scale, scale);
            if (rotation != 0) {
                g.rotate(rotation);
            }
            g.translate(-cx, -cy);

            g.drawImage(src, null, null);
            pt.unitDone();
        }
        finishProgressTracker();

        g.dispose();
        return dst;
    }
}
