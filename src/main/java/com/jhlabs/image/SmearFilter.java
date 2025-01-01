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

import pixelitor.ThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

public class SmearFilter extends WholeImageFilter {
    public static final int CROSSES = 0;
    public static final int LINES = 1;
    public static final int CIRCLES = 2;
    public static final int SQUARES = 3;
    public static final int DIAMONDS = 4;

    //    private Colormap colormap = new LinearColormap();
    private float angle = 0;
    private float density = 0.5f;
    private int distance = 8;
    private Random random;
    //    private long seed = 567;
    private int shape = LINES;
    private float mix = 0.5f;

    public SmearFilter(String filterName) {
        super(filterName);
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    /**
     * Sets the angle of the texture.
     *
     * @param angle the angle of the texture.
     * @angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels) {
        int[] outPixels = new int[width * height];

        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                outPixels[i] = inPixels[i];
                i++;
            }
        }

        switch (shape) {
            case CROSSES -> renderCrosses(width, height, inPixels, outPixels);
            case LINES -> renderLines(width, height, inPixels, outPixels);
            case SQUARES, CIRCLES, DIAMONDS -> renderShapes(width, height, inPixels, outPixels);
        }

        finishProgressTracker();

        return outPixels;
    }

    private void renderCrosses(int width, int height, int[] inPixels, int[] outPixels) {
        int numShapes = (int) (2 * density * width * height / (distance + 1));

        pt = createProgressTracker(numShapes);

        for (int i = 0; i < numShapes; i++) {
            int x = (random.nextInt() & 0x7fffffff) % width;
            int y = (random.nextInt() & 0x7fffffff) % height;
            int length = random.nextInt(distance) + 1;
            int rgb = inPixels[y * width + x];
            for (int x1 = x - length; x1 < x + length + 1; x1++) {
                if (x1 >= 0 && x1 < width) {
                    int offset = y * width + x1;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
                }
            }
            for (int y1 = y - length; y1 < y + length + 1; y1++) {
                if (y1 >= 0 && y1 < height) {
                    int offset = y1 * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
                }
            }
            pt.unitDone();
        }
    }

    private void renderLines(int width, int height, int[] inPixels, int[] outPixels) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        int numShapes = (int) (2 * density * width * height / 2);

        int stride = numShapes / 100 + 1;
        int estimatedWorkUnits = numShapes / stride;
        pt = createProgressTracker(estimatedWorkUnits);

        List<Future<?>> futures = new ArrayList<>(estimatedWorkUnits + 1);
        for (int i = 0; i < numShapes; i = i + stride) {
            Runnable r = () -> {
                for (int j = 0; j < stride; j++) {
                    renderOneLine(width, height, inPixels, outPixels, sin, cos);
                }
            };
            futures.add(ThreadPool.submit(r));
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneLine(int width, int height, int[] inPixels, int[] outPixels, float sin, float cos) {
        int sx = (random.nextInt() & 0x7fffffff) % width;
        int sy = (random.nextInt() & 0x7fffffff) % height;
        int rgb = inPixels[sy * width + sx];
        int length = (random.nextInt() & 0x7fffffff) % distance;
        int dx = (int) (length * cos);
        int dy = (int) (length * sin);

        int x0 = sx - dx;
        int y0 = sy - dy;
        int x1 = sx + dx;
        int y1 = sy + dy;
        int x, y, d, incrE, incrNE, ddx, ddy;

        if (x1 < x0) {
            ddx = -1;
        } else {
            ddx = 1;
        }
        if (y1 < y0) {
            ddy = -1;
        } else {
            ddy = 1;
        }
        dx = x1 - x0;
        dy = y1 - y0;
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        x = x0;
        y = y0;

        if (x < width && x >= 0 && y < height && y >= 0) {
            int offset = y * width + x;
            int rgb2 = outPixels[offset];
            outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
        }
        if (dx > dy) {
            d = 2 * dy - dx;
            incrE = 2 * dy;
            incrNE = 2 * (dy - dx);

            while (x != x1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    y += ddy;
                }
                x += ddx;
                if (x < width && x >= 0 && y < height && y >= 0) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
                }
            }
        } else {
            d = 2 * dx - dy;
            incrE = 2 * dx;
            incrNE = 2 * (dx - dy);

            while (y != y1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    x += ddx;
                }
                y += ddy;
                if (x < width && x >= 0 && y < height && y >= 0) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
                }
            }
        }
    }

    private void renderShapes(int width, int height, int[] inPixels, int[] outPixels) {
        int radius = distance + 1;
        int radius2 = radius * radius;

        int numShapes = (int) (2 * density * width * height / radius);

        pt = createProgressTracker(numShapes);
        Future<?>[] futures = new Future[numShapes];

        for (int i = 0; i < numShapes; i++) {
            Runnable r = () -> renderOneShape(width, height, inPixels, outPixels, radius, radius2);
            futures[i] = ThreadPool.submit(r);
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneShape(int width, int height, int[] inPixels, int[] outPixels, int radius, int radius2) {
        int sx = (random.nextInt() & 0x7fffffff) % width;
        int sy = (random.nextInt() & 0x7fffffff) % height;
        int rgb = inPixels[sy * width + sx];
        int minSx = sx - radius;
        int maxSx = sx + radius + 1;
        int minSy = sy - radius;
        int maxSy = sy + radius + 1;

        if (minSx < 0) {
            minSx = 0;
        }
        if (minSy < 0) {
            minSy = 0;
        }
        if (maxSx > width) {
            maxSx = width;
        }
        if (maxSy > height) {
            maxSy = height;
        }

        switch (shape) {
            case CIRCLES -> makeCircle(width, outPixels, radius2, sx, sy, rgb, minSx, maxSx, minSy, maxSy);
            case SQUARES -> makeSquare(width, outPixels, rgb, minSx, maxSx, minSy, maxSy);
            case DIAMONDS -> makeDiamond(width, outPixels, radius, sx, sy, rgb, minSx, maxSx, minSy, maxSy);
        }
    }

    private void makeCircle(int width, int[] outPixels, int radius2, int sx, int sy, int rgb, int minSx, int maxSx, int minSy, int maxSy) {
        for (int x = minSx; x < maxSx; x++) {
            for (int y = minSy; y < maxSy; y++) {
                int dsx = x - sx;
                int dsy = y - sy;
                boolean isInside = dsx * dsx + dsy * dsy <= radius2;
                if (isInside) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
                }
            }
        }
    }

    private void makeSquare(int width, int[] outPixels, int rgb, int minSx, int maxSx, int minSy, int maxSy) {
        for (int x = minSx; x < maxSx; x++) {
            for (int y = minSy; y < maxSy; y++) {
                int offset = y * width + x;
                int rgb2 = outPixels[offset];
                outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
            }
        }
    }

    private void makeDiamond(int width, int[] outPixels, int radius, int sx, int sy, int rgb, int minSx, int maxSx, int minSy, int maxSy) {
        for (int x = minSx; x < maxSx; x++) {
            for (int y = minSy; y < maxSy; y++) {
                int dx = Math.abs(x - sx);
                int dy = Math.abs(y - sy);
                boolean isInside = (dx + dy) <= radius;
                if (isInside) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(mix, rgb2, rgb);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Effects/Smear...";
    }

    public void setRandom(Random random) {
        this.random = random;
    }
}
