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

/**
 * A filter which produces a water ripple distortion.
 */
public class WaterFilter extends TransformFilter {
    private float wavelength = 16;
    private float amplitude = 10;
    private float phase = 0;
    private float radius = 50;

    private float radius2 = 0;
    private float cx;
    private float cy;

    public WaterFilter(String filterName) {
        super(filterName);
    }

    /**
     * Sets the wavelength of the ripples.
     *
     * @param wavelength the wavelength
     */
    public void setWavelength(float wavelength) {
        this.wavelength = wavelength;
    }

    /**
     * Sets the amplitude of the ripples.
     *
     * @param amplitude the amplitude
     */
    public void setAmplitude(float amplitude) {
        this.amplitude = amplitude;
    }

    /**
     * Sets the phase of the ripples.
     *
     * @param phase the phase
     */
    public void setPhase(float phase) {
        this.phase = phase;
    }

    /**
     * Sets the center of the effect in pixel coordinates.
     *
     * @param center the center
     */
    public void setCenter(Point2D center) {
        cx = (float) center.getX();
        cy = (float) center.getY();
    }

    /**
     * Sets the radius of the effect.
     *
     * @param radius the radius (must be >= 0)
     */
    public void setRadius(float radius) {
        this.radius = radius;
        radius2 = radius * radius;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float dx = x - cx;
        float dy = y - cy;
        float distance2 = dx * dx + dy * dy;
        if (distance2 > radius2) {
            out[0] = x;
            out[1] = y;
        } else {
            float distance = (float) Math.sqrt(distance2);
            float amount = amplitude * (float) FastMath.sin(distance / wavelength * ImageMath.TWO_PI - phase);
            amount *= (radius - distance) / radius;
            if (distance != 0) {
                amount *= wavelength / distance;
            }
            out[0] = x + dx * amount;
            out[1] = y + dy * amount;
        }
    }

    public Shape[] getAffectedAreaShapes() {
        return new Shape[]{
            new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius)
        };
    }
}
