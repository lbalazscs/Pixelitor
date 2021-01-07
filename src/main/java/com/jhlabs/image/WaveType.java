/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
    public static final int SINE = 0;
    /**
     * Sawtooth wave.
     */
    public static final int SAWTOOTH = 1;
    /**
     * Triangle wave.
     */
    public static final int TRIANGLE = 2;
    /**
     * Noise.
     */
    public static final int NOISE = 3;

    private WaveType() {
    }

    /**
     * Wave function with values between -1 and 1
     */
    public static double wave(double in, int type) {
        return switch (type) {
            case SINE -> FastMath.sin(in);
            case SAWTOOTH -> ImageMath.sinLikeSawtooth(in);
            case TRIANGLE -> ImageMath.sinLikeTriangle(in);
            case NOISE -> Noise.sinLikeNoise1((float) in);
            default -> throw new IllegalStateException("type == " + type);
        };
    }

    /**
     * Wave function with values between 0 and 1
     */
    public static double wave01(double in, int type) {
        return switch (type) {
            case SINE -> (1 + FastMath.sin(in)) / 2;
            case SAWTOOTH -> ImageMath.mod(in / (2 * Math.PI), 1);
            case TRIANGLE -> ImageMath.triangle(in / (2 * Math.PI));
            case NOISE -> Noise.noise1((float) in / ImageMath.PI);
            default -> throw new IllegalStateException("type == " + type);
        };
    }
}
