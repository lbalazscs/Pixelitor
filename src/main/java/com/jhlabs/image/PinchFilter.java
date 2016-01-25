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

import net.jafama.FastMath;
import pixelitor.filters.jhlabsproxies.SwirlPinchBulge;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A filter which performs the popular whirl-and-pinch distortion effect.
 */
public class PinchFilter extends TransformFilter implements SwirlMethod {

    private float angle = 0;
    private float centreX = 0.5f;
    private float centreY = 0.5f;
    private float radius = 100;
    private float pinchBulgeAmount = 0.5f;

    private float radius2 = 0;
    private float icentreX;
    private float icentreY;

    private float zoom;
    private float rotateResultAngle;

    public PinchFilter() {
        super(SwirlPinchBulge.NAME);
    }

    /**
     * Set the angle of twirl in radians. 0 means no distortion.
     *
     * @param angle the angle of twirl. This is the angle by which pixels at the nearest edge of the image will move.
     * @see #getAngle
     */
    @Override
    public void setSwirlAmount(float angle) {
        this.angle = -angle;
    }

    /**
     * Get the angle of twist.
     *
     * @return the angle in radians.
     * @see #setAngle
     */
    public float getAngle() {
        return angle;
    }

    /**
     * Set the centre of the effect in the X direction as a proportion of the image size.
     *
     * @param centerX the center
     * @see #getCentreX
     */
    @Override
    public void setCenterX(float centerX) {
        this.centreX = centerX;
    }

    /**
     * Get the centre of the effect in the X direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCenterX
     */
    public float getCentreX() {
        return centreX;
    }

    /**
     * Set the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @param centerY the center
     * @see #getCentreY
     */
    @Override
    public void setCenterY(float centerY) {
        this.centreY = centerY;
    }

    /**
     * Get the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCenterY
     */
    public float getCentreY() {
        return centreY;
    }

    /**
     * Set the centre of the effect as a proportion of the image size.
     *
     * @param center the center
     * @see #getCentre
     */
    public void setCenter(Point2D center) {
        this.centreX = (float) center.getX();
        this.centreY = (float) center.getY();
    }

    /**
     * Get the centre of the effect as a proportion of the image size.
     *
     * @return the center
     * @see #setCenter
     */
    public Point2D getCentre() {
        return new Point2D.Float(centreX, centreY);
    }

    /**
     * Set the radius of the effect.
     *
     * @param radius the radius
     * @min-value 0
     * @see #getRadius
     */
    @Override
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Get the radius of the effect.
     *
     * @return the radius
     * @see #setRadius
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Set the amount of pinch.
     *
     * @param amount the amount
     * @min-value -1
     * @max-value 1
     * @see #getPinchBulgeAmount
     */
    @Override
    public void setPinchBulgeAmount(float amount) {
        this.pinchBulgeAmount = -amount;
    }

    /**
     * Get the amount of pinch.
     *
     * @return the amount
     * @see #setSwirlAmount
     */
    public float getPinchBulgeAmount() {
        return pinchBulgeAmount;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        float width = src.getWidth();
        float height = src.getHeight();
        icentreX = width * centreX;
        icentreY = height * centreY;
        if (radius == 0) {
            radius = Math.min(icentreX, icentreY);
        }
        radius2 = radius * radius;
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - icentreX;
        float dy = y - icentreY;
        float distance = dx * dx + dy * dy;

        if (distance > radius2 || distance == 0) {
//            out[0] = x;
//            out[1] = y;

            double angle = FastMath.atan2(dy, dx);
            angle += rotateResultAngle;
            double r = Math.sqrt(distance);
            double zoomedR = r / zoom;
            float u = (float) (zoomedR * FastMath.cos(angle));
            float v = (float) (zoomedR * FastMath.sin(angle));

            out[0] = (u + icentreX);
            out[1] = (v + icentreY);
        } else {
            float scaledDist = (float) Math.sqrt(distance / radius2);
            float pinchBulgeFactor = (float) FastMath.pow(FastMath.sin(Math.PI * 0.5 * scaledDist), -pinchBulgeAmount);

            // pinch-bulge
            dx *= pinchBulgeFactor;
            dy *= pinchBulgeFactor;

            // twirl
            float e = 1 - scaledDist;
            float a = angle * e * e;

            a += rotateResultAngle;

            float sin = (float) FastMath.sin(a);
            float cos = (float) FastMath.cos(a);

            float u = (cos * dx - sin * dy) / zoom;
            float v = (sin * dx + cos * dy) / zoom;

            out[0] = icentreX + u;
            out[1] = icentreY + v;
        }
    }

    @Override
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    @Override
    public void setRotateResultAngle(float rotateResultAngle) {
        this.rotateResultAngle = rotateResultAngle;
    }

    public String toString() {
        return "Distort/Pinch...";
    }

    @Override
    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
                new Ellipse2D.Float(icentreX - radius, icentreY - radius, 2 * radius, 2 * radius)
        };
    }
}
