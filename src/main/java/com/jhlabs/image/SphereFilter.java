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

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * A filter which simulates a lens placed over an image.
 */
public class SphereFilter extends TransformFilter {
    private float a = 0;
    private float b = 0;
    private float a2 = 0;
    private float b2 = 0;
    private float centreX = 0.5f;
    private float centreY = 0.5f;
    private float refractionIndex = 1.5f;

    private float icentreX;
    private float icentreY;

    public SphereFilter(String filterName) {
        super(filterName);

        setEdgeAction(REPEAT_EDGE);
        setRadius(100.0f);
    }

    /**
     * Set the index of refraction.
     *
     * @param refractionIndex the index of refraction
     * @see #getRefractionIndex
     */
    public void setRefractionIndex(float refractionIndex) {
        this.refractionIndex = refractionIndex;
    }

    /**
     * Get the index of refaction.
     *
     * @return the index of refaction
     * @see #setRefractionIndex
     */
    public float getRefractionIndex() {
        return refractionIndex;
    }

    /**
     * Set the radius of the effect.
     *
     * @param r the radius
     * @min-value 0
     * @see #getRadius
     */
    public void setRadius(float r) {
        a = r;
        b = r;
    }

    /**
     * Get the radius of the effect.
     *
     * @return the radius
     * @see #setRadius
     */
    public float getRadius() {
        return a;
    }

    /**
     * Set the centre of the effect in the X direction as a proportion of the image size.
     *
     * @param centreX the center
     * @see #getCentreX
     */
    public void setCentreX(float centreX) {
        this.centreX = centreX;
    }

    /**
     * Get the centre of the effect in the X direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCentreX
     */
    public float getCentreX() {
        return centreX;
    }

    /**
     * Set the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @param centreY the center
     * @see #getCentreY
     */
    public void setCentreY(float centreY) {
        this.centreY = centreY;
    }

    /**
     * Get the centre of the effect in the Y direction as a proportion of the image size.
     *
     * @return the center
     * @see #setCentreY
     */
    public float getCentreY() {
        return centreY;
    }

    /**
     * Set the centre of the effect as a proportion of the image size.
     *
     * @param centre the center
     * @see #getCentre
     */
    public void setCentre(Point2D centre) {
        centreX = (float) centre.getX();
        centreY = (float) centre.getY();
    }

    /**
     * Get the centre of the effect as a proportion of the image size.
     *
     * @return the center
     * @see #setCentre
     */
    public Point2D getCentre() {
        return new Point2D.Float(centreX, centreY);
    }

    public void setA(float a) {
        this.a = a;
    }

    public void setB(float b) {
        this.b = b;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        float width = src.getWidth();
        float height = src.getHeight();
        icentreX = width * centreX;
        icentreY = height * centreY;
        if (a == 0) {
            a = width / 2;
        }
        if (b == 0) {
            b = height / 2;
        }
        a2 = a * a;
        b2 = b * b;
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - icentreX;
        float dy = y - icentreY;
        float x2 = dx * dx;
        float y2 = dy * dy;
        if (y2 >= (b2 - (b2 * x2) / a2)) {
            out[0] = x;
            out[1] = y;
        } else {
            float rRefraction = 1.0f / refractionIndex;

            float z = (float) Math.sqrt((1.0f - x2 / a2 - y2 / b2) * (a * b));
            float z2 = z * z;

            float xAngle = (float) FastMath.acos(dx / Math.sqrt(x2 + z2));
            float angle1 = ImageMath.HALF_PI - xAngle;
            float angle2 = (float) FastMath.asin(FastMath.sin(angle1) * rRefraction);
            angle2 = ImageMath.HALF_PI - xAngle - angle2;
            out[0] = x - (float) FastMath.tan(angle2) * z;

            float yAngle = (float) FastMath.acos(dy / Math.sqrt(y2 + z2));
            angle1 = ImageMath.HALF_PI - yAngle;
            angle2 = (float) FastMath.asin(FastMath.sin(angle1) * rRefraction);
            angle2 = ImageMath.HALF_PI - yAngle - angle2;
            out[1] = y - (float) FastMath.tan(angle2) * z;
        }
    }

    @Override
    public String toString() {
        return "Distort/Sphere...";
    }

    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
                new Ellipse2D.Float(icentreX - a, icentreY - b, 2 * a, 2 * b)
        };
    }
}
