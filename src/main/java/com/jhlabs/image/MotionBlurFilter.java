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

import net.jafama.DoubleWrapper;
import net.jafama.FastMath;
import pixelitor.ThreadPool;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.Future;

/**
 * A filter which produces motion blur the slow, but higher-quality way.
 */
public class MotionBlurFilter extends AbstractBufferedImageOp implements MotionBlur {
    private float angle = 0.0f;
    private float falloff = 1.0f;
    private float distance = 1.0f;
    private float zoom = 0.0f;
    private float rotation = 0.0f;
    private boolean wrapEdges = false;
    private boolean premultiplyAlpha = true;

    private float centreY = 0.5f;
    private float centreX = 0.5f;

    /**
     * Construct a MotionBlurFilter.
     */
    public MotionBlurFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set the centre of the effect in the X direction as a proportion of the image size.
     *
     * @param centreX the center
     * @see #getCentreX
     */
    @Override
    public void setCentreX(float centreX) {
        this.centreX = centreX;
    }

    /**
     * Get the centre of the effect in the X direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCentreX
     */
    @Override
    public float getCentreX() {
        return centreX;
    }

    /**
     * Set the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @param centreY the center
     * @see #getCentreY
     */
    @Override
    public void setCentreY(float centreY) {
        this.centreY = centreY;
    }

    /**
     * Get the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCentreY
     */
    @Override
    public float getCentreY() {
        return centreY;
    }

    /**
     * Set the centre of the effect as a proportion of the image size.
     *
     * @param centre the center
     * @see #getCentre
     */
    @Override
    public void setCentre(Point2D centre) {
        this.centreX = (float) centre.getX();
        this.centreY = (float) centre.getY();
    }

    /**
     * Get the centre of the effect as a proportion of the image size.
     *
     * @return the center
     * @see #setCentre
     */
    @Override
    public Point2D getCentre() {
        return new Point2D.Float(centreX, centreY);
    }

    /**
     * Specifies the angle of blur.
     *
     * @param angle the angle of blur.
     * @angle
     * @see #getAngle
     */
    @Override
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Returns the angle of blur.
     *
     * @return the angle of blur.
     * @see #setAngle
     */
    @Override
    public float getAngle() {
        return angle;
    }

    /**
     * Set the distance of blur.
     *
     * @param distance the distance of blur.
     * @see #getDistance
     */
    @Override
    public void setDistance(float distance) {
        this.distance = distance;
    }

    /**
     * Get the distance of blur.
     *
     * @return the distance of blur.
     * @see #setDistance
     */
    @Override
    public float getDistance() {
        return distance;
    }

    /**
     * Set the blur rotation.
     *
     * @param rotation the angle of rotation.
     * @see #getRotation
     */
    @Override
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * Get the blur rotation.
     *
     * @return the angle of rotation.
     * @see #setRotation
     */
    @Override
    public float getRotation() {
        return rotation;
    }

    /**
     * Set the blur zoom.
     *
     * @param zoom the zoom factor.
     * @see #getZoom
     */
    @Override
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Get the blur zoom.
     *
     * @return the zoom factor.
     * @see #setZoom
     */
    @Override
    public float getZoom() {
        return zoom;
    }

    /**
     * Set whether to wrap at the image edges.
     *
     * @param wrapEdges true if it should wrap.
     * @see #getWrapEdges
     */
    public void setWrapEdges(boolean wrapEdges) {
        this.wrapEdges = wrapEdges;
    }

    /**
     * Get whether to wrap at the image edges.
     *
     * @return true if it should wrap.
     * @see #setWrapEdges
     */
    public boolean getWrapEdges() {
        return wrapEdges;
    }

    /**
     * Set whether to premultiply the alpha channel.
     *
     * @param premultiplyAlpha true to premultiply the alpha
     * @see #getPremultiplyAlpha
     */
    public void setPremultiplyAlpha(boolean premultiplyAlpha) {
        this.premultiplyAlpha = premultiplyAlpha;
    }

    /**
     * Get whether to premultiply the alpha channel.
     *
     * @return true to premultiply the alpha
     * @see #setPremultiplyAlpha
     */
    public boolean getPremultiplyAlpha() {
        return premultiplyAlpha;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        pt = createProgressTracker(height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

//		float sinAngle = (float)Math.sin(angle);
//		float cosAngle = (float)Math.cos(angle);
//
//		float total;

//		int cx = width/2;
//		int cy = height/2;
        int cx = (int) (width * centreX);
        int cy = (int) (height * centreY);

//        int index = 0;

        float imageRadius = (float) Math.sqrt(cx * cx + cy * cy);
        float translateX = (float) (distance * Math.cos(angle));
        float translateY = (float) (distance * -Math.sin(angle));
        float maxDistance = distance + Math.abs(rotation * imageRadius) + zoom * imageRadius;
        int repetitions = (int) maxDistance;

        if (premultiplyAlpha) {
            ImageMath.premultiply(inPixels, 0, inPixels.length);
        }

        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable lineTask = () -> calcLine(width, height, inPixels, outPixels, cx, cy, translateX, translateY, repetitions, finalY);
            futures[y] = ThreadPool.submit(lineTask);
        }
        ThreadPool.waitForFutures(futures, pt);
        if (premultiplyAlpha) {
            ImageMath.unpremultiply(outPixels, 0, inPixels.length);
        }

        setRGB(dst, 0, 0, width, height, outPixels);

        finishProgressTracker();

        return dst;
    }

    private void calcLine(int width, int height, int[] inPixels, int[] outPixels, int cx, int cy, float translateX, float translateY, int repetitions, int y) {
        int index = y * width;
        FastTransform t = new FastTransform();
        Point2D.Float p = new Point2D.Float();

        for (int x = 0; x < width; x++) {
            int a = 0, r = 0, g = 0, b = 0;
            int count = 0;
            for (int i = 0; i < repetitions; i++) {
                int newX = x, newY = y;

                if (i != 0) {
                    float f = (float) i / repetitions;
                    p.x = x;
                    p.y = y;
                    t.setToIdentity();
                    t.translateAfterIdentity(cx + f * translateX, cy + f * translateY);
                    float s = 1 - zoom * f;
                    t.scaleAfterTranslate(s, s);
                    if (rotation != 0) {
                        t.rotate(-rotation * f);
                    }
                    t.translateAfterRotate(-cx, -cy);
                    t.transform(p, p);
                    newX = (int) p.x;
                    newY = (int) p.y;
                }
                if (newX < 0 || newX >= width) {
                    if (wrapEdges) {
                        newX = ImageMath.mod(newX, width);
                    } else {
                        break;
                    }
                }
                if (newY < 0 || newY >= height) {
                    if (wrapEdges) {
                        newY = ImageMath.mod(newY, height);
                    } else {
                        break;
                    }
                }

                count++;
                int rgb = inPixels[newY * width + newX];
                a += (rgb >> 24) & 0xff;
                r += (rgb >> 16) & 0xff;
                g += (rgb >> 8) & 0xff;
                b += rgb & 0xff;
            }
            if (count == 0) {
                outPixels[index] = inPixels[index];
            } else {
                a = PixelUtils.clamp(a / count);
                r = PixelUtils.clamp(r / count);
                g = PixelUtils.clamp(g / count);
                b = PixelUtils.clamp(b / count);
                outPixels[index] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            index++;
        }
    }

    public String toString() {
        return "Blur/Motion Blur...";
    }

    // an affine transform optimized for this specific filter
    static class FastTransform {
        private double m00, m10, m01, m11, m02, m12;
        private final DoubleWrapper doubleWrapper = new DoubleWrapper();

        public void setToIdentity() {
            m00 = m11 = 1.0;
            m10 = m01 = m02 = m12 = 0.0;
        }

        public void translateAfterIdentity(double tx, double ty) {
            m02 = tx;
            m12 = ty;
        }

        public void scaleAfterTranslate(double sx, double sy) {
            m00 = sx;
            m11 = sy;
        }

        public void rotate(double theta) {
            double sin = FastMath.sinAndCos(theta, doubleWrapper);
            double cos = doubleWrapper.value;
            double m0 = m00;
            double m1 = m01;
            m00 = cos * m0 + sin * m1;
            m01 = -sin * m0 + cos * m1;
            m0 = m10;
            m1 = m11;
            m10 = cos * m0 + sin * m1;
            m11 = -sin * m0 + cos * m1;
        }

        public void translateAfterRotate(double tx, double ty) {
            m02 = tx * m00 + ty * m01 + m02;
            m12 = tx * m10 + ty * m11 + m12;
        }

        public Point2D transform(Point2D ptSrc, Point2D ptDst) {
            double x = ptSrc.getX();
            double y = ptSrc.getY();
            ptDst.setLocation(x * m00 + y * m01 + m02,
                    x * m10 + y * m11 + m12);
            return ptDst;
        }
    }
}

