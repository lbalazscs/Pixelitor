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

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This filter diffuses an image by moving its pixels in random directions.
 */
public class DiffuseFilter extends TransformFilter {
    private float[] sinTable, cosTable;
    private float scale = 4;

    public DiffuseFilter(String filterName) {
        super(filterName);
        setEdgeAction(REPEAT_EDGE);
    }

    /**
     * Sets the scale of the texture.
     *
     * @param scale the scale of the texture.
     * @min-value 1
     * @max-value 100+
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        // int angle = (int) (Math.random() * 255);
        int angle = ThreadLocalRandom.current().nextInt(255); // faster than Math.random

        // float distance = (float) Math.random();
        float distance = ThreadLocalRandom.current().nextFloat(); // faster than Math.random

        out[0] = x + distance * sinTable[angle];
        out[1] = y + distance * cosTable[angle];
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        sinTable = new float[256];
        cosTable = new float[256];
        for (int i = 0; i < 256; i++) {
            float angle = ImageMath.TWO_PI * i / 256.0f;
            sinTable[i] = (float) (scale * Math.sin(angle));
            cosTable[i] = (float) (scale * Math.cos(angle));
        }
        return super.filter(src, dst);
    }

    @Override
    public String toString() {
        return "Distort/Diffuse...";
    }
}
