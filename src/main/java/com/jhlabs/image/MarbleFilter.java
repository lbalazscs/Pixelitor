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

import java.awt.image.BufferedImage;

/**
 * This filter applies a marbling effect to an image, displacing pixels by random amounts.
 */
public class MarbleFilter extends TransformFilter {

    private float[] sinTable, cosTable;
    private float scale = 4;
    private float amount = 4;
    private float turbulence = 1;

    public MarbleFilter(String filterName) {
        super(filterName);
        setEdgeAction(REPEAT_EDGE_PIXELS);
    }

    /**
     * Set the X scale of the effect.
     *
     * @param scale the scale.
     * @see #getScale
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Get the X scale of the effect.
     *
     * @return the scale.
     * @see #setScale
     */
    public float getScale() {
        return scale;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public float getAmount() {
        return amount;
    }

    /**
     * Specifies the turbulence of the effect.
     *
     * @param turbulence the turbulence of the effect.
     * @min-value 0
     * @max-value 1
     * @see #getTurbulence
     */
    public void setTurbulence(float turbulence) {
        this.turbulence = turbulence;
    }

    /**
     * Returns the turbulence of the effect.
     *
     * @return the turbulence of the effect.
     * @see #setTurbulence
     */
    public float getTurbulence() {
        return turbulence;
    }

    private void initialize() {
        sinTable = new float[256];
        cosTable = new float[256];
        for (int i = 0; i < 256; i++) {
            float angle = ImageMath.TWO_PI * i / 256.0f * turbulence;

            sinTable[i] = (float) (-amount * Math.sin(angle));
            cosTable[i] = (float) (amount * Math.cos(angle));
        }

        // Laszlo: balance the arrays around 0
        // so that the pixels are not shifted predominantly in one direction
        float sinAverage = average(sinTable);
        float cosAverage = average(cosTable);
        for (int i = 0; i < 256; i++) {
            sinTable[i] -= sinAverage;
            cosTable[i] -= cosAverage;
        }
    }

    static float average(float[] input) {
        float sum = 0f;
        for (float v : input) {
            sum += v;
        }
        return sum / input.length;
    }

    private int displacementMap(int x, int y) {
        float noise = Noise.noise2(x / scale, y / scale); // mostly between -1 and 1 but not distributed uniformly
        return PixelUtils.clamp((int) (127 * (1 + noise)));
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        int displacement = displacementMap(x, y);

        out[0] = x + sinTable[displacement];
        out[1] = y + cosTable[displacement];
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst ) {
		initialize();
		return super.filter( src, dst );
	}

	public String toString() {
		return "Distort/Marble...";
	}
}
