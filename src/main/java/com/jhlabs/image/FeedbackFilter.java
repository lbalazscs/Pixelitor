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
 * A filter which priduces a video feedback effect by repeated transformations.
 */
public class FeedbackFilter extends AbstractBufferedImageOp {
    private float centerX = 0.5f, centerY = 0.5f;
    private float distance;
    private float angle;
    private float rotation;
    private float zoom;
    private float startAlpha = 1;
    private float endAlpha = 1;
    private int iterations;

    /**
     * Construct a FeedbackFilter.
     */
    public FeedbackFilter(String filterName) {
        super(filterName);
    }

    /**
     * Construct a FeedbackFilter.
     *
     * @param distance the distance to move on each iteration
     * @param angle    the angle to move on each iteration
     * @param rotation the amount to rotate on each iteration
     * @param zoom     the amount to scale on each iteration
     */
    public FeedbackFilter(float distance, float angle, float rotation, float zoom, String filterName) {
        super(filterName);

        this.distance = distance;
        this.angle = angle;
        this.rotation = rotation;
        this.zoom = zoom;
    }

    /**
     * Sets the angle of each iteration.
     *
     * @param angle the angle of each iteration.
     * @angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Sets the distance to move on each iteration.
     *
     * @param distance the distance
     */
    public void setDistance(float distance) {
        this.distance = distance;
    }

    /**
     * Sets the amount of rotation on each iteration.
     *
     * @param rotation the angle of rotation
     * @angle
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * Sets the amount to scale on each iteration.
     *
     * @param zoom the zoom factor
     */
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Sets the alpha value at the first iteration.
     *
     * @param startAlpha the alpha value
     * @min-value 0
     * @max-value 1
     */
    public void setStartAlpha(float startAlpha) {
        this.startAlpha = startAlpha;
    }

    /**
     * Sets the alpha value at the last iteration.
     *
     * @param endAlpha the alpha value
     * @min-value 0
     * @max-value 1
     */
    public void setEndAlpha(float endAlpha) {
        this.endAlpha = endAlpha;
    }

    /**
     * Sets the center of the effect in the X direction as a proportion of the image size.
     *
     * @param centerX the center
     */
    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    /**
     * Sets the center of the effect in the Y direction as a proportion of the image size.
     *
     * @param centerY the center
     */
    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    /**
     * Sets the center of the effect as a proportion of the image size.
     *
     * @param center the center
     */
    public void setCenter(Point2D center) {
        centerX = (float) center.getX();
        centerY = (float) center.getY();
    }

    /**
     * Sets the number of iterations.
     *
     * @param iterations the number of iterations
     * @min-value 0
     */
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }
        float cx = src.getWidth() * centerX;
        float cy = src.getHeight() * centerY;
//        float imageRadius = (float)Math.sqrt( cx*cx + cy*cy );
        float translateX = (float) (distance * Math.cos(angle));
        float translateY = (float) (distance * -Math.sin(angle));
        float scale = (float) Math.exp(zoom);
        float rotate = rotation;

        if (iterations == 0) {
            Graphics2D g = dst.createGraphics();
            g.drawRenderedImage(src, null);
            g.dispose();
            return dst;
        }

        Graphics2D g = dst.createGraphics();
        g.drawImage(src, null, null);

        pt = createProgressTracker(iterations);

        for (int i = 0; i < iterations; i++) {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ImageMath
                    .lerp((float) i / (iterations - 1), startAlpha, endAlpha)));

            g.translate(cx + translateX, cy + translateY);
            g.scale(scale, scale);  // The .0001 works round a bug on Windows where drawImage throws an ArrayIndexOutofBoundException
            if (rotation != 0) {
                g.rotate(rotate);
            }
            g.translate(-cx, -cy);

            g.drawImage(src, null, null);
            pt.unitDone();
        }
        finishProgressTracker();

        g.dispose();
        return dst;
    }

    @Override
    public String toString() {
        return "Effects/Feedback...";
    }
}
