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
import pixelitor.ThreadPool;
import pixelitor.utils.CachedFloatRandom;

import java.util.concurrent.Future;

/**
 * An abstract base class for filters that partition an image space into
 * grid cells to search for nearest feature points (e.g. Voronoi/Worley noise).
 */
public abstract class CellSearchFilter extends WholeImageFilter {
    private static final int POISSON_ARRAY_SIZE = 8192;

    // reduces artifacts around (0,0) because cell
    // indices stay non-negative for typical images
    private static final float ORIGIN_OFFSET = 1000f;

    private final GridType gridType;
    private final float randomness;

    // image space -> noise space: rotation, then division by scale (x) and scale * stretch (y)
    private final float m00;
    private final float m01;
    private final float m10;
    private final float m11;

    // noise space -> image space: the inverse of the matrix above
    private final float inv00;
    private final float inv01;
    private final float inv10;
    private final float inv11;

    protected static final ThreadLocal<Point[]> resultsTL = ThreadLocal.withInitial(() -> {
        Point[] results = new Point[3];
        for (int j = 0; j < results.length; j++) {
            results[j] = new Point();
        }
        return results;
    });

    private static final byte[] poisson = initPoisson();

    /**
     * Constructs a new {@code CellSearchFilter}.
     *
     * @param filterName the name of the filter
     * @param scale      the texture scale
     * @param stretch    the texture stretch factor
     * @param angle      the texture angle in radians
     * @param gridType   the grid type defining feature point placement
     * @param randomness the randomness factor for grid point placement
     */
    protected CellSearchFilter(String filterName,
                               float scale,
                               float stretch,
                               float angle,
                               GridType gridType,
                               float randomness) {
        super(filterName);

        this.gridType = gridType;
        this.randomness = randomness;

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float scaleY = scale * stretch;

        m00 = cos / scale;
        m01 = sin / scale;
        m10 = -sin / scaleY;
        m11 = cos / scaleY;

        inv00 = cos * scale;
        inv01 = -sin * scaleY;
        inv10 = sin * scale;
        inv11 = cos * scaleY;
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
     * A feature point in noise space and its weighted distance to the sampled position.
     */
    public static class Point {
        public float x, y;
        public float distance;
    }

    /**
     * The filter divides the image into a grid of cells.
     * Within each cell, one or more feature points are generated.
     * The different grid types define how these points are placed.
     */
    public enum GridType {
        // this one ignores the randomness setting
        RANDOM("Fully Random") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                CachedFloatRandom random = randomTL.get();
                setRandomSeed(random, cellX, cellY);
                int randomIndex = random.nextInt() & 0x1F_FF;
                int numPoints = poisson[randomIndex];
                for (int i = 0; i < numPoints; i++) {
                    float px = random.nextFloat();
                    float py = random.nextFloat();
                    insertIfNearer(x, y, cellX, cellY, results, px, py, 1.0f);
                }
                return results[needed - 1].distance;
            }
        }, SQUARE("Squares") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                float px = 0.5f;
                float py = 0.5f;
                if (randomness != 0) {
                    CachedFloatRandom random = randomTL.get();
                    setRandomSeed(random, cellX, cellY);
                    px += randomness * (random.nextFloat() - 0.5f);
                    py += randomness * (random.nextFloat() - 0.5f);
                }
                insertIfNearer(x, y, cellX, cellY, results, px, py, 1.0f);
                return results[needed - 1].distance;
            }
        }, HEXAGONAL("Hexagons") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                float px = 0.75f;
                float py = (cellX & 1) == 0 ? 0.0f : 0.5f;
                evaluatePoint(x, y, cellX, cellY, results, randomness, px, py, 1.0f);
                return results[needed - 1].distance;
            }
        }, OCTAGONAL("Octagons & Squares") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.207f, 0.207f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.707f, 0.707f, 1.6f);
                return results[needed - 1].distance;
            }
        }, TRIANGULAR("Triangles") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                boolean evenY = (cellY & 1) == 0;
                float px1 = evenY ? 0.25f : 0.75f;
                float px2 = evenY ? 0.75f : 0.25f;
                evaluatePoint(x, y, cellX, cellY, results, randomness, px1, 0.35f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, px2, 0.65f, 1.0f);
                return results[needed - 1].distance;
            }
        }, CAIRO("Pentagons (Cairo)") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                // 4 points of a regular snub-square lattice (wallpaper group p4g),
                // whose Voronoi cells form the Cairo pentagonal tiling.
                // 0.31699 = (3 - sqrt(3)) / 4
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.31699f, 0.81699f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.68301f, 0.18301f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.81699f, 0.68301f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.18301f, 0.31699f, 1.0f);
                return results[needed - 1].distance;
            }
        }, LIEB("Squares & Hexagons") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                // Lieb lattice: a square lattice with every fourth point removed
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.0f, 0.0f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.5f, 0.0f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.0f, 0.5f, 1.0f);
                return results[needed - 1].distance;
            }
        }, PINWHEEL("Pinwheels") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                // four points related by 90° rotation around the cell center (wallpaper group p4)
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.70f, 0.58f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.42f, 0.70f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.30f, 0.42f, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.58f, 0.30f, 1.0f);
                return results[needed - 1].distance;
            }
        }, ROSETTE("Rosettes") {
            // offsets of 6 points on a ring of radius 0.3, rotated by 15°
            private static final float[] RING = {
                0.2898f, 0.0776f, 0.0776f, 0.2898f, -0.2121f, 0.2121f,
                -0.2898f, -0.0776f, -0.0776f, -0.2898f, 0.2121f, -0.2121f
            };

            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                // a hub point surrounded by a ring of six points, giving flower-like clusters
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.5f, 0.5f, 1.0f);
                for (int i = 0; i < RING.length; i += 2) {
                    evaluatePoint(x, y, cellX, cellY, results, randomness,
                        0.5f + RING[i], 0.5f + RING[i + 1], 1.0f);
                }
                return results[needed - 1].distance;
            }
        }, DIAGONALS("Alternating Diagonals") {
            @Override
            float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed) {
                // point pairs on alternating diagonals in a checkerboard arrangement;
                // gives a pattern of triangles and kites
                boolean even = ((cellX + cellY) & 1) == 0;
                float y1 = even ? 0.2f : 0.8f;
                float y2 = even ? 0.8f : 0.2f;
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.2f, y1, 1.0f);
                evaluatePoint(x, y, cellX, cellY, results, randomness, 0.8f, y2, 1.0f);
                return results[needed - 1].distance;
            }
        };

        private final String displayName;

        static final ThreadLocal<CachedFloatRandom> randomTL = ThreadLocal.withInitial(CachedFloatRandom::new);

        GridType(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Checks a grid cell for a feature point and updates the list of nearest points.
         * x/y are relative to the cell origin, cellX/cellY are absolute cell
         * indices, and the return value is the current needed-th nearest distance.
         */
        abstract float checkCell(float x, float y, int cellX, int cellY, Point[] results, float randomness, int needed);

        // maintains the result array such that it always contains
        // the three closest points found so far, sorted by distance
        static void insertIfNearer(float x, float y, int cellX, int cellY, Point[] results, float px, float py, float weight) {
            float dx = (x - px) * weight;
            float dy = (y - py) * weight;

            // the distance between the current point and the new feature point
            float d = ImageMath.hypot(dx, dy);

            if (d < results[0].distance) {
                // closer than the current closest point (at index 0):
                // shift 0->1, 1->2, and insert the new point at 0
                Point p = results[2];
                results[2] = results[1];
                results[1] = results[0];
                results[0] = p;
                updatePoint(p, d, cellX + px, cellY + py);
            } else if (d < results[1].distance) {
                // closer than the second closest point:
                // shifts 1->2, and inserts the new point at 1
                Point p = results[2];
                results[2] = results[1];
                results[1] = p;
                updatePoint(p, d, cellX + px, cellY + py);
            } else if (d < results[2].distance) {
                // replace the point at index 2 with the current point
                updatePoint(results[2], d, cellX + px, cellY + py);
            }
        }

        private static void updatePoint(Point p, float d, float x, float y) {
            p.distance = d;
            p.x = x;
            p.y = y;
        }

        static void evaluatePoint(float x, float y, int cellX, int cellY, Point[] results, float randomness, float px, float py, float weight) {
            if (randomness != 0) {
                float origPx = px, origPy = py;
                px += randomness * Noise.noise2(271 * (cellX + origPx), 271 * (cellY + origPy));
                py += randomness * Noise.noise2(271 * (cellX + origPx) + 89, 271 * (cellY + origPy) + 137);
            }
            insertIfNearer(x, y, cellX, cellY, results, px, py, weight);
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

    protected float toNoiseX(int x, int y) {
        return m00 * x + m01 * y + ORIGIN_OFFSET;
    }

    protected float toNoiseY(int x, int y) {
        return m10 * x + m11 * y + ORIGIN_OFFSET;
    }

    /**
     * Returns the nearest feature points (a reused, thread-local array).
     */
    protected Point[] findNearestPoints(int x, int y, int needed) {
        Point[] results = resultsTL.get();
        searchNeighborhood(toNoiseX(x, y), toNoiseY(x, y), results, needed);
        return results;
    }

    protected void searchNeighborhood(float nx, float ny, Point[] results, int needed) {
        for (Point result : results) {
            result.distance = Float.POSITIVE_INFINITY;
        }

        // fast floor coordinates of the cell that correctly handles negative bounds
        int ix = (int) nx;
        if (nx < ix) {
            ix--;
        }

        int iy = (int) ny;
        if (ny < iy) {
            iy--;
        }

        // the fractional part is the coordinate within the cell
        float fx = nx - ix;
        float fy = ny - iy;

        // check the current cell
        float d = gridType.checkCell(fx, fy, ix, iy, results, randomness, needed);

        // check adjacent cells if necessary
        if (d > fy) {
            d = gridType.checkCell(fx, fy + 1, ix, iy - 1, results, randomness, needed);
        }
        if (d > 1 - fy) {
            d = gridType.checkCell(fx, fy - 1, ix, iy + 1, results, randomness, needed);
        }
        if (d > fx) {
            d = gridType.checkCell(fx + 1, fy, ix - 1, iy, results, randomness, needed);
            if (d > fy) {
                d = gridType.checkCell(fx + 1, fy + 1, ix - 1, iy - 1, results, randomness, needed);
            }
            if (d > 1 - fy) {
                d = gridType.checkCell(fx + 1, fy - 1, ix - 1, iy + 1, results, randomness, needed);
            }
        }
        if (d > 1 - fx) {
            d = gridType.checkCell(fx - 1, fy, ix + 1, iy, results, randomness, needed);
            if (d > fy) {
                d = gridType.checkCell(fx - 1, fy + 1, ix + 1, iy - 1, results, randomness, needed);
            }
            if (d > 1 - fy) {
                gridType.checkCell(fx - 1, fy - 1, ix + 1, iy + 1, results, randomness, needed);
            }
        }
    }

    /**
     * Generates a single pixel at the specified coordinate.
     */
    public abstract int genPixel(int x, int y, int[] inPixels, int width, int height, int needed);

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        pt = createProgressTracker(height);
        int[] outPixels = new int[width * height];
        int needed = requiredPoints();

        Future<?>[] rowFutures = new Future<?>[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> {
                int index = width * finalY;
                for (int x = 0; x < width; x++) {
                    outPixels[index++] = genPixel(x, finalY, inPixels, width, height, needed);
                }
            };
            rowFutures[y] = ThreadPool.submit(rowTask);
        }
        ThreadPool.waitFor(rowFutures, pt);

        finishProgressTracker();

        return outPixels;
    }

    /**
     * Maps a feature point from noise space back to image space (inverting
     * scale, stretch, and rotation) and samples the source pixel.
     */
    protected int getSourcePixel(Point p, int[] inPixels, int width, int height) {
        float dx = p.x - ORIGIN_OFFSET;
        float dy = p.y - ORIGIN_OFFSET;
        int srcX = ImageMath.clamp((int) (inv00 * dx + inv01 * dy), 0, width - 1);
        int srcY = ImageMath.clamp((int) (inv10 * dx + inv11 * dy), 0, height - 1);
        return inPixels[srcY * width + srcX];
    }

    /**
     * How many of the nearest points (1 to 3) the current settings read.
     */
    protected abstract int requiredPoints();
}
