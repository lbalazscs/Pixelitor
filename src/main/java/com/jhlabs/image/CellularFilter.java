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

    public static final int GR_RANDOM = 0;
    public static final int GR_SQUARE = 1;
    public static final int GR_HEXAGONAL = 2;
    public static final int GR_OCTAGONAL = 3;
    public static final int GR_TRIANGULAR = 4;
    private GridType gridType;

    protected float scale = 32;
    protected float stretch = 1.0f;
    protected float angle = 0.0f;
    public float amount = 1.0f;
    public float turbulence = 1.0f;
    protected Colormap colormap = new Gradient();
    private final float[] coefficients = {1, 0, 0, 0};

    protected float m00 = 1.0f;
    protected float m01 = 0.0f;
    protected float m10 = 0.0f;
    protected float m11 = 1.0f;
//    protected Point[] results = null;

    protected static final ThreadLocal<Point[]> resultsTL = ThreadLocal.withInitial(() -> {
        Point[] results = new Point[3];
        for (int j = 0; j < results.length; j++) {
            results[j] = new Point();
        }
        return results;
    });

    protected float randomness = 0;

    private static byte[] poisson;

    public CellularFilter(String filterName) {
        super(filterName);

        if (poisson == null) {
            initPoisson();
        }
    }

    /**
     * Creates a discretized approximation of the Poisson probability
     * distribution. This array maps a uniformly distributed random
     * index to a specific outcome, representing the number of events,
     * enabling fast random sampling.
     */
    private static void initPoisson() {
        poisson = new byte[POISSON_ARRAY_SIZE];
        float factorial = 1;
        float total = 0; // the cumulative sum of probabilities
        float mean = 2.5f; // the λ parameter of the Poisson distribution
        for (int k = 0; k < 10; k++) {
            if (k > 1) {
                factorial *= k;
            }

            // the probability of an event occurring k times
            float probability = (float) Math.pow(mean, k) * (float) Math.exp(-mean) / factorial;

            int start = (int) (total * POISSON_ARRAY_SIZE);
            total += probability;
            int end = (int) (total * POISSON_ARRAY_SIZE);
            for (int j = start; j < end; j++) {
                poisson[j] = (byte) k;
            }
        }
    }

    /**
     * Sets the scale of the texture.
     *
     * @param scale the scale of the texture.
     * @min-value 1
     * @max-value 300+
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Sets the stretch factor of the texture.
     *
     * @param stretch the stretch factor of the texture.
     * @min-value 1
     * @max-value 50+
     */
    public void setStretch(float stretch) {
        this.stretch = stretch;
    }

    /**
     * Sets the angle of the texture.
     *
     * @param angle the angle of the texture.
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

    public void setCoefficient(int i, float v) {
        coefficients[i] = v;
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
     * @param randomness the randomness factor.
     */
    public void setRandomness(float randomness) {
        this.randomness = randomness;
    }

    /**
     * Sets the grid type for the texture.
     *
     * @param gt the code representing the grid type.
     */
    public void setGridType(int gt) {
        gridType = switch (gt) {
            case GR_HEXAGONAL -> GridType.HEXAGONAL;
            case GR_OCTAGONAL -> GridType.OCTAGONAL;
            case GR_RANDOM -> GridType.RANDOM;
            case GR_SQUARE -> GridType.SQUARE;
            case GR_TRIANGULAR -> GridType.TRIANGULAR;
            default -> throw new IllegalArgumentException("gridType = " + gt);
        };
    }

    /**
     * Sets the turbulence of the texture.
     *
     * @param turbulence the turbulence of the texture.
     * @min-value 0
     * @max-value 1
     */
    public void setTurbulence(float turbulence) {
        this.turbulence = turbulence;
    }

    /**
     * Sets the effect amount of the texture.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
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
    enum GridType {
        RANDOM {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                CachedFloatRandom random = randomTL.get();
                random.setSeed(571 * cellX + 23 * cellY);
                int randomIndex = random.nextInt() & 0x1fff;
                int numPoints = poisson[randomIndex];
                float weight = 1.0f;
                for (int i = 0; i < numPoints; i++) {
                    float px = random.nextFloat();
                    float py = random.nextFloat();
                    keepNearest3(x, y, cellX, cellY, results, px, py, weight);
                }
                return results[2].distance;
            }
        }, SQUARE {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
                CachedFloatRandom random = randomTL.get();
                random.setSeed(571 * cellX + 23 * cellY);
                float px = 0.5f;
                float py = 0.5f;
                if (randomness != 0) {
                    px = (float) (px + randomness * (random.nextFloat() - 0.5));
                    py = (float) (py + randomness * (random.nextFloat() - 0.5));
                }
                float weight = 1.0f;
                keepNearest3(x, y, cellX, cellY, results, px, py, weight);
                return results[2].distance;
            }
        }, HEXAGONAL {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
//                random.setSeed(571 * cubeX + 23 * cubeY);
                float px, py;
                if ((cellX & 1) == 0) {
                    px = 0.75f;
                    py = 0;
                } else {
                    px = 0.75f;
                    py = 0.5f;
                }
                if (randomness != 0) {
                    px += randomness * Noise.noise2(271 * (cellX + px), 271 * (cellY + py));
                    py += randomness * Noise.noise2(271 * (cellX + px) + 89, 271 * (cellY + py) + 137);
                }
                keepNearest3(x, y, cellX, cellY, results, px, py, 1.0f);
                return results[2].distance;
            }
        }, OCTAGONAL {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
//                random.setSeed(571 * cubeX + 23 * cubeY);
                float weight = 1.0f;
                for (int i = 0; i < 2; i++) {
                    float px = 0.0f;
                    float py = 0.0f;
                    switch (i) {
                        case 0:
                            px = 0.207f;
                            py = 0.207f;
                            break;
                        case 1:
                            px = 0.707f;
                            py = 0.707f;
                            weight = 1.6f;
                            break;
                    }
                    if (randomness != 0) {
                        px += randomness * Noise.noise2(271 * (cellX + px), 271 * (cellY + py));
                        py += randomness * Noise.noise2(271 * (cellX + px) + 89, 271 * (cellY + py) + 137);
                    }
                    keepNearest3(x, y, cellX, cellY, results, px, py, weight);
                }
                return results[2].distance;
            }
        }, TRIANGULAR {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness) {
//                random.setSeed(571 * cubeX + 23 * cubeY);
                float weight = 1.0f;
                for (int i = 0; i < 2; i++) {
                    float px, py;
                    if ((cellY & 1) == 0) {
                        if (i == 0) {
                            px = 0.25f;
                            py = 0.35f;
                        } else {
                            px = 0.75f;
                            py = 0.65f;
                        }
                    } else {
                        if (i == 0) {
                            px = 0.75f;
                            py = 0.35f;
                        } else {
                            px = 0.25f;
                            py = 0.65f;
                        }
                    }
                    if (randomness != 0) {
                        px += randomness * Noise.noise2(271 * (cellX + px), 271 * (cellY + py));
                        py += randomness * Noise.noise2(271 * (cellX + px) + 89, 271 * (cellY + py) + 137);
                    }
                    keepNearest3(x, y, cellX, cellY, results, px, py, weight);
                }
                return results[2].distance;
            }
        };

        static final ThreadLocal<CachedFloatRandom> randomTL =
                ThreadLocal.withInitial(CachedFloatRandom::new);

        /**
         * Checks a grid cell for a feature point and updates the list of nearest points.
         *
         * @param x          The fractional x-coordinate within the cell (0-1).
         * @param y          The fractional y-coordinate within the cell (0-1).
         * @param cellX      The integer x-coordinate of the cell.
         * @param cellY      The integer y-coordinate of the cell.
         * @param results    An array to store the three nearest points found so far. This array is modified by this method.
         * @param randomness A randomness factor to jitter the feature point position.
         * @return The distance to the third nearest point (used for optimization in some grid types).
         */
        abstract float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness);

        // maintains the result array such that it always contains
        // the three closest points found so far, sorted by distance
        static void keepNearest3(float x, float y, int cubeX, int cubeY, Point[] results, float px, float py, float weight) {
            float dx = Math.abs(x - px);
            float dy = Math.abs(y - py);
            float d;
            dx *= weight;
            dy *= weight;

            // the distance between the current point and the new feature point
            d = (float) FastMath.sqrt(dx * dx + dy * dy);  // sqrtQuick is ugly

            if (d < results[0].distance) {
                // closer than the the current closest point (at index 0):
                // shift 0->1, 1->2, and insert the new point at 0
                Point p = results[2];
                results[2] = results[1];
                results[1] = results[0];
                results[0] = p;
                p.distance = d;
                p.dx = dx;
                p.dy = dy;
                p.x = cubeX + px;
                p.y = cubeY + py;
            } else if (d < results[1].distance) {
                // closer than the second closest point:
                // shifts 1->2, and inserts the new point at 1
                Point p = results[2];
                results[2] = results[1];
                results[1] = p;
                p.distance = d;
                p.dx = dx;
                p.dy = dy;
                p.x = cubeX + px;
                p.y = cubeY + py;
            } else if (d < results[2].distance) {
                // replace the point at index 2 with the current point
                Point p = results[2];
                p.distance = d;
                p.dx = dx;
                p.dy = dy;
                p.x = cubeX + px;
                p.y = cubeY + py;
            }
        }
    }

    public float evaluate(float x, float y) {
        Point[] results = resultsTL.get();
        for (Point result : results) {
            result.distance = Float.POSITIVE_INFINITY;
        }

        // the coordinates of the cell
        int ix = (int) x;
        int iy = (int) y;

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
            gridType.checkCell(fx + 1, fy, ix - 1, iy, results, randomness);
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

        // At this point results is guaranteed to hold
        // the three smallest distances encountered.
        // Now calculate combination of these distances.
        float t = 0;
        for (int i = 0; i < 3; i++) {
            t += coefficients[i] * results[i].distance;
        }

        return t;
    }

    public float turbulence2(float x, float y, float freq) {
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
        f *= 2;
        f *= amount;

        int a = 0xff000000;
        int v;

        if (colormap != null) {
            return colormap.getColor(f);
        } else {
            v = PixelUtils.clamp((int) (f * 255));
            int r = v << 16;
            int g = v << 8;
            int b = v;
            return a | r | g | b;
        }
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);
        int[] outPixels = new int[width * height];

        Future<?>[] rowFutures = new Future[height];
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

    @Override
    public String toString() {
        return "Texture/Cellular...";
    }
}
