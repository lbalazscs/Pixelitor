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
import pixelitor.filters.jhlabsproxies.JHSwirlPinchBulge;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A filter which performs the popular whirl-and-pinch distortion effect.
 */
public class PinchFilter extends TransformFilter implements SwirlMethod {
    private float angle = 0;
    private float centerX = 0.5f;
    private float centerY = 0.5f;
    private float radius = 100;
    private float pinchBulgeAmount = 0.5f;

    private float radius2 = 0;
    private float icenterX;
    private float icenterY;

    private float zoom;
    private float rotateResultAngle;

    public PinchFilter() {
        super(JHSwirlPinchBulge.NAME);
    }

    /**
     * Set the angle of twirl in radians. 0 means no distortion.
     *
     * @param angle the angle of twirl. This is the angle by which pixels at the nearest edge of the image will move.
     */
    @Override
    public void setSwirlAmount(float angle) {
        this.angle = -angle;
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
    public void setCenter(Point2D center) {
        centerX = (float) center.getX();
        centerY = (float) center.getY();
    }

    /**
     * Set the radius of the effect.
     *
     * @param radius the radius
     * @min-value 0
     */
    @Override
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Set the amount of pinch.
     *
     * @param amount the amount
     * @min-value -1
     * @max-value 1
     */
    @Override
    public void setPinchBulgeAmount(float amount) {
        pinchBulgeAmount = -amount;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        float width = src.getWidth();
        float height = src.getHeight();
        icenterX = width * centerX;
        icenterY = height * centerY;
        if (radius == 0) {
            radius = Math.min(icenterX, icenterY);
        }
        radius2 = radius * radius;
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - icenterX;
        float dy = y - icenterY;
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

            out[0] = (u + icenterX);
            out[1] = (v + icenterY);
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

            out[0] = icenterX + u;
            out[1] = icenterY + v;
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

    @Override
    public String toString() {
        return "Distort/Pinch...";
    }

    @Override
    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
            new Ellipse2D.Float(icenterX - radius, icenterY - radius, 2 * radius, 2 * radius)
        };
    }
}
