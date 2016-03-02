/*
 * Copyright 2016 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jhlabs.image;

import com.jhlabs.math.Noise;
import net.jafama.FastMath;

/**
 * Constants for wave types
 */
public class WaveType {
    /**
     * Sine wave.
     */
    public final static int SINE = 0;
    /**
     * Sawtooth wave.
     */
    public final static int SAWTOOTH = 1;
    /**
     * Triangle wave.
     */
    public final static int TRIANGLE = 2;
    /**
     * Noise.
     */
    public final static int NOISE = 3;

    public static double wave(double in, int type) {
        switch (type) {
            case SINE:
                return FastMath.sin(in);
            case SAWTOOTH:
                return ImageMath.sinLikeSawtooth(in);
            case TRIANGLE:
                return ImageMath.sinLikeTriangle(in);
            case NOISE:
                return Noise.sinLikeNoise1((float) in);
            default:
                throw new IllegalStateException("type == " + type);
        }
    }
}
