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

import com.jhlabs.math.Noise;

/**
 * A filter which distorts an image as if it were underwater.
 */
public class SwimFilter extends TransformFilter {
    private float scale = 32;
    private float stretch = 1.0f;
    private float angle = 0.0f;
    private float amount = 1.0f;
    //	private float turbulence = 1.0f; // Laszlo: commented out, because it did not seem to be useful
    private float time = 0.0f;
    private float m00 = 1.0f;
    private float m01 = 0.0f;
    private float m10 = 0.0f;
    private float m11 = 1.0f;

    public SwimFilter(String filterName) {
        super(filterName);
    }

    /**
     * Set the amount of swim.
     *
     * @param amount the amount of swim
     * @min-value 0
     * @max-value 100+
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    /**
     * Sets the scale of the distortion.
     *
     * @param scale the scale of the distortion.
     * @min-value 1
     * @max-value 300+
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Sets the stretch factor of the distortion.
     *
     * @param stretch the stretch factor of the distortion.
     * @min-value 1
     * @max-value 50+
     */
    public void setStretch(float stretch) {
        this.stretch = stretch;
    }

    /**
     * Sets the angle of the effect.
     *
     * @param angle the angle of the effect.
     * @angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        m00 = cos;
        m01 = sin;
        m10 = -sin;
        m11 = cos;
    }

    /**
     * Sets the time. Use this to animate the effect.
     *
     * @param time the time.
     * @angle
     */
    public void setTime(float time) {
        this.time = time;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        float nx = m00 * x + m01 * y;
        float ny = m10 * x + m11 * y;
        nx /= scale;
        ny /= scale * stretch;

        float noise3x = Noise.noise3(nx + 0.5f, ny, time);
        float noise3y = Noise.noise3(nx, ny + 0.5f, time);

        out[0] = x + amount * noise3x;
        out[1] = y + amount * noise3y;
    }

    @Override
    public String toString() {
        return "Distort/Swim...";
    }
}
