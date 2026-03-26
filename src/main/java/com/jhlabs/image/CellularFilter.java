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
import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.utils.CachedFloatRandom;

import java.util.concurrent.Future;

/**
 * A variant of Worley noise that calculates distances to the three
 * nearest points and uses a weighted combination of these distances.
 * See https://en.wikipedia.org/wiki/Worley_noise
 */
public class CellularFilter extends WholeImageFilter {
    private static final int POISSON_ARRAY_SIZE = 8192;

    private GridType gridType;

    protected float scale = 32;
    protected float stretch = 1.0f;
    public float amount = 1.0f;
    public float turbulence = 1.0f;
    protected Colormap colormap;
    private final float[] coefficients = {1, 0, 0, 0};

    protected float m00 = 1.0f;
    protected float m01 = 0.0f;
    protected float m10 = 0.0f;
    protected float m11 = 1.0f;

    protected static final ThreadLocal<Point[]> resultsTL = ThreadLocal.withInitial(() -> {
        Point[] results = new Point[3];
        for (int j = 0; j < results.length; j++) {
            results[j] = new Point();
        }
        return results;
    });

    protected float randomness = 0;

    private static final byte[] poisson = initPoisson();

    public CellularFilter(String filterName) {
        super(filterName);
    }

    /**
     * Creates a discretized approximation of the Poisson probability
     * distribution. This array maps a uniformly distributed random
     * index to a specific outcome, representing the number of events,
     * enabling fast random sampling.
     */
    private static byte[] initPoisson() {
        byte[] arr = new byte[POISSON_ARRAY_SIZE];
        float total = 0; // the cumulative sum of probabilities
        float mean = 2.5f; // the λ parameter of the Poisson distribution
        float expMean = (float) Math.exp(-mean);
        float meanPow = 1.0f;
        float factorial = 1.0f;

        for (int k = 0; k < 10; k++) {
            if (k > 0) {
                meanPow *= mean;
                factorial *= k;
            }

            // the probability of an event occurring k times
            float probability = meanPow * expMean / factorial;

            int start = (int) (total * POISSON_ARRAY_SIZE);
            total += probability;
            int end = (int) (total * POISSON_ARRAY_SIZE);

            end = Math.min(end, POISSON_ARRAY_SIZE);

            for (int j = start; j < end; j++) {
                arr[j] = (byte) k;
            }
        }
        return arr;
    }

    /**
     * Sets the scale of the texture.
     *
     * @param scale the texture scale
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Sets the stretch factor of the texture.
     *
     * @param stretch the texture stretch factor
     */
    public void setStretch(float stretch) {
        this.stretch = stretch;
    }

    /**
     * Sets the angle of the texture.
     *
     * @param angle the texture angle
     */
    public void setAngle(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        m00 = cos;
        m01 = sin;
        m10 = -sin;
        m11 = cos;
    }

    public void setF1(float v) {
        coefficients[0] = v;
    }

    public void setF2(float v) {
        coefficients[1] = v;
    }

    public void setF3(float v) {
        coefficients[2] = v;
    }

    public void setF4(float v) {
        coefficients[3] = v;
    }

    /**
     * Sets the colormap to be used for the filter.
     *
     * @param colormap the colormap
     */
    public void setColormap(Colormap colormap) {
        this.colormap = colormap;
    }

    /**
     * Sets the randomness factor for grid point placement.
     *
     * @param randomness the randomness factor
     */
    public void setRandomness(float randomness) {
        this.randomness = randomness;
    }

    /**
     * Sets the grid type for the texture.
     *
     * @param gt the code representing the grid type
     */
    public void setGridType(GridType gridType) {
        this.gridType = gridType;
    }

    /**
     * Sets the turbulence of the texture.
     *
     * @param turbulence the turbulence of the texture (in the range [0, 1])
     */
    public void setTurbulence(float turbulence) {
        this.turbulence = turbulence;
    }

    /**
     * Sets the effect amount of the texture.
     *
     * @param amount the amount (in the range [0, 1])
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    public static class Point {
        public int index;
        public float x, y;
        public float dx, dy;
        public float distance;
    }

    /**
     * The filter divides the image into a grid of cells.
     * Within each cell, one or more feature points are generated.
     * The different grid types define how these points are placed.
     */
    public enum GridType {
        RANDOM("Fully Random") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                CachedFloatRandom random = randomTL.get();
                setRandomSeed(random, cellX, cellY);
                int randomIndex = random.nextInt() & 0x1F_FF;
                int numPoints = poisson[randomIndex];
                for (int i = 0; i < numPoints; i++) {
                    float px = random.nextFloat();
                    float py = random.nextFloat();
                    keepNearest3(x, y, cellX, cellY, results, px, py, 1.0f);
                }
                return results[2].distance;
            }
        }, SQUARE("Squares") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                float px = 0.5f;
                float py = 0.5f;
                if (randomness != 0) {
                    CachedFloatRandom random = randomTL.get();
                    setRandomSeed(random, cellX, cellY);
                    px += randomness * (random.nextFloat() - 0.5f);
                    py += randomness * (random.nextFloat() - 0.5f);
                }
                keepNearest3(x, y, cellX, cellY, results, px, py, 1.0f);
                return results[2].distance;
            }
        }, HEXAGONAL("Hexagons") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                float px = 0.75f;
                float py = (cellX & 1) == 0 ? 0.0f : 0.5f;
                evaluatePoint(x, y, cellX, cellY, results, randomness, px, py, 1.0f);
                return results[2].distance;
            }
        }, OCTAGONAL("Octagons & Squares") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.207f, 0.207f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.707f, 0.707f, 1.6f);
                return results[2].distance;
            }
        }, TRIANGULAR("Triangles") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                boolean evenY = (cellY & 1) == 0;
                float px1 = evenY ? 0.25f : 0.75f;
                float px2 = evenY ? 0.75f : 0.25f;
                evaluatePoint(x, y, cellX, cellY, results, randomness, px1, 0.35f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, px2, 0.65f, 1.0f);
                return results[2].distance;
            }
        };

        private final String displayName;

        static final ThreadLocal<CachedFloatRandom> randomTL = ThreadLocal.withInitial(CachedFloatRandom::new);

        GridType(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Checks a grid cell for a feature point and updates the list of nearest points.
         */
        abstract float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness);

        // maintains the result array such that it always contains
        // the three closest points found so far, sorted by distance
        static void keepNearest3(float x, float y, int cubeX, int cubeY, Point[] results, float px, float py, float weight) {
            float dx = Math.abs(x - px) * weight;
            float dy = Math.abs(y - py) * weight;

            // the distance between the current point and the new feature point
            float d = (float) FastMath.sqrt(dx * dx + dy * dy); // sqrtQuick is ugly

            if (d < results[0].distance) {
                // closer than the current closest point (at index 0):
                // shift 0->1, 1->2, and insert the new point at 0
                Point p = results[2];
                results[2] = results[1];
                results[1] = results[0];
                results[0] = p;
                updatePoint(p, d, dx, dy, cubeX + px, cubeY + py);
            } else if (d < results[1].distance) {
                // closer than the second closest point:
                // shifts 1->2, and inserts the new point at 1
                Point p = results[2];
                results[2] = results[1];
                results[1] = p;
                updatePoint(p, d, dx, dy, cubeX + px, cubeY + py);
            } else if (d < results[2].distance) {
                // replace the point at index 2 with the current point
                updatePoint(results[2], d, dx, dy, cubeX + px, cubeY + py);
            }
        }

        private static void updatePoint(Point p, float d, float dx, float dy, float x, float y) {
            p.distance = d;
            p.dx = dx;
            p.dy = dy;
            p.x = x;
            p.y = y;
        }

        static void evaluatePoint(float x, float y, int cellX, int cellY, Point[] results, float randomness, float px, float py, float weight) {
            if (randomness != 0) {
                px += randomness * Noise.noise2(271 * (cellX + px), 271 * (cellY + py));
                py += randomness * Noise.noise2(271 * (cellX + px) + 89, 271 * (cellY + py) + 137);
            }
            keepNearest3(x, y, cellX, cellY, results, px, py, weight);
        }

        static void setRandomSeed(CachedFloatRandom random, int cellX, int cellY) {
            int seed = (571 * cellX + 23 * cellY) & 0x7F_FF_FF_FF;
            if (seed == 0) {
                seed = 1;
            }
            random.setSeed(seed);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public float evaluate(float x, float y) {
        Point[] results = resultsTL.get();
        for (Point result : results) {
            result.distance = Float.POSITIVE_INFINITY;
        }

        // fast floor coordinates of the cell that correctly handles negative bounds
        int ix = (int) x;
        if (x < ix) {
            ix--;
        }

        int iy = (int) y;
        if (y < iy) {
            iy--;
        }

        // the fractional part is the coordinate within the cell
        float fx = x - ix;
        float fy = y - iy;

        // check the current cell
        float d = gridType.checkCell(fx, fy, ix, iy, results, randomness);

        // check adjacent cells if necessary
        if (d > fy) {
            d = gridType.checkCell(fx, fy + 1, ix, iy - 1, results, randomness);
        }
        if (d > 1 - fy) {
            d = gridType.checkCell(fx, fy - 1, ix, iy + 1, results, randomness);
        }
        if (d > fx) {
            d = gridType.checkCell(fx + 1, fy, ix - 1, iy, results, randomness);
            if (d > fy) {
                d = gridType.checkCell(fx + 1, fy + 1, ix - 1, iy - 1, results, randomness);
            }
            if (d > 1 - fy) {
                d = gridType.checkCell(fx + 1, fy - 1, ix - 1, iy + 1, results, randomness);
            }
        }
        if (d > 1 - fx) {
            d = gridType.checkCell(fx - 1, fy, ix + 1, iy, results, randomness);
            if (d > fy) {
                d = gridType.checkCell(fx - 1, fy + 1, ix + 1, iy - 1, results, randomness);
            }
            if (d > 1 - fy) {
                d = gridType.checkCell(fx - 1, fy - 1, ix + 1, iy + 1, results, randomness);
            }
        }

        // at this point results is guaranteed to hold the three smallest distances encountered,
        // so we can now calculate the weighted combination of these distances
        return coefficients[0] * results[0].distance
            + coefficients[1] * results[1].distance
            + coefficients[2] * results[2].distance;
    }

    private float turbulence2(float x, float y, float freq) {
        float t = 0.0f;

        for (float f = 1.0f; f <= freq; f *= 2) {
            t += evaluate(f * x, f * y) / f;
        }
        return t;
    }

    public int genPixel(int x, int y, int[] inPixels, int width, int height) {
        float nx = m00 * x + m01 * y;
        float ny = m10 * x + m11 * y;
        nx /= scale;
        ny /= scale * stretch;
        nx += 1000;
        ny += 1000;    // offset to reduce artifacts around (0,0)

        float f = turbulence == 1.0f ? evaluate(nx, ny) : turbulence2(nx, ny, turbulence);
        f *= 2 * amount;

        if (colormap != null) {
            return colormap.getColor(f);
        }

        int v = PixelUtils.clamp((int) (f * 255));
        return 0xFF_00_00_00 | (v << 16) | (v << 8) | v;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);
        int[] outPixels = new int[width * height];

        Future<?>[] rowFutures = new Future<?>[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> {
                int index = width * finalY;
                for (int x = 0; x < width; x++) {
                    outPixels[index++] = genPixel(x, finalY, inPixels, width, height);
                }
            };
            rowFutures[y] = ThreadPool.submit(rowTask);
        }
        ThreadPool.waitFor(rowFutures, pt);

        finishProgressTracker();

        return outPixels;
    }
}
