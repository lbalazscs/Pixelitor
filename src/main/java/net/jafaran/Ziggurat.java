/*
 * Copyright 2014 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jafaran;

import java.util.Random;

/**
 * Provides normal gaussian numbers using a Ziggurat algorithm, and a specified
 * Random implementation for uniform randomness.
 * 
 * For a same uniform randomness, a same version of these treatments always
 * return a same sequence of normal gaussian numbers, as if by using StrictMath
 * and strictfp.
 */
public class Ziggurat {
    
    /*
     * Algorithm derived from
     * "An Improved Ziggurat Method to Generate Normal Random Samples",
     * J. A. Doornik, 2005.
     * 
     * We use further improvements, regarding both precision
     * and speed (regardless of whether we use 128 or 256 rectangles):
     * - Instead of computing a random double, and then a random index,
     *   - for accurate generation, we just compute a random long,
     *     and then use 54 bits of it to compute a double
     *     in [-1,1[, and 8 other bits of it for the index.
     *   - for fast generation, we just compute a random int,
     *     and then use all of its bits to compute a double
     *     in [-1,1[, and the 8 LSBits of it for the index.
     * - We do the first test with integer arithmetic,
     *   avoiding the use of Math.abs(double), and using
     *   NumbersUtils.absNeg for int bits, to handle
     *   Integer.MIN_VALUE. With server VM it looks around
     *   ten percents faster.
     * - The efficiency of the rare cases actually matters, because they are
     *   not that rare and are much slower. To speed them up, we cache
     *   some computations in an additional table.
     * 
     * 256 rectangles are used, even though the paper
     * describes a methods for 128 rectangles.
     * Also, instead of 2^32, we often use 2^31,
     * int being signed.
     * 
     * Using StrictMath to ensure that a same uniform randomness always yields
     * a same gaussian pseudo-randomness (no need for strictfp, since we don't
     * have underflows nor overflows).
     * 
     * NB:
     * For fast generation, "The Ziggurat Method for Generating Random
     * Variables", G. Marsaglia and W. W. Tsang, 2000, could also be used,
     * but it turns out to be a bit slower than the fast generation
     * derived from Doornik's work.
     * Also, there is a "bug" in this paper, in that the test
     * before the most common return, involves taking the
     * absolute value of a signed integer, which does not
     * exist if it's 0x80...0. This could rarely occur in
     * paper's code, because it was a 64 bits long, but here
     * we would use an int so the bias would be more obvious.
     * A workaround is to negate the test ("-abs(bits) >= -KI[index]"),
     * but it makes things slower if you don't have a negative-abs method.
     */

    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------

    /**
     * Add to nextDouble() to have values in ]0,1]
     */
    private static final double ANTI_ZERO_EPS = 1.0/(1L<<53);
    
    /*
     * Ziggurat constants for 256 rectangles.
     */

    /**
     * Number of rectangles (bottom reclangle included).
     */
    private static final int N = 256;

    /**
     * X where the tail starts.
     */
    static final double R_256 = 3.6541528853610088;
    
    /**
     * Volume of each non-bottom rectangle (N-1 of them),
     * and volume of the bottom rectangle (x <= R, y <= f(R))
     * plus the tail (x > R, y <= f(x)).
     */
    private static final double V_256 = 0.00492867323399;

    /*
     * Doornik's tables.
     * Bottom reclangle has index 0 (but X_255 = R, which is confusing!).
     */

    private static final double[] S_AD_ZIG_X = new double[N+1];
    private static final double[] S_AD_ZIG_R = new double[N];
    static {
        double f = f(R_256);
        S_AD_ZIG_X[0] = V_256 / f;
        S_AD_ZIG_X[1] = R_256;
        for (int i=2;i<N;i++) {
            final double xi = StrictMath.sqrt(-2.0 * StrictMath.log(V_256 / S_AD_ZIG_X[i-1] + f));
            S_AD_ZIG_X[i] = xi;
            f = f(xi);
        }
        S_AD_ZIG_X[N] = 0.0;
        
        for (int i=0;i<N;i++) {
            S_AD_ZIG_R[i] = S_AD_ZIG_X[i+1] / S_AD_ZIG_X[i];
        }
    }

    /*
     * For faster nextGaussian(Random).
     */
    
    private static final double[] S_AD_ZIG_X_NG = new double[N]; // N enough
    private static final long[] S_AD_ZIG_R_NG = new long[N];
    static {
        for (int i=0;i<N;i++) {
            // abs(u) < threshold
            // abs(bits * (1.0/(1L<<53))) < threshold
            // abs(bits) < threshold * (1L<<53)
            // abs(bits) < ceil(threshold * (1L<<53))
            S_AD_ZIG_R_NG[i] = (long)Math.ceil(S_AD_ZIG_R[i] * (1L<<53));
            S_AD_ZIG_X_NG[i] = S_AD_ZIG_X[i] * (1.0/(1L<<53));
        }
    }

    /*
     * For faster nextGaussianFast(Random).
     */
    
    private static final double[] S_AD_ZIG_X_NGF = new double[N]; // N is enough.
    private static final int[] S_AD_ZIG_R_NGF = new int[N];
    static {
        for (int i=0;i<N;i++) {
            // abs(u) < threshold
            // abs(bits * (1.0/(1L<<31))) < threshold
            // abs(bits) < threshold * (1L<<31)
            // abs(bits) < ceil(threshold * (1L<<31))
            // -abs(bits) >= -ceil(threshold * (1L<<31))
            // -abs(bits) >= floor(-threshold * (1L<<31))
            S_AD_ZIG_R_NGF[i] = (int)Math.floor(-S_AD_ZIG_R[i] * (1L<<31));
            S_AD_ZIG_X_NGF[i] = S_AD_ZIG_X[i] * (1.0/(1L<<31));
        }
    }
    
    /*
     * For faster rare cases.
     */

    private static final double[] F_S_AD_ZIG_X = new double[S_AD_ZIG_X.length];
    static {
        for (int i=0;i<F_S_AD_ZIG_X.length;i++) {
            F_S_AD_ZIG_X[i] = f(S_AD_ZIG_X[i]);
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param random The uniform randomness generator to use.
     * @return A normal gaussian number.
     */
    public static double nextGaussian(Random random) {
        do {
            if (false) {
                // Closer to Doornik's paper.
                if (false) {
                    // Paper's version, slower and less random.
                    final double d01 = nextDouble(random);
                    // u in ]-1,1]
                    final double u = (d01+d01) - 1.0;
                    final int index = random.nextInt() & 0xFF;
                }
                final long bits = random.nextLong();
                // u in [-1,1[, using 54 MSBits, i.e. with 2^-53 granularity.
                final double u = (bits>>(64-54)) * (1.0/(1L<<53));
                final int index = ((int)bits) & 0xFF;

                if (Math.abs(u) < S_AD_ZIG_R[index]) { 
                    return u * S_AD_ZIG_X[index];
                }
                
                // Using 9th LSBit to decide which side to go
                // (has not been used yet).
                final double x = rareCase(random, index, u, (bits<<(64-9)) < 0);
                if (x == x) {
                    return x;
                }
            } else {
                final long bits = random.nextLong();
                // Using 54 MSBits (and 1st MSBit as sign bit).
                final long uLong = (bits>>(64-54));
                // Using 8 LSBits.
                final int index = ((int)bits) & 0xFF;
                
                if (RandomUtilz.abs(uLong) < S_AD_ZIG_R_NG[index]) { 
                    return uLong * S_AD_ZIG_X_NG[index];
                }
                
                // u in [-1,1[, using 54 MSBits, i.e. with 2^-53 granularity.
                final double u = uLong * (1.0/(1L<<53));

                // Using 9th LSBit to decide which side to go
                // (has not been used yet).
                final double x = rareCase(random, index, u, (bits<<(64-9)) < 0);
                if (x == x) {
                    return x;
                }
            }
        } while (true);
    }

    /**
     * @param random The uniform randomness generator to use.
     * @return A normal gaussian number, possibly of lower quality or precision
     *         than nextGaussian(Random) method.
     */
    public static double nextGaussianFast(Random random) {
        do {
            if (false) {
                // Closer to Doornik's paper.
                final int bits = random.nextInt();
                // u in [-1,1[, using 32 bits, i.e. with 2^-31 granularity.
                final double u = bits * (1.0/(1L<<31));
                // Cheap index.
                final int index = (bits & 0xFF);

                if (Math.abs(u) < S_AD_ZIG_R[index]) {
                    return u * S_AD_ZIG_X[index];
                }

                // Using MSBit to decide which side to go
                // (has been used for u but had no impact yet).
                final double x = rareCase(random, index, u, bits < 0);
                if (x == x) {
                    return x;
                }
            } else {
                final int bits = random.nextInt();
                // Cheap index.
                final int index = (bits & 0xFF);

                if (RandomUtilz.absNeg(bits) >= S_AD_ZIG_R_NGF[index]) {
                    return bits * S_AD_ZIG_X_NGF[index];
                }

                // u in [-1,1[, using 32 bits, i.e. with 2^-31 granularity.
                final double u = bits * (1.0/(1L<<31));

                // Using MSBit to decide which side to go
                // (has been used for u but had no impact yet).
                final double x = rareCase(random, index, u, bits < 0);
                if (x == x) {
                    return x;
                }
            }
        } while (true);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private Ziggurat() {
    }
    
    /**
     * Defining our own nextDouble():
     * - not to have to trust specified implementation's one,
     * - to make sure we don't use subnormal values,
     * - it should cause one less megamorphic call
     *   (to Random.nextDouble()), so be faster.
     */
    private static double nextDouble(Random random) {
        return (random.nextLong() & ((1L<<53)-1)) * (1.0/(1L<<53));
    }
    
    private static double f(double x) {
        return StrictMath.exp(-0.5 * (x*x));
    }

    /**
     * u and negSide are used exclusively, so it doesn't hurt
     * randomness if same random bits were used to compute both.
     * 
     * @return Value to return, or NaN if shall retry.
     */
    private static double rareCase(
            Random random,
            int index,
            double u,
            boolean negSide) {
        if (index == 0) {
            return bottomCase(random, negSide);
        }
        final double x = u * S_AD_ZIG_X[index];
        final double fI = F_S_AD_ZIG_X[index];
        final double fIP1 = F_S_AD_ZIG_X[index+1];
        if (fIP1 + (fI - fIP1) * nextDouble(random) < f(x)) {
            return x;
        }
        return Double.NaN;
    }
    
    private static double bottomCase(Random random, boolean negSide) {
        /*
         * xx is in [log(2^-53)*(1.0/R_256),0]
         *    (i.e. [-10.053438299434404,0])
         * so result max absolute value is
         * R_256 - log(2^-53) * (1.0/R_256)
         * (i.e. 13.707591184795413)
         */
        double xx, yy;
        do {
            // We take care for log argument not being 0,
            // else it would result into -Infinity, and if
            // xx is -Infinity we return +-Infinity (unless
            // yy is -Infinity as well, for we use < and not <=),
            // which is bad.
            //
            // log(2^-53) = -36.7368005696771, so no risk of overflow
            // if we add 2^-53 to nextDouble()'s result.
            xx = StrictMath.log(nextDouble(random)+ANTI_ZERO_EPS) * (1.0/R_256);
            yy = StrictMath.log(nextDouble(random)+ANTI_ZERO_EPS);
        } while (-(yy + yy) < xx * xx);
        return negSide ? xx - R_256 : R_256 - xx;
    }
}
